package com.dpp.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dpp.consts.ChannelConst;
import com.dpp.consts.TaskExecuteType;
import com.dpp.domain.Config;
import com.dpp.domain.MatchResultReqDomain;
import com.dpp.domain.ReqDomain;
import com.dpp.domain.TalkingDMatchResultReqDomain;
import com.dpp.domain.TaskHistory;
import com.dpp.repository.ConfigRepository;
import com.dpp.repository.TaskHistoryRepository;
import com.dpp.service.AbstractJobService;
import com.dpp.service.ChannelFactory;

/**
* @author youguoqiang
* @version 2019年3月4日 上午10:13:25
* @description 
*/
@RestController
public class ManualChannelController {

	@Autowired
	private TaskHistoryRepository taskHistoryRepository;
	
	@Autowired
	private ConfigRepository configRepository;
	@Autowired
	private ChannelFactory channelFactory;
	
	@RequestMapping("/home")
	public String home() {
		return "DataPullPlatform is ok!";
	}
	
	@RequestMapping("/runTask")
	public String runTask(HttpServletRequest request) {
		final String fileName = request.getParameter("fileName");
		final String date = request.getParameter("date");
		final String channel = request.getParameter("channel");
		if(StringUtils.isBlank(channel)) {
			return "param is not right";
		}
		
		if(StringUtils.isBlank(date) && (StringUtils.isBlank(fileName) || !fileName.contains(".txt")))
			return "param is not right";
		
		
		
		new Thread(new Runnable() {
			public void run() {
				try {
					channelFactory.getChannelJobService(channel).executeManual(date, fileName, TaskExecuteType.MANUAL.getStatus());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
		return "started,wait result";
	}
	
	@RequestMapping("/queryPath")
	public String queryPath(HttpServletRequest request) {
		String type = request.getParameter("type");
		String date = request.getParameter("date");
		final String channel = request.getParameter("channel");
		if(StringUtils.isBlank(channel)) {
			return "param is not right";
		}
		
		AbstractJobService jobService = channelFactory.getChannelJobService(channel);
		if("output".equals(type)) {
			return jobService.getOutputDir(date);
		} else {
			return jobService.getInputDir(date);
		}
	}
	
	@RequestMapping("/queryAllFiles")
	public String queryAllFiles(HttpServletRequest request) {
		String date = request.getParameter("date");
		String type = request.getParameter("type");
		final String channel = request.getParameter("channel");
		if(StringUtils.isBlank(channel)) {
			return "param is not right";
		}
		
		AbstractJobService jobService = channelFactory.getChannelJobService(channel);
		if(null == type || "input".equalsIgnoreCase(type))
			return JSONObject.toJSONString(jobService.getFilesNameListInput(date));
		else 
			return JSONObject.toJSONString(jobService.getFilesNameListOutput(date));
	}
	
	@RequestMapping("/queryHisTask")
	public String queryHisTask(HttpServletRequest request) {
		String resp = "no data";
		List<TaskHistory> list = null;
		if(null != request.getParameter("date")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			Date d;
			try {
				d = sdf.parse(request.getParameter("date"));
			} catch (ParseException e) {
				e.printStackTrace();
				return "date is not right";
			}
			list = taskHistoryRepository.findAfterDate(d);
		} else {
			list =  taskHistoryRepository.findAll();
		}
		if(CollectionUtils.isNotEmpty(list)) {
			return JSONObject.toJSONStringWithDateFormat(list, "yyyy-MM-dd HH:mm:ss", SerializerFeature.WriteDateUseDateFormat);
		}
		return resp;
	}
	
	@RequestMapping("/updateConfig")
	public String updateConfig(HttpServletRequest request) {
		String isRun = request.getParameter("isRun");
		String isJobRun = request.getParameter("isJobRun");
		final String channel = request.getParameter("channel");
		if(StringUtils.isBlank(channel)) {
			return "param is not right";
		}
		
		if(StringUtils.isBlank(isRun) && StringUtils.isBlank(isJobRun)) {
			return "param is not right.";
		}
		
		Config cfg = configRepository.findCfgByChannel(channel);
		
		if(null != cfg) {
			if(!StringUtils.isBlank(isRun)) {
				cfg.setIsRun(isRun);
			}
			if(!StringUtils.isBlank(isJobRun)) {
				cfg.setIsJobRun(isJobRun);
			}
			cfg.setUpdateTime(new Date());
		} else {
			cfg = new Config();
			if(!StringUtils.isBlank(isRun)) {
				cfg.setIsRun(isRun);
			}
			if(!StringUtils.isBlank(isJobRun)) {
				cfg.setIsJobRun(isJobRun);
			}
			cfg.setUpdateTime(new Date());
		}
		configRepository.save(cfg);
		return "ok";
	}
	
	@RequestMapping(value = "/queryMatchResult",method = RequestMethod.POST)
	public String queryMatchResult(HttpServletRequest request) {
		final String channel = request.getParameter("channel");
		if(StringUtils.isBlank(channel)) {
			return "param is not right";
		}
		
		String body = null;
        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			body = IOUtils.toString(reader);
		} catch (IOException e) {
			e.printStackTrace();
			return "json fomat not right";
		}
        ReqDomain reqDomain = null;
        try {
        	reqDomain = getReqDomain(channel, body);
        	return channelFactory.getChannelService(channel).queryMatchResult(reqDomain);
        } catch(Exception e) {
        	return e.getMessage();
        }
	}
	
	private ReqDomain getReqDomain(String channel,String body) throws Exception {
		ReqDomain reqDomain = null;
		if(ChannelConst.HOT_CLOUND_CHANNEL.equalsIgnoreCase(channel)) {
			reqDomain = JSONObject.parseObject(body, MatchResultReqDomain.class);
		}else if(ChannelConst.TALKING_DATA_CHANNEL.equalsIgnoreCase(channel)) {
			reqDomain = JSONObject.parseObject(body, TalkingDMatchResultReqDomain.class);
		} else {
			throw new IllegalArgumentException("channel is not right");
		}
		return reqDomain;
	}
}

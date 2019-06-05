package com.dpp.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dpp.common.util.HttpClientService;
import com.dpp.domain.HotCloundRespDomain;
import com.dpp.domain.ReqDomain;
import com.dpp.domain.TalkingDMatchResultReqDomain;

/**
* @author youguoqiang
* @version 2019年2月25日 下午4:22:30
* @description 
*/
@Service
public class TalkingDataService extends AbstractChannelService {
	
	@Autowired
	private HttpClientService httpClientService;
	//TODO
	@Value("${talkingData.queryMatchResult.url}")
	private String queryUrl;
	private static final Logger logger = LoggerFactory.getLogger(TalkingDataService.class);
	
	
	public TalkingDMatchResultReqDomain parseContent(String fileName,String line,String mediaCode,String today) {
		if(StringUtils.isBlank(line)) {
			logger.error("parseContent fileName={}, line:{}",fileName,line);
			return null;
		}
		TalkingDMatchResultReqDomain req = new TalkingDMatchResultReqDomain();
		try {
			String[] s = line.split(",");
			if(null == s || s.length != 3) {
				logger.error("parseContent fileName={}, line:{}",fileName,line);
				return null;
			}
			String camp_ids_str = s[0];
			if(StringUtils.isBlank(camp_ids_str)) {
				logger.error("parseContent fileName={}, line:{}",fileName,line);
				return null;
			}
			String[] camp_ids = camp_ids_str.split("#");
			req.setCampaignId(camp_ids);
			req.setDeviceIdType(Integer.valueOf(s[1]));
			req.setDeviceId(s[2]);
			req.setMediaId(mediaCode);
			return req;
		} catch(Exception e) {
			logger.error("parseContent fileName={}, line:{}",fileName,line);
			return null;
		}
		
	}
	
	
	private void testParseResult() {
		HotCloundRespDomain d = new HotCloundRespDomain();
		d.setCode("123");
		String[] ids = {"abc","eee"};
		d.setCamp_ids(ids);
		String s  = JSONObject.toJSONString(d);
		System.out.println(s);
		
		Map m = JSONObject.parseObject(s,Map.class);
		String code = null == m.get("code") ? null : m.get("code").toString();
		//String[] camp = (String[])m.get("camp_ids");
		String ss = m.get("camp_ids").toString();
		System.out.println("camp_ids="+ss);
		JSONArray ja = JSONArray.parseArray(m.get("camp_ids").toString());
	}
	
	public String executeMatchPreMachine(String fileName,String line,String mediaCode,String today) {
		TalkingDMatchResultReqDomain  req = parseContent(fileName, line,mediaCode,today);
		if(null != req) {
			String resp = queryMatchResult(req);
			if(null != resp) {
				Map m = JSONObject.parseObject(resp,Map.class);
				//String code = null == m.get("code") ? null : m.get("code").toString();
				return JSONObject.toJSONString(m);
			}
		} else {//TODO write err file
			
		}
		return null;
	}
	
	public String queryMatchResult(ReqDomain reqDomain) {
		String json = JSONObject.toJSONString(reqDomain);
		StringEntity entity = null;
		try {
			entity = new StringEntity(json,"UTF-8");
			return httpClientService.post(queryUrl, entity);
		} catch (UnsupportedEncodingException e) {
			logger.error("queryMatchResult errMsg={}, reqDomain={}",e.getMessage(),json);
		} catch(IOException e) {
			try {
				return httpClientService.post(queryUrl, entity);
			} catch (IOException e1) {
				logger.error("queryMatchResult IOException errMsg={},err={}, reqDomain={}",e1.getMessage(),e1,json);
			}
		} catch(Exception e) {
			logger.error("queryMatchResult Exception errMsg={},err={}, reqDomain={}",e.getMessage(),e,json);
		}
		return null;
	}
}

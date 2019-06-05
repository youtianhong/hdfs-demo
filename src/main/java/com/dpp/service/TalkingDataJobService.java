package com.dpp.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import com.dpp.common.util.DateUtil;
import com.dpp.common.util.HdfsUtil;
import com.dpp.consts.ChannelConst;
import com.dpp.consts.TaskExecuteType;
import com.dpp.consts.TaskStatus;
import com.dpp.domain.Task;
import com.dpp.domain.TaskHistory;
import com.dpp.repository.ConfigRepository;
import com.dpp.repository.TaskHistoryRepository;
import com.dpp.repository.TaskRepository;

/**
* @author youguoqiang
* @version 2019年5月15日 下午1:39:29
* @description 
*/
@Service
public class TalkingDataJobService extends AbstractJobService  { //implements SchedulingConfigurer

	@Value("${hdfs.baseDir}")
	private String HDFS_BASE_DIR;
	
	@Value("${threadCount}")
	private int threadCount = 10;
	
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private TaskHistoryRepository taskHistoryRepository;
	@Autowired
	private ChannelFactory channelFactory;
	
	private static final String CHANNEL = ChannelConst.TALKING_DATA_CHANNEL;
	
	@Autowired
	private HdfsUtil hdfsUtil;
	
	private static final Logger logger = LoggerFactory.getLogger(TalkingDataJobService.class);
	
	private  ExecutorService pool = Executors.newFixedThreadPool(threadCount);
	private  CompletionService<Long> cService = new ExecutorCompletionService<Long>(pool);
	
	private  ExecutorService manualPool = Executors.newFixedThreadPool(threadCount);
	private  CompletionService<Long> manualService = new ExecutorCompletionService<Long>(manualPool);
	
	@Autowired
	private ConfigRepository configRepository;
	private volatile boolean isRunning = false;
	
	//30分钟一次
	//"0 0/30 * * * ?"
	@Scheduled(cron = "0 0/5 * * * ?")
	public void execute() {
		logger.info("start execute job......................");
		
		if(!enabledRunChannel(CHANNEL, configRepository)) {
			logger.warn("the running setting is off now");
			isRunning = false;
			return;
		}
		
		if(!isUploadCompeleted()) {
			logger.warn("not upload compeleted now");
			return;
		}
		
		if(!isFileUploaded(null)) {
			logger.warn("no file upload now for channel={}.",CHANNEL);
			isRunning = false;
			return;
		}
		List<String> nameList = getAllExecutesFilesInput(null);
		if(CollectionUtils.isEmpty(nameList)) {
			logger.warn("no file upload now for channel={}.",CHANNEL);
			isRunning = false;
			return;
		}
		
		logger.info("start execute file......................");
		long startTime = System.currentTimeMillis();
		isRunning = true;
		int i = 0;
		int j =0;
		for(String name: nameList) {
			Task task = taskRepository.findByNameAndChannel(name, CHANNEL);
			if(null !=task && !TaskStatus.CANCELED.getStatus().equalsIgnoreCase(task.getStatus()) ) {
				i++;
				continue;
			} 
			j++;
			cService.submit(new TaskService(task,name,null,TaskExecuteType.JOB.getStatus()));
		}
		if(i == nameList.size()) {
			logger.info("finish execute..no new file upload....................");
			isRunning = false;
			return;
		}
		AtomicLong totalCou = new AtomicLong(0L);
		for(int k=0;k<j; k++) {
			try {
				Future<Long> cou = cService.take();
				try {
					totalCou.addAndGet(cou.get());
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
		isRunning = false;
		hdfsUtil.writeFile(getHotCloundOutputDir(null), getToday()+".done", "query preMachine finished");
		long endTime = System.currentTimeMillis();
		logger.info("finish execute......totalCou={},costTime={}min................",totalCou.get(),(endTime-startTime)/60000);
		hdfsUtil.closeFileSystem();
		//pool.shutdown();
	}
	
	private boolean isUploadCompeleted() {
		return hdfsUtil.isExists(getHotCloundInputDir(null)+"/"+getToday()+".done");
	}
	
	public void executeManual(String date,String fileName,String executeType) throws InterruptedException {
		
		logger.info("==============threadCount={}",threadCount);
//		while(isRunning) {
//			Thread.sleep(5000);
//		}
		
		if(StringUtils.isNotBlank(fileName)) {
			processPerTask(null, fileName, executeType,date);
			return;
		}
		
		if(StringUtils.isBlank(date) || date.length() != 8) {
			throw new IllegalArgumentException("date is not right");
		}
		if(!isFileUploaded(date)) {
			logger.warn("no file upload now for channel={}.",CHANNEL);
			return;
		}
		List<String> nameList = getAllExecutesFilesInput(date);
		if(CollectionUtils.isEmpty(nameList)) {
			logger.warn("no file upload now for channel={}.",CHANNEL);
			return;
		}
		
		logger.info("start manual execute......................");
		long startTime = System.currentTimeMillis();
		for(String name: nameList) {
			manualService.submit(new TaskService(null,name,date,TaskExecuteType.MANUAL.getStatus()));
		}
		AtomicLong totalCou = new AtomicLong(0L);
		for(String name: nameList) {
			try {
				Future<Long> cou = manualService.take();
				try {
					totalCou.addAndGet(cou.get());
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
		hdfsUtil.writeFile(getHotCloundOutputDir(date), date+".done", "query preMachine finished");
		long endTime = System.currentTimeMillis();
		logger.info("finish manual execute......totalCou={},costTime={}min................",totalCou.get(),(endTime-startTime)/60000);
		hdfsUtil.closeFileSystem();
		manualPool.shutdown();
	}
	
	
	class TaskService implements Callable<Long> {
		private String name;
		private Task task;
		private String date;
		private String type;
		
		public TaskService(Task task,String name,String date,String type) {
			this.task = task;
			this.name = name;
			this.date = date;
			this.type = type;
		}
		
		public Long call() {
			try {
				return processPerTask(task, name,type,date);
			} catch(Exception e) {
				logger.error("run task fileName={} errMsg={},err={}",name,e.getMessage(),e);
				return 0L;
			}
		}
		
	}
	
	public Long processPerTask(Task task,String fileName,String executeType,String date) {
		
		if(TaskExecuteType.JOB.getStatus().equals(executeType)) {
			Task t = taskRepository.findByNameAndChannel(fileName, CHANNEL);
			if(null !=t && !TaskStatus.CANCELED.getStatus().equalsIgnoreCase(t.getStatus()) ) {
				logger.warn("this file has already process,fileName={}",fileName);
				return 0L;
			}
		}
		
		if(null == task) {
			Task t = new Task();
			t.setName(fileName);
			t.setStartTime(new Date());
			t.setStatus(TaskStatus.PROCESSING.getStatus());
			t.setChannel(CHANNEL);
			
			TaskHistory th = new TaskHistory();
			BeanUtils.copyProperties(t, th);
			
			t = taskRepository.save(t);
			th.setTaskId(t.getId());
			th.setExecuteType(executeType);
			th = taskHistoryRepository.save(th);
			
			long completedCount = readFile(getHotCloundInputDir(date), fileName,date);
			t.setCompletedCount(completedCount);
			th.setCompletedCount(completedCount);
			t.setStatus(TaskStatus.COMPLETED.getStatus());
			th.setStatus(TaskStatus.COMPLETED.getStatus());
			Date endDate = new Date();
			t.setEndTime(endDate);
			th.setEndTime(endDate);
			taskRepository.save(t);
			taskHistoryRepository.save(th);
			return completedCount;
		} else {
			TaskHistory th = new TaskHistory();
			th.setName(fileName);
			th.setStartTime(new Date());
			th.setStatus(TaskStatus.PROCESSING.getStatus());
			th.setChannel(CHANNEL);
			th.setExecuteType(executeType);
			
			long completedCount = readFile(getHotCloundInputDir(date), fileName,date);
			th.setCompletedCount(completedCount);
			task.setCompletedCount(completedCount);
			task.setStatus(TaskStatus.RERUN_COMPLETED.getStatus());
			th.setStatus(TaskStatus.COMPLETED.getStatus());
			Date endDate = new Date();
			task.setEndTime(endDate);
			th.setEndTime(endDate);
			taskRepository.save(task);
			taskHistoryRepository.save(th);
			return completedCount;
		}
	}
	
	
	private long readFile(String path,String fileName,String date){
		String mediaCode = null;
		String today = null;
		try {
			String[] fn = fileName.split("_");
			today = fn[0];
			mediaCode = fn[1];
		} catch(Exception e) {
			logger.error("fileName format is not right={}",fileName);
		}
		if(null == today || null == mediaCode) {
			throw new IllegalArgumentException("fileName format is not right");
		}
		logger.info("start process fileName={}",fileName);
		long startTime = System.currentTimeMillis();
		long completedCount = hdfsUtil.readWriteFileLineByLine(path, fileName,mediaCode,today,getHotCloundOutputDir(date),channelFactory.getChannelService(ChannelConst.TALKING_DATA_CHANNEL));
		long endTime = System.currentTimeMillis();
		logger.info("finish process fileName={},completedCount={},costTime={}min",fileName,completedCount,(endTime-startTime)/60000);
		return completedCount;
	}
	
	public List<String> getFilesNameListInput(String date) {
		return hdfsUtil.getFileNameList(getHotCloundInputDir(date));
	}
	
	
	public List<String> getAllExecutesFilesInput(String date) {
		List<String> list =  hdfsUtil.getFileNameList(getHotCloundInputDir(date));
		String file = (null == date ? getToday() : date) + ".done";
		if(CollectionUtils.isNotEmpty(list)) {
			for(int i=0; i<list.size(); i++) {
				if(file.equals(list.get(i))) {
					list.remove(i);
				}
			}
		}
		return list;
	}
	
	public List<String> getFilesNameListOutput(String date) {
		return hdfsUtil.getFileNameList(getHotCloundOutputDir(date));
	}
	
	private boolean isFileUploaded(String date) {
		return hdfsUtil.isExists(getHotCloundInputDir(date));
	}
	
	public String getHotCloundInputDir(String date) {
		String day = null;
		if(null == date) {
			day = DateUtil.formatYYYYMMdd(new Date());
		} else {
			day = date;
		}
		return  HDFS_BASE_DIR + CHANNEL + "/" + day + "/input";
	}
	
	private String getToday() {
		return DateUtil.formatYYYYMMdd(new Date());
	}
	
	public String getHotCloundOutputDir(String date) {
		String day = null;
		if(null == date) {
			day = DateUtil.formatYYYYMMdd(new Date());
		} else {
			day = date;
		}
		return  HDFS_BASE_DIR + CHANNEL + "/" + day + "/output";
	}


	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getChannel() {
		return CHANNEL;
	}

	@Override
	public String getInputDirByDay(String day) {
		return  HDFS_BASE_DIR + CHANNEL + "/" + day + "/input";
	}

	@Override
	public String getOutputDirByDay(String day) {
		return  HDFS_BASE_DIR + CHANNEL + "/" + day + "/output";
	}
	
}

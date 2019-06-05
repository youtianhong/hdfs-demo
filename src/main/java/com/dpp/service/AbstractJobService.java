package com.dpp.service;

import java.util.Date;
import java.util.List;

import com.dpp.common.util.DateUtil;
import com.dpp.domain.Config;
import com.dpp.repository.ConfigRepository;

/**
* @author youguoqiang
* @version 2019年3月1日 上午9:57:00
* @description 
*/
public abstract class AbstractJobService {

	protected boolean enabledRunChannel(String channel,ConfigRepository configRepository) {
		Config cfg = configRepository.findCfgByChannel(channel);
		if(null != cfg && "off".equalsIgnoreCase(cfg.getIsJobRun())) {
			return false;
		}
		return true;
	}
	
	public String getInputDir(String date) {
		String day = null;
		if(null == date) {
			day = DateUtil.formatYYYYMMdd(new Date());
		} else {
			day = date;
		}
		return getInputDirByDay(day);
	}
	
	public String getOutputDir(String date) {
		String day = null;
		if(null == date) {
			day = DateUtil.formatYYYYMMdd(new Date());
		} else {
			day = date;
		}
		return getOutputDirByDay(day);
	}
	
	public abstract void executeManual(String date,String fileName,String executeType) throws InterruptedException;
	
	public abstract String getChannel();
	
	public abstract String getInputDirByDay(String day);
	
	public abstract String getOutputDirByDay(String day);
	
	public abstract List<String> getFilesNameListInput(String date);
	
	public abstract List<String> getFilesNameListOutput(String date);
}

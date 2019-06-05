package com.dpp.domain;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
* @author youguoqiang
* @version 2019年3月5日 上午10:06:12
* @description 
*/
@Document(collection="config")
public class Config {

	@Id
	private String id;
	private String isRun; //for while running 
	private String isJobRun;// for start
	private Date updateTime;
	private String channel;
	
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getIsJobRun() {
		return isJobRun;
	}
	public void setIsJobRun(String isJobRun) {
		this.isJobRun = isJobRun;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getIsRun() {
		return isRun;
	}
	public void setIsRun(String isRun) {
		this.isRun = isRun;
	}
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	
	
}

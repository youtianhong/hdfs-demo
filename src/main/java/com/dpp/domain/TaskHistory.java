package com.dpp.domain;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
* @author youguoqiang
* @version 2019年2月27日 下午1:25:28
* @description 
*/
@Document(collection="task_history")
public class TaskHistory {

	@Id
	private String id;
	private String taskId;
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	public String getExecuteType() {
		return executeType;
	}
	public void setExecuteType(String executeType) {
		this.executeType = executeType;
	}
	private String name;
	private String channel;
	private String desc;
	private String status;
	private String executeType;
	private long completedCount;
	private int skipedCount;
	private Date startTime;
	private Date endTime;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public long getCompletedCount() {
		return completedCount;
	}
	public void setCompletedCount(long completedCount) {
		this.completedCount = completedCount;
	}
	public int getSkipedCount() {
		return skipedCount;
	}
	public void setSkipedCount(int skipedCount) {
		this.skipedCount = skipedCount;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
}

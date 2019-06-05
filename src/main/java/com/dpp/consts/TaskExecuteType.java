package com.dpp.consts;

public enum TaskExecuteType {
	
	JOB("JOB","JOB批处理"),
	MANUAL("MANUAL","手工执行");

	private String status;
	private String message;

	private TaskExecuteType(String status, String message) {
		this.status = status;
		this.message = message;
	}

	public static String getMessageByStatus(String status) {
		for (TaskExecuteType ps : TaskExecuteType.values()) {
			if (ps.getStatus().equals(status)) {
				return ps.getMessage();
			}
		}
		return null;
	}

	public String getStatus() {
		return status;
	}


	public String getMessage() {
		return message;
	}
}

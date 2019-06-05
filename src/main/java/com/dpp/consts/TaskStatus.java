package com.dpp.consts;

public enum TaskStatus {
	
	PROCESSING("P","处理中"),
	FAILED("F","处理失败"),
	CANCELED("CANCELED","已取消"),
	COMPLETED("C","处理完成"),
	RERUN_COMPLETED("RC","处理完成"),
	COMPLETED_WITH_SKIP("CWS","COMPLETED_WITH_SKIP");

	private String status;
	private String message;

	private TaskStatus(String status, String message) {
		this.status = status;
		this.message = message;
	}

	public static String getMessageByStatus(String status) {
		for (TaskStatus ps : TaskStatus.values()) {
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

package com.dpp.domain;
/**
* @author youguoqiang
* @version 2019年2月26日 上午11:07:48
* @description 
*/
public class TalkingDMatchResultReqDomain implements ReqDomain {

	private String mediaId;
	private String[] campaignId;
	
	private int deviceIdType;
	public String getMediaId() {
		return mediaId;
	}
	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}
	public String[] getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String[] campaignId) {
		this.campaignId = campaignId;
	}
	public int getDeviceIdType() {
		return deviceIdType;
	}
	public void setDeviceIdType(int deviceIdType) {
		this.deviceIdType = deviceIdType;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	private String deviceId;
	
	
}

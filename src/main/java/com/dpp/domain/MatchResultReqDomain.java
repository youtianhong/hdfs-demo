package com.dpp.domain;
/**
* @author youguoqiang
* @version 2019年2月26日 上午11:07:48
* @description 
*/
public class MatchResultReqDomain implements ReqDomain {

	private String media_id;
	public String getMedia_id() {
		return media_id;
	}
	public void setMedia_id(String media_id) {
		this.media_id = media_id;
	}
	private String[] camp_ids;
	/**
	设备ID类型（1：idfa；2：imei；3：idfa-md5；4：imei-md5）
	（idfa、imei明文大写；idfa-md5、imei-md5明文大写进行md5计算后保持小写）
	（idfa、imei明文大写；idfa-md5、imei-md5明文大写进行md5计算后保持小写）
	*/
	private int deviceid_type;
	private String device_id;
	
	public String[] getCamp_ids() {
		return camp_ids;
	}
	public void setCamp_ids(String[] camp_ids) {
		this.camp_ids = camp_ids;
	}
	public int getDeviceid_type() {
		return deviceid_type;
	}
	public void setDeviceid_type(int deviceid_type) {
		this.deviceid_type = deviceid_type;
	}
	public String getDevice_id() {
		return device_id;
	}
	public void setDevice_id(String device_id) {
		this.device_id = device_id;
	}
	
	
}

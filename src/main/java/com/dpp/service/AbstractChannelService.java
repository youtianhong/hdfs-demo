package com.dpp.service;

import com.dpp.domain.ReqDomain;

/**
* @author youguoqiang
* @version 2019年5月6日 下午2:04:57
* @description 
*/
public abstract class AbstractChannelService {

	public abstract String executeMatchPreMachine(String fileName,String line,String mediaCode,String today);
	
	public abstract String queryMatchResult(ReqDomain reqDomain);
}

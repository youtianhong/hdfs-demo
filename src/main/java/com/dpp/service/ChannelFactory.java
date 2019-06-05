package com.dpp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.dpp.consts.ChannelConst;

/**
* @author youguoqiang
* @version 2019年5月6日 下午2:16:35
* @description 
*/
@Component
public class ChannelFactory {
	
	@Autowired 
	private ApplicationContext applicationContext;

	public AbstractChannelService getChannelService(String channel) {
		if(ChannelConst.HOT_CLOUND_CHANNEL.equalsIgnoreCase(channel)) {
			Class<?> clazz = HotCloundService.class;
			HotCloundService service = (HotCloundService)applicationContext.getAutowireCapableBeanFactory().autowire(clazz, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
			return service;
		} else if(ChannelConst.TALKING_DATA_CHANNEL.equalsIgnoreCase(channel)) {
			Class<?> clazz = TalkingDataService.class;
			TalkingDataService service = (TalkingDataService)applicationContext.getAutowireCapableBeanFactory().autowire(clazz, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
			return service;
		} else {
			throw new IllegalArgumentException("can not support channel="+channel); 
		}
	}
	
	public AbstractJobService getChannelJobService(String channel) {
		if(ChannelConst.HOT_CLOUND_CHANNEL.equalsIgnoreCase(channel)) {
			Class<?> clazz = HotCloundJobService.class;
			HotCloundJobService service = (HotCloundJobService)applicationContext.getAutowireCapableBeanFactory().autowire(clazz, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
			return service;
		} else if(ChannelConst.TALKING_DATA_CHANNEL.equalsIgnoreCase(channel)) {
			Class<?> clazz = TalkingDataJobService.class;
			TalkingDataJobService service = (TalkingDataJobService)applicationContext.getAutowireCapableBeanFactory().autowire(clazz, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
			return service;
		} else {
			throw new IllegalArgumentException("can not support channel="+channel); 
		}
	}
}

package com.dpp.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

public class LocationsSettingConfigFileApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static Logger logger = LoggerFactory.getLogger(LocationsSettingConfigFileApplicationListener.class);
	private static boolean hasError = false;
	private static String errMsg = "GlobalConfig is not initialized";
	private final static String APP_CONFIG_FILE_NAME = "application.properties";
	private final static String PROPERTY_FILE_EXTENSION = "properties";
	private final static String YAML_FILE_EXTENSION = "yml";
	private final static String DEFAULT_CONFIG_DIR = "/data/app/dmp/config";

	/**
	 * this should run before ConfigFileApplicationListener so it can set its
	 * state accordingly
	 */
	//@Override
	public int getOrder() {
		return ConfigFileApplicationListener.DEFAULT_ORDER - 1;
	}

	//@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		SpringApplication app = event.getSpringApplication();
		for (ApplicationListener<?> listener : app.getListeners()) {
			if (listener instanceof ConfigFileApplicationListener) {
				ConfigFileApplicationListener cfal = (ConfigFileApplicationListener) listener;
				// getSearchLocations omitted
				String fileStr = findFiles();
				if(!StringUtils.isEmpty(fileStr)){
					cfal.setSearchLocations(fileStr);
				}
			}
		}
	}

	private String findFiles() {
		List<FileSystemResource> configFiles = Lists.newArrayList();
		//////////////////////////////////// 载入应用配置////////////////////////////////////
		InputStream appConfigInputStream = LocationsSettingConfigFileApplicationListener.class
				.getResourceAsStream("/" + APP_CONFIG_FILE_NAME);
		Properties appProperties = new Properties();
		if (appConfigInputStream != null) {
			try {
				appProperties.load(appConfigInputStream);
			} catch (IOException e) {
				hasError = true;
				errMsg = e.getMessage();
				logger.error(e.getMessage(), e);
			}
		} else {
			hasError = true;
			errMsg = "can't find application.properties in classpath";
		}
		String appCode = appProperties.getProperty("APP_CODE");
		String configCode = appProperties.getProperty("CONFIG_CODE");
		// 检查参数
		if (!hasError && StringUtils.isEmpty(appCode)) {
			hasError = true;
			errMsg = "APP_CODE can't be empty";
		}
		if (!hasError) {
		    String configDir = System.getProperty("config.dir");
		    if(StringUtils.isEmpty(configDir)) {
		        configDir = DEFAULT_CONFIG_DIR;
		    }
			// 查找额制定的目录
			String additionalConfigDirPath = System.getProperty("additional.config.dir");
			if (!StringUtils.isEmpty(additionalConfigDirPath)) {
				File zfdir = new File(additionalConfigDirPath);
				addConfigFileFromDir(configFiles, zfdir);
			}
			// 查找ZF目录
			if (!StringUtils.isEmpty(appCode)) {
				File zfdir = new File(configDir, appCode);
				addConfigFileFromDir(configFiles, zfdir);
			}
			// 查找config目录
			if (!StringUtils.isEmpty(configCode)) {
				if(!configCode.equals(appCode)){
					File zfdir = new File(configDir, configCode);
					addConfigFileFromDir(configFiles, zfdir);
				}
			}
		}
		if (hasError) {
			logger.error("GlobalConfig init failed . " + errMsg);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < configFiles.size(); i++) {
			try {
				sb.append(configFiles.get(i).getURI().toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (i < configFiles.size()-1) {
				sb.append(",");
			}
		}
		logger.info("Load config files from {}", sb.toString());
		return sb.toString();
	}
	
	private void addConfigFileFromDir(List<FileSystemResource> configFiles, File configDir){
		if (configDir.exists() && configDir.isDirectory()) {
			Collection<File> propertiesFiles = FileUtils.listFiles(configDir,
					new String[] { PROPERTY_FILE_EXTENSION, YAML_FILE_EXTENSION }, false);
			for (File file : propertiesFiles) {
				configFiles.add(new FileSystemResource(file));
			}
		}
	}
}
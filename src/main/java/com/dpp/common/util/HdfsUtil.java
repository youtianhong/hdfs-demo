package com.dpp.common.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.crsh.cli.impl.descriptor.IllegalParameterException;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.dpp.domain.Config;
import com.dpp.repository.ConfigRepository;
import com.dpp.service.AbstractChannelService;
import com.dpp.service.HotCloundService;

/**
* @author youguoqiang
* @version 2019年2月28日 上午9:57:26
* @description 
*/
@Service
public class HdfsUtil {
	
	@Value("${currentEnv}")
	private String env;
	
	@Value("${hdfs.defaultFS}")
	private  String hdfsUrl;
	@Value("${hdfs.homeDir}")
	private  String hdfsHomeDir = "/";
	@Value("${hdfs.user}")
	private  String hdfsUser;
	@Value("${hdfs.nameservices}")
	private String nameServices;
	@Value("${hdfs.namenodes}")
	private String nameNodes;
	@Value("${hdfs.rpc-address-namenode671}")
	private String rpcAddressNameNode671;
	@Value("${hdfs.rpc-address-namenode673}")
	private String rpcAddressNameNode673;
	@Value("${hdfs.client_failover_proxy}")
	private String clientFailOverProxy;
	@Value("${hdfs.keytab_file}")
	private String keyTabFile;
	@Value("${hdfs.kerberos_principal}")
	private String kerberosPrincipal;
	
	
	private static Configuration conf = null;
	private static final Logger logger = LoggerFactory.getLogger(HdfsUtil.class);
	private static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static ExecutorService pool = Executors.newFixedThreadPool(5);
	private static CompletionService<Long> cService = new ExecutorCompletionService<Long>(pool);
	private static Map<String,FileSystem> fsMap = new ConcurrentHashMap<String,FileSystem>(); 
	private Lock lock = new ReentrantLock();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static Map<String,String> isRunMap = new ConcurrentHashMap<String,String>();
	
	public static final String EAGLE_KEYTAB_FILE_KEY = "eagle.keytab.file";
	public static final String EAGLE_USER_NAME_KEY = "eagle.kerberos.principal";
	
	@Autowired
	private HotCloundService hotCloundService;
	
	@Autowired
	private ConfigRepository configRepository;
	
	
	@PostConstruct
	public void init() {
		// ====== Init HDFS File System Object
		conf = new Configuration();
		logger.info("current env={}",env);
		
		if("dev".equals(env)) {
			setLocalCfg();
		} else {
			setProdCfg();
		}
		
		scheduler.scheduleWithFixedDelay(new Runnable() {
			//@Override
			public void run() {
				syncCache();
			}
		}, 30, 30, TimeUnit.SECONDS);
	}

	private void setLocalCfg() {
		// Set FileSystem URI
		conf.set("fs.defaultFS", hdfsUrl);
		conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
		conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
		conf.setBoolean("fs.hdfs.impl.disable.cache", true);
		// Set HADOOP user
		System.setProperty("HADOOP_USER_NAME", hdfsUser);
		System.setProperty("hadoop.home.dir", hdfsHomeDir);
	}
	
	private void setProdCfg() {
		// Set FileSystem URI
		conf.set("fs.defaultFS", hdfsUrl);
		conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
		conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
		
		conf.set("dfs.nameservices",nameServices);
		conf.set("dfs.ha.namenodes.nswx",nameNodes);
		conf.set("dfs.namenode.rpc-address.nswx.namenode671",rpcAddressNameNode671);
		conf.set("dfs.namenode.rpc-address.nswx.namenode673",rpcAddressNameNode673);
		conf.set("dfs.client.failover.proxy.provider.nswx",clientFailOverProxy);
		conf.set("eagle.keytab.file",keyTabFile);
		conf.set("eagle.kerberos.principal",kerberosPrincipal);
		
//		  <property>
//		    <name>dfs.namenode.kerberos.principal</name>
//		    <value>hdfs/_HOST@ABC.COM</value>
//		  </property>
//		  <property>
//		    <name>dfs.namenode.kerberos.internal.spnego.principal</name>
//		    <value>HTTP/_HOST@ABC.COM</value>
//		  </property>
//		  <property>
//		    <name>dfs.datanode.kerberos.principal</name>
//		    <value>hdfs/_HOST@ABC.COM</value>
//		  </property>
		conf.set("dfs.namenode.kerberos.principal","hdfs/_HOST@ABC.COM");
		conf.set("dfs.namenode.kerberos.internal.spnego.principal","HTTP/_HOST@ABC.COM");
		conf.set("dfs.datanode.kerberos.principal","hdfs/_HOST@ABC.COM");
		conf.set("dfs.datanode.keytab.file",keyTabFile);
		conf.set("dfs.namenode.keytab.file",keyTabFile);
		conf.set("dfs.namenode.kerberos.principal.pattern","*");
		
		
		conf.setBoolean("fs.hdfs.impl.disable.cache", true);
		// Set HADOOP user
		System.setProperty("HADOOP_USER_NAME", hdfsUser);
		System.setProperty("hadoop.home.dir", hdfsHomeDir);
		
		login(conf);
	}
	
	private void login(Configuration kConfig) {
		kConfig.setBoolean("hadoop.security.authorization", true);
        kConfig.set("hadoop.security.authentication", "kerberos");
        try {
        	logger.info("hdfs config={}",JSONObject.toJSONString(kConfig));
        } catch(Exception e){
        	logger.error("toJSONString for hdfs config error={}",e.getMessage());
        }
        UserGroupInformation.setConfiguration(kConfig);
        try {
			UserGroupInformation.loginUserFromKeytab(kConfig.get(EAGLE_USER_NAME_KEY), kConfig.get(EAGLE_KEYTAB_FILE_KEY));
			logger.info("login hdfs success.");
		} catch (IOException e) {
			logger.error("login hdfs failed, errMsg={},err={}",e.getMessage(),e);
			e.printStackTrace();
		}
	}
//	 "fs.defaultFS":"hdfs://nameservice1",
//	 "dfs.nameservices": "nameservice1",
//	 "dfs.ha.namenodes.nameservice1":"namenode1,namenode2",
//	 "dfs.namenode.rpc-address.nameservice1.namenode1": "hadoopnamenode01:8020",
//	 "dfs.namenode.rpc-address.nameservice1.namenode2": "hadoopnamenode02:8020",
//	 "dfs.client.failover.proxy.provider.apollo-phx-nn-ha": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
//	 "eagle.keytab.file":"/EAGLE-HOME/.keytab/b_eagle.keytab_apd",
//	 "eagle.kerberos.principal":"eagle@EXAMPLE.COM"
	
	
	private void syncCache() {
		List<Config> cfgList = configRepository.findAll();
		
		if(CollectionUtils.isNotEmpty(cfgList)) {
			isRunMap.put("isRun", cfgList.get(0).getIsRun());
		}
	}
	
	private boolean isRun() {
		String isRun = isRunMap.get("isRun");
		if(null != isRun && "off".equalsIgnoreCase(isRun)) {
			return false;
		} else {
			return true;
		}
	}
	
	@PreDestroy
	public void destory() {
		
	}
	
	public List<String> getFileNameList(String dirName) {
		List<String> list = new ArrayList<String>();
		try {
			Path f = new Path(dirName);
			FileStatus[] status = getFileSystem().listStatus(f);
			for (FileStatus fileStatus : status) {
				list.add(fileStatus.getPath().getName());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("getFileNameList FileNotFoundException errMsg={},err={}",e.getMessage(),e);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("getFileNameList errMsg={},err={}",e.getMessage(),e);
		} finally {
			//closeFileSystem();
		}
		return list;
	}
	
	public boolean isExists(String path) {
		Path workingDir = getFileSystem().getWorkingDirectory();
		Path newFolderPath = new Path(path);
		try {
			return getFileSystem().exists(newFolderPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean mkDir(String path) {
		try {
			Path workingDir = getFileSystem().getWorkingDirectory();
			Path newFolderPath = new Path(path);
			if (!getFileSystem().exists(newFolderPath)) {
				// Create new Directory
				getFileSystem().mkdirs(newFolderPath);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public void writeFile(String path, String fileName, String fileContent) {
		FSDataOutputStream outputStream = null;
		try {
			// ==== Create folder if not exists
			Path workingDir = getFileSystem().getWorkingDirectory();
			Path newFolderPath = new Path(path);
			if (!getFileSystem().exists(newFolderPath)) {
				// Create new Directory
				getFileSystem().mkdirs(newFolderPath);
				System.out.println("Path " + path + " created.");
			}

			// ==== Write file
			System.out.println("Begin Write file into hdfs");
			// Create a path
			Path hdfswritepath = new Path(newFolderPath + "/" + fileName);
			// Init output stream
			outputStream = getFileSystem().create(hdfswritepath);
			// Cassical output stream usage
			outputStream.writeBytes(fileContent);
		} catch (IOException e) {
			logger.error("writeFile error errMsg={},err={}",e.getMessage(),e);
		} finally {
			try {
				if(null != outputStream)
					outputStream.close();
			} catch (IOException e) {
			}
		}
	}
	
	public int writeFile(String path, String fileName, List<String> fileContent) throws IllegalParameterException {
		if(null == fileContent  || CollectionUtils.isEmpty(fileContent)) {
			logger.error("writeFile param is not right,fileContent is empty.");
			throw new IllegalParameterException("writeFile param is not right,fileContent is empty.");
		}
		
		FSDataOutputStream outputStream = null;
		FileSystem fs = null;
		int finishedIndex = 0;
		try {
			// ==== Create folder if not exists
			Path workingDir = getFileSystem().getWorkingDirectory();
			Path newFolderPath = new Path(path);
			if (!getFileSystem().exists(newFolderPath)) {
				// Create new Directory
				getFileSystem().mkdirs(newFolderPath);
			}

			// Create a path
			Path hdfswritepath = new Path(newFolderPath + "/" + fileName);
			// Init output stream
			outputStream = getFileSystem().create(hdfswritepath);
			//flush per 1W
			int i = 1;
			for(String line : fileContent) {
				outputStream.writeBytes(line);
				outputStream.writeBytes(LINE_SEPARATOR);
				if(i == 10000) {
					outputStream.flush();
					i = 0;
				}
				i++;
				finishedIndex++;
			}
			outputStream.flush();
			return finishedIndex;
		} catch (IOException e) {
			e.printStackTrace();
			return finishedIndex;
		} finally {
			try {
				if(null != outputStream)
					outputStream.close();
			} catch (IOException e) {
			}
			//fileContent.clear();
		}
	}
	
	public FileSystem getFileSystem() {
		if (null != fsMap.get("fileSystem")) {
			return fsMap.get("fileSystem");
		}
		boolean getLock = lock.tryLock();
		try {
			if (null != fsMap.get("fileSystem")) {
				return fsMap.get("fileSystem");
			}
			fsMap.put("fileSystem", FileSystem.get(URI.create(hdfsUrl), conf));
			return fsMap.get("fileSystem");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("getFileSystem IOException hdfsUrl={}, errMsg={},err={}",hdfsUrl,e.getMessage(),e);
			return null;
		} catch(Exception e) {
			logger.error("getFileSystem error hdfsUrl={}, errMsg={},err={}",hdfsUrl,e.getMessage(),e);
			return null;
		}finally {
			if(getLock)
				lock.unlock();
		}
	}
	
	public void readFile(String path, String fileName) {
		FSDataInputStream inputStream = null;
		FileSystem fs = null;
		try {
			// Get the filesystem - HDFS
			fs = getFileSystem();
			// ==== Create folder if not exists
			Path newFolderPath = new Path(path);
			if (!getFileSystem().exists(newFolderPath)) {
				// Create new Directory
				getFileSystem().mkdirs(newFolderPath);
				System.out.println("Path " + path + " created.");
			}

			// Create a path
			Path hdfsreadpath = new Path(newFolderPath + "/" + fileName);
			// Init input stream
			inputStream = getFileSystem().open(hdfsreadpath);
			// Classical input stream usage
			String out = IOUtils.toString(inputStream, "UTF-8");
			System.out.println(out);
		} catch(IOException e) {
			
		} finally {
			try {
				if(null != inputStream)
					inputStream.close();
				if(null != fs)
					getFileSystem().close();
			} catch (IOException e) {
			}
		}
	}
	
	public long readWriteFileLineByLine(String path, String fileName,String mediaCode,String today,String outPutPath,AbstractChannelService channelService) {
		FSDataInputStream inputStream = null;
		BufferedReader br = null;
		FSDataOutputStream outputStream = null;
		long i=0;
		String line = null;
		try {
			// ==== Create folder if not exists
			Path newFolderPath = new Path(path);
			if (!getFileSystem().exists(newFolderPath)) {
				// Create new Directory
				getFileSystem().mkdirs(newFolderPath);
			}

			// Create a path
			Path hdfsReadpath = new Path(newFolderPath + "/" + fileName);
			// Init input stream
			inputStream = getFileSystem().open(hdfsReadpath);
			br = new BufferedReader(new InputStreamReader(inputStream));
			
			Path outPutFPath = new Path(outPutPath);
			if (!getFileSystem().exists(outPutFPath)) {
				getFileSystem().mkdirs(outPutFPath);
			}
			// Create a path
			Path hdfsWritepath = new Path(outPutFPath + "/" + fileName);
			// Init output stream
			outputStream = getFileSystem().create(hdfsWritepath);
			
			int j = 0;
			while((line = br.readLine()) != null) {
				if(!isRun()) {
					break;
				}
				String result = channelService.executeMatchPreMachine(fileName, line,mediaCode,today);
				logger.debug("preMachine request param={},result={}",line,result);
				if(null != result) {
					StringBuffer sb = new StringBuffer();
					sb.append(line).append("$result=").append(result).append(LINE_SEPARATOR);
					outputStream.writeBytes(sb.toString());
					j++;
					if(j == 10000) {
						j = 0;
						outputStream.flush();
					}
					if(i>0 && i % 100000 == 0) {
						logger.info("the {} line processed,line={}",i,sb.toString());
					}
				} else {
					StringBuffer sb = new StringBuffer();
					sb.append(line).append("$result=").append("remote request error.");
					outputStream.writeBytes(sb.toString());
					j++;
					if(j == 10000) {
						j = 0;
						outputStream.flush();
					}
					if(i>0 && i % 100000 == 0) {
						logger.info("the {} line processed,line={}",i,sb.toString());
					}
				}
				i++;
			}
			outputStream.flush();
		} catch(IOException e) {
			logger.error("readWriteFileLineByLine errMsg={},err={},param={}",e.getMessage(),e,line);
		} finally {
			try {
				if(null != outputStream)
					outputStream.close();
				if(null != br)
					br.close();
				if(null != inputStream)
					inputStream.close();
				//closeFileSystem();
			} catch (IOException e) {
				logger.error("finally close IOException errMsg={},err",e.getMessage(),e);
			}
		}
		return i;
	}
	
	
	public void closeFileSystem() {
		try {
			if(lock.tryLock(5000,TimeUnit.SECONDS)) {
				try {
					if (null != fsMap.get("fileSystem")) {
						fsMap.get("fileSystem").close();
						fsMap.clear();
					}
				} catch (IOException e) {
					e.printStackTrace();
					fsMap.clear();
				}  finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	 
	
	static int getListLen(List<String> list) {
		Field f = null;
		try {
			f = ArrayList.class.getDeclaredField("elementData");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		f.setAccessible(true);
		Object[] elementData = null;
		try {
			elementData = (Object[])f.get(list);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return elementData.length;
	}
	
}

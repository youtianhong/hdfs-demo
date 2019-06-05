package com.dpp.common.util;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.dpp.domain.MatchResultReqDomain;

@Service
public class HttpClientService {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
	
	private CloseableHttpClient httpClient;

	private RequestConfig requestConfig;
	
	private PoolingHttpClientConnectionManager cm;
	
	@PostConstruct
	public void init() {
		cm = new PoolingHttpClientConnectionManager();
		// Increase max total connection to 300
		cm.setMaxTotal(300);
		// Increase default max connection per route to 30
		cm.setDefaultMaxPerRoute(30);
		
		cm.setValidateAfterInactivity(2000);
		
		httpClient = HttpClients.custom().setConnectionManager(cm).build();
		requestConfig = RequestConfig.custom().setSocketTimeout(8000).setConnectTimeout(5000).setConnectionRequestTimeout(5000).build();
	}

	@PreDestroy
	public void destory() {
		try {
			httpClient.close();
		} catch (IOException e) {
			logger.error("desory found exception:{},{}",e.getMessage(),e);
		}
	}
	
	public String post(String url,AbstractHttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig);
		httpEntity.setContentEncoding("UTF-8");//default utf-8
		httpEntity.setContentType("application/json");
		httpPost.setEntity(httpEntity);
		httpPost.addHeader("Content-type", "application/json;charset=UTF-8");
		httpPost.addHeader("Accept", "application/json");
		try {
			return httpClient.execute(httpPost, new CustomResponseHandler());
		} catch(IOException e) {
			logger.error("post IOException {},{}",e.getMessage(),e);
			throw new IOException(e);
		}finally {
			httpPost.releaseConnection();
		}
	}
	
	
	class  CustomResponseHandler implements ResponseHandler<String>{
		//@Override
		public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		}
	}

	/**
	 * curl -l -H "Content-type: application/json" -X POST -d 
	'{"camp_ids":["be0a4b8700101217"],"media_id":"9a5d004b84c30dbc",
	"device_id":"c17ce91cbba87ba6618c6adaa2c4c70e","deviceid_type":4}'  
	 http://58.215.105.242:18080/dmp-fec/fec/v1/ads/get/
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		HttpClientService hc = new HttpClientService();
		hc.init();
		MatchResultReqDomain req = new MatchResultReqDomain();
		String[] camIds = {"be0a4b8700101217"};
		req.setCamp_ids(camIds);
		req.setMedia_id("9a5d004b84c30dbc");
		req.setDevice_id("c17ce91cbba87ba6618c6adaa2c4c70e");
		req.setDeviceid_type(4);
		String Oldjson = JSONObject.toJSONString(req);
		//String json = "{\"camp_ids\":[\"b57d08fb1ab75bcb\"],\"media_id\":\"9a5d004b84c30dbc\",\"device_id\":\"8f41f6672a88ddac2fee7554\",\"deviceid_type\":3}";
		System.out.println(Oldjson);
		
		StringEntity entity = new StringEntity(Oldjson,"UTF-8");
		String res = hc.post("http://58.215.105.242:18080/dmp-fec/fec/v1/ads/get/",entity);
		System.out.println(res);
	}
}

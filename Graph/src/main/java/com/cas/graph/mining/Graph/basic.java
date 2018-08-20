package com.cas.graph.mining.Graph;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.cas.graph.mining.util.JsonUtil;
import com.topsec.tsm.util.DateUtils;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

public class basic {
	// build a JestClientFactory to connect ES
		public static JestClientFactory userpwdfactory(String url, String user, String pw) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
					return true;
				}
			}).build();

			// skip hostname checks
			HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			SchemeIOSessionStrategy httpsIOSessionStrategy = new SSLIOSessionStrategy(sslContext, hostnameVerifier);

			JestClientFactory factory = new JestClientFactory();
			factory.setHttpClientConfig(
					new HttpClientConfig.Builder(url)
					.defaultCredentials(user, pw)
					.defaultSchemeForDiscoveredNodes("http")
					.sslSocketFactory(sslSocketFactory)
					.httpsIOSessionStrategy(httpsIOSessionStrategy)
					.readTimeout(200000)
					.build()
					);

			return factory;
		}
		public static String CurrentNextDay(String currentdate) throws ParseException
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");//设置日期格式
			
			Date current1=dateFormat.parse(currentdate);
			Calendar current2=Calendar.getInstance();
			current2.setTime(current1);
			current2.add(Calendar.DATE, 1);
			return dateFormat.format(current2.getTime());
			
		}
		public static String hashToJSON(Map<String, Object> map_arg0){
	         
	         //String json = JSON.toJSONString(map_arg0);
	         String json = JsonUtil.map2json(map_arg0);
	         
	         return json;

	     } // hashToJSON
		public static long timeTounix(String str) {
			// TODO Auto-generated method stub
			String str_timePattern =  "yyyy-MM-dd hh:mm:ss";
			long long_time = 0;
		    try{
		        /*���ո�ʽת��Ϊunixʱ���*/
		        long_time =DateUtils.fromString2Long(str, str_timePattern);
		        //System.out.println(long_date);
		        }catch(Exception e){e.printStackTrace();}
			return long_time;
		}


		
		

}

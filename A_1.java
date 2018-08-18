package A1;

import A1.ALERTcom;
import A1.rd;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Statis_2.Statis_2.Statis;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import io.searchbox.indices.CreateIndex;
import io.searchbox.params.Parameters;
import phh.basic;
import io.searchbox.indices.IndicesExists;

import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class A_1 {
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		// read config file
		Properties props = new Properties();
		String propertyPath = "./config/A1/configa-1.properties"; // path of
																	// config
																	// file
		props.load(new FileInputStream(propertyPath)); // load config file

		String processDate = props.getProperty("processDate"); // deal with
																// which day's
																// data
		String auditFilePath = props.getProperty("auditFilePath"); // audit csv
																	// file path

		String url = props.getProperty("url");
		String user = props.getProperty("user");
		String pw = props.getProperty("pw");

		// set connection to ES
		JestClientFactory factory = basic.userpwdfactory(url, user, pw);
		JestClient client = factory.getObject();
		// System.out.println("Connect to ES successfully!");

		// set indextype
		String indextype = "jg_liuliang";
		String startdate = processDate;
		Go(client, startdate, indextype, auditFilePath);

	}

	public static void Go(JestClient client, String startdate, String indextype, String auditFilePath)
			throws Exception {
		String currentdate = startdate.trim();
		//
		while (true) {
			// if the currentdate equals the current system date
			Calendar now = Calendar.getInstance();
			Date now1 = now.getTime();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
			String now2 = dateFormat.format(now1); // 当前最新日期
			if (currentdate.equalsIgnoreCase(now2)) {
				// next
				now.add(Calendar.DATE, 1);
				now.set(Calendar.HOUR_OF_DAY, 7);
				now.set(Calendar.MINUTE, 0);
				now.set(Calendar.SECOND, 0);
				Date next1 = now.getTime();

				long dalta = (next1.getTime() - now1.getTime());

				Thread.sleep(dalta);
				// System.out.println("ret");
				continue;
			}
			// all sip_dip in audit csv file in one day
			System.out.println("deal with " + currentdate + " audit.csv data");
			Set<String> audit = readAudit(currentdate, auditFilePath);
			// all sip_dip in liuliang in one day

			if (audit.isEmpty()) { // no record, exit
				System.out.println("there no record  of audit file on " + currentdate);
			} else {
				Set<String> liuliang = readLiuliang(client, currentdate, indextype);
				if (!liuliang.isEmpty()) {
					// compare two set
					int alter_num = compareSets(client, liuliang, audit, auditFilePath, startdate);
					System.out.println("there are " + alter_num + " alter messages put into JG_gaojing_" + currentdate);
				}
			}
			// 继续检查下一天
			currentdate = basic.CurrentNextDay(currentdate);
		}

	}

	// deal with .csv file, to get one day's all sip_dip couple(no repeat)
	public static Set<String> readAudit(String date, String filePath) {
		Set<String> result = new HashSet<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			reader.readLine(); // delete file title
			String line = null;
			while ((line = reader.readLine()) != null) {
				String item[] = line.split(","); // split very line with ","
				// item[0] = CREATIONTIME; item[1] = SIP; item[2] = DIP
				String date_time = item[0]; // yyyy-mm-dd hh:mm:ss
				String current_date = getdate(date_time); // change to yyyymmdd
															// formate
				if (current_date.equals(date)) {
					String sip_dip = item[1] + "_" + item[2]; // SIP_DIP
					result.add(sip_dip);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	// return yyyymmdd from yyyy-mm-dd hh:mm:ss
	public static String getdate(String date_time) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		Date current1 = dateFormat.parse(date_time);
		SimpleDateFormat newformat = new SimpleDateFormat("yyyyMMdd");
		String date = newformat.format(current1);
		return date;
	}

	// get one day JG_liuliang 's all sip_dip
	public static Set<String> readLiuliang(JestClient client, String date, String indextype) throws Exception {
		Set<String> result = new HashSet<String>();

		// set indextype
		String query = "{}";
		int size = 1000;
		String indexname = indextype + "_" + date; // table name :
													// 'jg_liuliang_20180129'

		IndicesExists indicesExists = new IndicesExists.Builder(indexname).build();
		JestResult result2 = client.execute(indicesExists);

		if (!result2.isSucceeded()) {
			// 该时间的index不存在
			System.out.println(indexname + "这一天在es中不存在，继续检查下一天");
			// Create articles index
			return result;
		}

		Search search = new Search.Builder(query)
				// multiple index or types can be added.
				.addIndex(indexname).addType(indextype).setParameter(Parameters.SIZE, size)
				.setParameter(Parameters.SCROLL, "1m").build();

		SearchResult result1 = client.execute(search);
		String resultJson = result1.getJsonString();
		addElements(result, resultJson);

		// get total
		JsonParser parse = new JsonParser();
		JsonObject json = (JsonObject) parse.parse(resultJson);
		JsonObject hits = json.get("hits").getAsJsonObject();

		int total = hits.get("total").getAsInt();
		int count = total / size + 1;

		// iterator
		String scrollId;
		for (int itera = 1; itera < count; itera++) {
			scrollId = result1.getJsonObject().get("_scroll_id").getAsString();

			// read more
			SearchScroll scroll = new SearchScroll.Builder(scrollId, "1m").setParameter(Parameters.SIZE, size).build();

			JestResult searchResult = client.execute(scroll);

			resultJson = searchResult.getJsonString();
			addElements(result, resultJson);
		}
		return result;
	}

	public static void addElements(Set<String> result, String resultJson) {
		JsonParser parse = new JsonParser();
		JsonObject json = (JsonObject) parse.parse(resultJson);
		JsonObject hits = json.get("hits").getAsJsonObject();
		JsonArray hits2 = hits.get("hits").getAsJsonArray();

		for (int i = 0; i < hits2.size(); i++) {
			// deal with every record in ES
			JsonObject tmp = hits2.get(i).getAsJsonObject();
			JsonObject tmp2 = tmp.get("_source").getAsJsonObject();
			JsonObject dst = tmp2.get("dst").getAsJsonObject();// dst
			String dip = dst.get("ip").getAsString(); // dip
			JsonObject src = tmp2.get("src").getAsJsonObject();// src
			String sip = src.get("ip").getAsString(); // sip
			String sip_dip = sip + dip;
			result.add(sip_dip);
		}
	}

	// to find out which sip_dip appear in audit but not in liuliang
	public static int compareSets(JestClient client, Set<String> liuliang, Set<String> audit, String filePath,
			String date) {
		int count = 0;
		for (String sip_dip : audit) {
			// System.out.println(str);
			// audit's sip_dip is not in liuliang.json
			if (!liuliang.contains(sip_dip)) {
				// gaojing
				AlertMessage(client, sip_dip, filePath, date); // generate alter
																// massage and
																// write to ES
				count++;
			} else
				continue;
		}
		return count;
	}

	public static JestResult indicesExists(String indexname, JestClient client) {
		IndicesExists indicesExists = new IndicesExists.Builder(indexname).build();
		JestResult result = null;
		try {
			result = client.execute(indicesExists);
			System.out.println("indicesExists == " + result.getJsonString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// put the alert message into ES(all sip_dip matched in csv file, different
	// time)
	private static void AlertMessage(JestClient client, String sip_dip, String filePath, String date) {
		String indexname = "JG_gaojing_" + date;
		try {
			// if the indexname not exit
			System.out.println(indexname);
			JestResult result = indicesExists(indexname, client);
			if (!result.isSucceeded()) {
				System.out.println("this index does not exit!");
				CreateIndex index = new CreateIndex.Builder(indexname).build();
				JestResult jr = client.execute(index);
				System.out.println("Create success!");
			}
			// other field contents search from csv file based on sip_dip
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			reader.readLine(); // delete file title
			String line = null;
			while ((line = reader.readLine()) != null) {
				String item[] = line.split(","); // split very line with ","
				// item[0] = CREATIONTIME; item[1] = SIP; item[2] = DIP
				String date_time = item[0]; // yyyy-mm-dd hh:mm:ss
				String current_date = getdate(date_time);
				if (current_date.equals(date)) { // same date
					String temp_sip_dip = item[1] + "_" + item[2]; // SIP_DIP
					if (temp_sip_dip.equals(sip_dip)) {
						// this record should write in ES
						rd rd_entity = new rd();
						rd_entity.setDip(item[2]);
						rd_entity.setSip(item[1]);
						ALERTcom alter = new ALERTcom();
						String[] asset = new String[] { item[1], item[2] };
						alter.setasset(asset);
						alter.setdescription("疑似违规外联");
						alter.setlogtime(basic.timeTounix(item[0])); // logtime
																		// to
																		// save
																		// creationtime
						alter.setrd(rd_entity);
						alter.setrisklevel("2");
						alter.setver("1");

						Index index2 = new Index.Builder(alter).index(indexname).type(indexname).build();
						JestResult jr2 = client.execute(index2);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

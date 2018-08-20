package com.cas.graph.mining.Graph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSONArray;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

public class ReadES_Weight {

	public static void main(String[] args) throws InterruptedException, IOException, ParseException,
			KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NSQException, TimeoutException {
		System.setOut(new PrintStream(new FileOutputStream("output.txt")));
		/*
		 * PropertyConfigurator.configure( "./config/log4j.properties" ); Logger
		 * logger = Logger.getLogger(ReadES_Weight.class); logger.info("start");
		 */

		// read config file
		Properties props = new Properties();
		String propertyPath = "./config/config.txt"; // path of config file
		props.load(new FileInputStream(propertyPath)); // load config file
		String startdate = props.getProperty("startdate");
		String enddate = props.getProperty("enddate");
		System.out.println(enddate);
		String url = props.getProperty("url");
		String user = props.getProperty("user");
		String pw = props.getProperty("pw");

		// String numSize = props.getProperty("size");
		// set connection to ES
		JestClientFactory factory = basic.userpwdfactory(url, user, pw);
		// JestClientFactory factory=basic.userpwdfactory(url,user,pw);
		JestClient client = factory.getObject();
		System.out.println("*****Connecting ES successfully！*****");

		String indextype = "jg_liuliang";
		System.out.println("*****Start Dealing with ES*****");
		// The first param is date,the second param is Ip addresses of everyday.

		HashMap<String, HashMap<String, Integer>> ipNode = new HashMap<String, HashMap<String, Integer>>();
		// The first param is date, the second param is Sip and Dip.
		HashMap<String, HashMap<String, HashMap<String, Integer>>> ipEdge = new HashMap<String, HashMap<String, HashMap<String, Integer>>>();

		// begin
		searchES(client, startdate, enddate, indextype, ipNode, ipEdge);

		System.out.println("*****Finished!*****");

	}

	public static void searchES(JestClient client, String startdate, String enddate, String indextype,
			HashMap<String, HashMap<String, Integer>> ipNode,
			HashMap<String, HashMap<String, HashMap<String, Integer>>> ipEdge) throws IOException, ParseException {

		String query = "{}";
		int size = 3000;
		String currentdate = startdate.trim();
		String stopdate = enddate.trim();
		Boolean num = true;
		Boolean num2 = true;

		while (true) {
			HashMap<String, Integer> nodeOfToday = new HashMap<String, Integer>();
			HashMap<String, HashMap<String, Integer>> edgeOfToday = new HashMap<String, HashMap<String, Integer>>();

			String indexname = indextype + "_" + currentdate;
			System.out.println("正在处理" + indexname + ".");
			Search search = new Search.Builder(query)
					// multiple index or types can be added.
					.addIndex(indexname).addType(indextype).setParameter(Parameters.SIZE, size)
					.setParameter(Parameters.SCROLL, "1m").build();
			SearchResult result = client.execute(search);
			if (!result.isSucceeded()) {
				System.out.println(indexname + "未查询到日志");
				// currentdate=basic.CurrentNextDay(currentdate);
				// continue;
			} else {
				String resultJson = result.getJsonString();
				num = readIP(resultJson, indexname, nodeOfToday, edgeOfToday);
				/*
				 * System.out.println(indexname + "的节点数：" + nodeOfToday.size() +
				 * "边数是：" + edgeOfToday.size());
				 */
				if (num == true) {
					JsonParser parse = new JsonParser();
					JsonObject json = (JsonObject) parse.parse(resultJson);
					JsonObject hits = json.get("hits").getAsJsonObject();
					int total = hits.get("total").getAsInt();
					int count = total / size + 1;
					System.out.println(total);
					// iterator
					String scrollId;
					for (int itera = 1; itera < count; itera++) {
						scrollId = result.getJsonObject().get("_scroll_id").getAsString();

						// read more
						SearchScroll scroll = new SearchScroll.Builder(scrollId, "1m")
								.setParameter(Parameters.SIZE, size).build();

						JestResult searchResult = client.execute(scroll);

						resultJson = searchResult.getJsonString();
						num2 = readIP(resultJson, indexname, nodeOfToday, edgeOfToday);
						if (num2 == false) {
							break;
						}

					}
				}

				// After dealling with currentdate, write data to HashMap
				ipNode.put(currentdate, nodeOfToday);
				ipEdge.put(currentdate, edgeOfToday);
			}

			if (currentdate.equalsIgnoreCase(stopdate)) {
				// Finish searchES,then writeToGraph
				// writeForGBAD(ipNode, ipEdge);
				writeForMining(ipNode, ipEdge);
				// writeForFiveHun(ipNode, ipEdge);
				return;
			} else {
				currentdate = basic.CurrentNextDay(currentdate);
			}

		}

	}

	// Read everyday's IP
	public static Boolean readIP(String resultJson, String date, HashMap<String, Integer> nodeOfToday,
			HashMap<String, HashMap<String, Integer>> edgeOfToday) {
		JsonParser parse = new JsonParser();
		JsonObject json = (JsonObject) parse.parse(resultJson);
		JsonObject hits = json.get("hits").getAsJsonObject();
		JsonArray hits2 = hits.get("hits").getAsJsonArray();
		String[] va = date.split("_"); // date
										// (index_name)"jg_liuliang_20180726"
		String value = va[va.length - 1]; // value "20180726"
		for (int i = 0; i < hits2.size(); i++) {
			JsonObject tmp = hits2.get(i).getAsJsonObject();
			JsonObject tmp2 = tmp.get("_source").getAsJsonObject();
			JsonObject dst = tmp2.get("dst").getAsJsonObject();// dst
			// Get dip and dport
			String dip = dst.get("ip").getAsString();
			String dport = dst.get("port").getAsString();
			JsonObject src = tmp2.get("src").getAsJsonObject();// src
			// Get sip and sport
			String sip = src.get("ip").getAsString();
			String sport = src.get("port").getAsString();
			// Get proto
			String pro1 = tmp2.get("proto").getAsJsonArray().get(0).getAsString();
			String pro2 = tmp2.get("proto").getAsJsonArray().get(1).getAsString();
			String id = tmp2.get("id").getAsString();
			String source = sip + sport;
			String dest = dip + dport;
			String transport = pro1 + "_" + pro2;
			/*** Check whether over 500 ***/
			/*
			 * if(nodeOfToday.size() < 500){ if(!nodeOfToday.containsKey(sip)){
			 * nodeOfToday.put(sip, nodeOfToday.size() + 1); }
			 * if(!nodeOfToday.containsKey(dip)){ nodeOfToday.put(dip,
			 * nodeOfToday.size() + 1); } String edge = sip + ":" + dip;
			 * if(edgeOfToday.containsKey(edge)){ //There are the same edge
			 * HashMap<String, Integer> sameEdgeWeight = edgeOfToday.get(edge);
			 * if(sameEdgeWeight.containsKey(transport)){ int transValue =
			 * sameEdgeWeight.get(transport); transValue ++;
			 * sameEdgeWeight.put(transport, transValue); }else{
			 * sameEdgeWeight.put(transport, 1); }
			 * 
			 * }else{ //There are not the same edge (sip->dip) HashMap<String,
			 * Integer> edgeWeight = new HashMap<String, Integer>();
			 * edgeWeight.put(transport, 1); edgeOfToday.put(edge, edgeWeight);
			 * }
			 * 
			 * }else{ return false; }
			 */
			if (sip.equals(dip)) {
				continue;
			} else {
				if (!nodeOfToday.containsKey(sip)) {
					/* node number start from 0 */
					nodeOfToday.put(sip, nodeOfToday.size());
					/* node number start from 1 */
					// nodeOfToday.put(sip, nodeOfToday.size() + 1);
				}
				if (!nodeOfToday.containsKey(dip)) {
					// nodeOfToday.put(dip, nodeOfToday.size() + 1);
					nodeOfToday.put(dip, nodeOfToday.size());
				}

				String edge = sip + ":" + dip;
				if (edgeOfToday.containsKey(edge)) {
					// There are the same edge
					HashMap<String, Integer> sameEdgeWeight = edgeOfToday.get(edge);
					if (sameEdgeWeight.containsKey(transport)) {
						int transValue = sameEdgeWeight.get(transport);
						transValue++;
						sameEdgeWeight.put(transport, transValue);
					} else {
						sameEdgeWeight.put(transport, 1);
					}

				} else {
					// There are not the same edge (sip->dip)
					HashMap<String, Integer> edgeWeight = new HashMap<String, Integer>();
					edgeWeight.put(transport, 1);
					edgeOfToday.put(edge, edgeWeight);
				}
			}

			// System.out.println("*******From source: sip:" + sip +"sport:" +
			// sport + " To dest: dip:" + dip + "dport:" + dport + "******");
		}
		// System.out.println(value + "日节点的数量是：" + nodeOfToday.size());
		return true;
	}

	// Get ipNode and ipEdge, then write to graph.txt.
	public static void writeForGBAD(HashMap<String, HashMap<String, Integer>> ipNode,
			HashMap<String, HashMap<String, HashMap<String, Integer>>> ipEdge) throws IOException {
		// TODO Auto-generated method stub
		File graph = new File("graph.g");
		FileWriter fw = new FileWriter(graph);
		BufferedWriter writer = new BufferedWriter(fw);

		// all date
		Set<String> keys = ipNode.keySet();
		System.out.println("所有的日期数：" + keys.size());
		int dateNum = 0;
		Iterator<String> dates = keys.iterator();
		while (dates.hasNext()) {
			// Anaylize everyday's nodes and edges

			String date = dates.next();
			System.out.println("当前日期是：" + date);
			HashMap<String, Integer> dayNode = ipNode.get(date);
			HashMap<String, HashMap<String, Integer>> dayEdge = ipEdge.get(date);
			if (dayNode.size() < 10) {
				continue;
			} else {
				dateNum++;
				System.out.println("日期" + date + "的节点数是：" + dayNode.size());
				// The first line ' XP # 4 '
				writer.write("XP # " + dateNum);
				writer.newLine();
				// Sort by value desc
				// As for Node
				List<Map.Entry<String, Integer>> list_ip = new ArrayList<Map.Entry<String, Integer>>(
						dayNode.entrySet());
				Collections.sort(list_ip, new Comparator<Map.Entry<String, Integer>>() {
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						if (o2.getValue() != null && o1.getValue() != null
								&& o2.getValue().compareTo(o1.getValue()) < 0) {
							return 1;
						} else {
							return -1;
						}

					}
				});

				for (Entry<String, Integer> entry : list_ip) {
					writer.write("v " + entry.getValue() + " \"" + entry.getKey() + "\"");
					writer.newLine();
				}

				// As for Edge
				System.out.println("日期" + date + "的边数是：" + dayEdge.size());

				for (String ipKey : dayEdge.keySet()) {
					String[] edgeArray = ipKey.split(":");
					String sip = edgeArray[0];
					String dip = edgeArray[1];
					Integer snode = dayNode.get(sip);
					Integer dnode = dayNode.get(dip);
					HashMap<String, Integer> weightValue = dayEdge.get(ipKey);
					String weight = "|";
					for (String protoKey : weightValue.keySet()) {
						int protoNum = weightValue.get(protoKey);
						weight = weight + protoKey + ":" + protoNum + "|";
					}
					writer.write("e " + String.valueOf(snode) + " " + String.valueOf(dnode) + " \"" + weight + "\"");
					writer.newLine();
				}

			}
		}
		writer.flush();
		writer.close();
		fw.close();
	}

	// Get ipNode and ipEdge, then write to graph.txt.
	public static void writeForFiveHun(HashMap<String, HashMap<String, Integer>> ipNode,
			HashMap<String, HashSet<String>> ipEdge) throws IOException {
		// TODO Auto-generated method stub
		File graph = new File("graph500.g");
		FileWriter fw = new FileWriter(graph);
		BufferedWriter writer = new BufferedWriter(fw);

		String date = "20180508";
		// all date
		int dateNum = 0;
		while (dateNum <= 10) {
			// Anaylize everyday's nodes and edges

			System.out.println("当前日期是：" + date);
			HashMap<String, Integer> dayNode = ipNode.get(date);
			HashSet<String> dayEdge = ipEdge.get(date);
			if (dayNode.size() < 10) {
				continue;
			} else {
				dateNum++;
				System.out.println("日期" + date + "的节点数是：" + dayNode.size());
				// The first line ' XP # 4 '
				writer.write("XP # " + dateNum);
				writer.newLine();
				// Sort by value desc
				// As for Node
				List<Map.Entry<String, Integer>> list_ip = new ArrayList<Map.Entry<String, Integer>>(
						dayNode.entrySet());
				Collections.sort(list_ip, new Comparator<Map.Entry<String, Integer>>() {
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						if (o2.getValue() != null && o1.getValue() != null
								&& o2.getValue().compareTo(o1.getValue()) < 0) {
							return 1;
						} else {
							return -1;
						}

					}
				});

				for (Entry<String, Integer> entry : list_ip) {
					writer.write("v " + entry.getValue() + " \"" + entry.getKey() + "\"");
					writer.newLine();
				}

				// As for Edge
				System.out.println("日期" + date + "的边数是：" + dayEdge.size());
				Iterator<String> edges = dayEdge.iterator();
				while (edges.hasNext()) {
					String edge = edges.next();
					String[] edgeArray = edge.split(":");
					String sip = edgeArray[0];
					String dip = edgeArray[1];
					Integer snode = dayNode.get(sip);
					Integer dnode = dayNode.get(dip);
					writer.write("e " + String.valueOf(snode) + " " + String.valueOf(dnode) + " \"e\"");
					writer.newLine();
				}
			}
		}
		writer.flush();
		writer.close();
		fw.close();
	}

	public static void writeForMining(HashMap<String, HashMap<String, Integer>> ipNode,
			HashMap<String, HashMap<String, HashMap<String, Integer>>> ipEdge) throws IOException {
		// TODO Auto-generated method stub

		File community = new File("network.txt");
		FileWriter fw = new FileWriter(community);
		BufferedWriter writer = new BufferedWriter(fw);

		File node = new File("nodeForCom.txt");
		FileWriter fwn = new FileWriter(node);
		BufferedWriter Nwriter = new BufferedWriter(fwn);

		String date = "20180702";
		HashMap<String, Integer> dayNode = ipNode.get(date);
		HashMap<String, HashMap<String, Integer>> dayEdge = ipEdge.get(date);

		System.out.println("总共节点数是：" + dayNode.size());
		int size = dayNode.size();
		writer.write(Integer.toString(size));
		writer.newLine();
		for (String ipKey : dayEdge.keySet()) {
			String[] edgeArray = ipKey.split(":");
			String sip = edgeArray[0];
			String dip = edgeArray[1];
			Integer snode = dayNode.get(sip);
			Integer dnode = dayNode.get(dip);
			String conIp = dip + ":" + sip;

			HashMap<String, Integer> SDEdge = dayEdge.get(ipKey);
			int SDNum = 0;
			for (String proto : SDEdge.keySet()) {
				SDNum += SDEdge.get(proto);
			}
			/* UnDirection Edges */
			if (!dayEdge.containsKey(conIp)) {

				writer.write(String.valueOf(dnode) + " " + String.valueOf(snode) + " " + SDNum);
				writer.newLine();
				writer.write(String.valueOf(snode) + " " + String.valueOf(dnode) + " " + SDNum);
				writer.newLine();
			} else {
				HashMap<String, Integer> DSEdge = dayEdge.get(conIp);
				int DSNum = 0;
				for (String proto : DSEdge.keySet()) {
					DSNum += DSEdge.get(proto);
				}
				int total = SDNum + DSNum;
				if (total > 5)
					System.out.println("某条无向边的权重是：" + total);
				writer.write(String.valueOf(snode) + " " + String.valueOf(dnode) + " " + total);
				writer.newLine();
			}

			/* Direction Edges */
			/*
			 * writer.write(String.valueOf(snode) + " " + String.valueOf(dnode)
			 * + " " + SDNum); writer.newLine();
			 */

		}

		// AS for node
		// As for Node
		List<Map.Entry<String, Integer>> list_ip = new ArrayList<Map.Entry<String, Integer>>(dayNode.entrySet());
		Collections.sort(list_ip, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				if (o2.getValue() != null && o1.getValue() != null && o2.getValue().compareTo(o1.getValue()) < 0) {
					return 1;
				} else {
					return -1;
				}

			}
		});

		for (Entry<String, Integer> entry : list_ip) {
			Nwriter.write("v " + entry.getValue() + " \"" + entry.getKey() + "\"");
			Nwriter.newLine();
		}

		writer.flush();
		writer.close();
		fw.close();
		Nwriter.flush();
		Nwriter.close();
		fwn.close();
	}
}

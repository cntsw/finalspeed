// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.pcap4j.core.Pcaps;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import net.fs.rudp.Route;

public class FSClient {

	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("b", "back", false, "有此参数则运行CLI版本");
		options.addOption("min", "minimize", false, "启动窗口最小化");
		CommandLine commandLine = null;
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar finalspeed.jar [-b/--back]", options);
			System.exit(0);
		}

		new FSClient().run();
	}

	MapClient mapClient;
	ClientConfig config = null;
	String configFilePath = "client_config.json";

	private void run() {
		loadConfig();
		boolean tcpEnvSuccess = false;
		try {
			Pcaps.findAllDevs();
			tcpEnvSuccess = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			mapClient = new MapClient(tcpEnvSuccess);
		} catch (final Exception e1) {
			e1.printStackTrace();
		}

//		if (!mapClient.route_tcp.capEnv.tcpEnable) {
//			System.out.println("无可用网络接口,只能使用udp协议.");
//		}

		mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getRemotePort(), null, null,
				config.isDirect_cn(), config.getProtocal().equals("tcp"), null);

		Route.localDownloadSpeed = config.getDownloadSpeed();
		Route.localUploadSpeed = config.getUploadSpeed();
	}

	ClientConfig loadConfig() {
		ClientConfig cfg = new ClientConfig();
		if (!new File(configFilePath).exists()) {
			System.err.println(configFilePath + " not exists");
			return cfg;
		}
		try {
			String content = readFileUtf8(configFilePath);
			JSONObject json = JSONObject.parseObject(content);
			cfg.setServerAddress(json.getString("server_address"));
			cfg.setServerPort(json.getIntValue("server_port"));
			cfg.setRemotePort(json.getIntValue("remote_port"));
			cfg.setRemoteAddress(json.getString("remote_address"));
			if (json.containsKey("direct_cn")) {
				cfg.setDirect_cn(json.getBooleanValue("direct_cn"));
			}
			cfg.setDownloadSpeed(json.getIntValue("download_speed"));
			cfg.setUploadSpeed(json.getIntValue("upload_speed"));
			if (json.containsKey("socks5_port")) {
				cfg.setSocks5Port(json.getIntValue("socks5_port"));
			}
			if (json.containsKey("protocal")) {
				cfg.setProtocal(json.getString("protocal"));
			}
			if (json.containsKey("auto_start")) {
				cfg.setAutoStart(json.getBooleanValue("auto_start"));
			}
			if (json.containsKey("recent_address_list")) {
				JSONArray list = json.getJSONArray("recent_address_list");
				for (int i = 0; i < list.size(); i++) {
					cfg.getRecentAddressList().add(list.get(i).toString());
				}
			}

			config = cfg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cfg;
	}

	public static String readFileUtf8(String path) throws IOException {
		File file = new File(path);
		InputStream in = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		in.read(buffer);
		return new String(buffer, "utf-8");
	}
}

// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Random;

import net.fs.rudp.ClientProcessorInterface;
import net.fs.rudp.ConnectionProcessor;
import net.fs.rudp.Route;
import net.fs.rudp.TrafficEvent;
import net.fs.rudp.Trafficlistener;
import net.fs.utils.NetStatus;

public class MapClient implements Trafficlistener{

	ConnectionProcessor imTunnelProcessor;

	Route route_udp,route_tcp;

	short routePort=45;

	String serverAddress="";

	InetAddress address=null;

	int serverPort=130;

	NetStatus netStatus;

	long lastTrafficTime;

	int downloadSum=0;

	int uploadSum=0;

	Thread clientUISpeedUpdateThread;

	int connNum=0;
	
	HashSet<ClientProcessorInterface> processTable=new HashSet<ClientProcessorInterface>();
	
	Object syn_process=new Object();
	
	static MapClient mapClient;
	
	PortMapManager portMapManager;
		
	public String mapdstAddress;
	 
	public int mapdstPort;
	
	String systemName=System.getProperty("os.name").toLowerCase();
	
	boolean useTcp=true;
	
	long clientId;

	Random ran=new Random();
	
	boolean tcpEnable;

	MapClient(boolean tcpEnvSuccess) throws Exception {
		mapClient=this;
		try {
			route_tcp = new Route(null,routePort,Route.mode_client,true,tcpEnvSuccess);
		} catch (Exception e1) {
			throw e1;
		}
		try {
			route_udp = new Route(null,routePort,Route.mode_client,false,tcpEnvSuccess);
		} catch (Exception e1) {
			throw e1;
		}
		netStatus=new NetStatus();
		portMapManager=new PortMapManager(this);
		Route.addTrafficlistener(this);
	}
	
	public static MapClient get(){
		return mapClient;
	}
	
	public void setMapServer(String serverAddress,int serverPort,int remotePort,String passwordMd5,String password_proxy_Md5,boolean direct_cn,boolean tcp,
			String password){
		if(this.serverAddress==null
				||!this.serverAddress.equals(serverAddress)
				||this.serverPort!=serverPort){
			
			if(route_tcp.lastClientControl!=null){
				route_tcp.lastClientControl.close();
			} 
			
			if(route_udp.lastClientControl!=null){
				route_udp.lastClientControl.close();
			} 
		}
		this.serverAddress=serverAddress;
		this.serverPort=serverPort;
		this.address=null;
		this.useTcp=tcp;
	}

	void saveFile(byte[] data,String path) throws Exception{
		FileOutputStream fos=null;
		try {
			fos=new FileOutputStream(path);
			fos.write(data);
		} catch (Exception e) {
			throw e;
		} finally {
			if(fos!=null){
				fos.close();
			}
		}
	}

	public void onProcessClose(ClientProcessorInterface process){
		synchronized (syn_process) {
			processTable.remove(process);
		}
	}
	
	public void trafficDownload(TrafficEvent event) {
		////#MLog.println("下载 "+event.getTraffic());
		netStatus.addDownload(event.getTraffic());
		lastTrafficTime=System.currentTimeMillis();
		downloadSum+=event.getTraffic();
	}

	public void trafficUpload(TrafficEvent event) {
		////#MLog.println("上传 "+event.getTraffic());
		netStatus.addUpload(event.getTraffic());
		lastTrafficTime=System.currentTimeMillis();
		uploadSum+=event.getTraffic();
	}

	public boolean isUseTcp() {
		return useTcp;
	}

	public void setUseTcp(boolean useTcp) {
		this.useTcp = useTcp;
	}
}

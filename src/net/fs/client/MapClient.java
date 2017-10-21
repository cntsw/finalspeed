// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.net.InetAddress;
import java.util.HashSet;

import net.fs.rudp.ClientProcessorInterface;
import net.fs.rudp.ConnectionProcessor;
import net.fs.rudp.Route;
import net.fs.rudp.TrafficEvent;
import net.fs.rudp.Trafficlistener;
import net.fs.utils.NetStatus;
import net.fs.utils.StaticUtil;

public class MapClient implements Trafficlistener{

	ConnectionProcessor imTunnelProcessor;

	Route route_udp; //route_tcp

	short routePort=45;

	String serverAddress="";

	InetAddress address=null;

	int serverPort=130;

	NetStatus netStatus;

	long lastTrafficTime;

	int downloadSum=0;

	int uploadSum=0;

	HashSet<ClientProcessorInterface> processTable=new HashSet<ClientProcessorInterface>();

	Object syn_process=new Object();

	static MapClient mapClient;

	PortMapManager portMapManager;

	MapClient(boolean tcpEnvSuccess) throws Exception {
		mapClient=this;
		try {
			route_udp = new Route(null,routePort,Route.mode_client,false,tcpEnvSuccess);
		} catch (Exception e1) {
			throw e1;
		}
		netStatus=new NetStatus();
		portMapManager=new PortMapManager(this);
		Route.addTrafficlistener(this);

		Thread t = new Thread(()->{
			long dSum;
			String dSumStr;
			while(true){
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				dSum = netStatus.downloadSum / 1024 / 1024;
				dSumStr = dSum + "MB";
				if (dSum > 999L) {
					dSum /= 1024;
					dSumStr = dSum + "GB";
				}
				String s = String.format("d:%4dkB/s %6s; u:%3dkB/s; d:%5dms; p%s  ",
						netStatus.downSpeed/1024,
						dSumStr,
						netStatus.upSpeed/1024,
						StaticUtil.delay,
						StaticUtil.getLastPortStatics());
				System.out.print('\r' + s);
			}
		});
		t.setDaemon(true);
		t.start();
	}

	public static MapClient get(){
		return mapClient;
	}

	public void setMapServer(String serverAddress,int serverPort,int remotePort,String passwordMd5,String password_proxy_Md5,boolean direct_cn,boolean tcp,
			String password){
		if(this.serverAddress==null
				||!this.serverAddress.equals(serverAddress)
				||this.serverPort!=serverPort){

			if(route_udp.lastClientControl!=null){
				route_udp.lastClientControl.close();
			}
		}
		this.serverAddress=serverAddress;
		this.serverPort=serverPort;
		this.address=null;
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
}

// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
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

	//ClientUII ui;

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
	
	static int monPort=25874;
	
	String systemName=System.getProperty("os.name").toLowerCase();
	
	boolean useTcp=true;
	
	long clientId;

	Random ran=new Random();
	
	boolean tcpEnable;

	MapClient(boolean tcpEnvSuccess) throws Exception {
		//this.ui=ui;
		mapClient=this;
		try {
			final ServerSocket socket=new ServerSocket(monPort);
			new Thread(){
				public void run(){
					try {
						socket.accept();
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}.start();
		} catch (Exception e) {
			//e.printStackTrace();
			System.exit(0);
		}
		try {
			route_tcp = new Route(null,routePort,Route.mode_client,true,tcpEnvSuccess);
		} catch (Exception e1) {
			//e1.printStackTrace();
			throw e1;
		}
		try {
			route_udp = new Route(null,routePort,Route.mode_client,false,tcpEnvSuccess);
		} catch (Exception e1) {
			//e1.printStackTrace();
			throw e1;
		}
		netStatus=new NetStatus();
		
		portMapManager=new PortMapManager(this);

		clientUISpeedUpdateThread=new Thread(){
			public void run(){
				while(true){
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
//					updateUISpeed();
				}
			}
		};
		clientUISpeedUpdateThread.start();
		
		Route.addTrafficlistener(this);
		
	}
	
	public static MapClient get(){
		return mapClient;
	}

//	private void updateUISpeed(){
//		if(ui!=null){
//			ui.updateUISpeed(connNum,netStatus.getDownSpeed(),netStatus.getUpSpeed());
//		}
//	}
	
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
		address=null;
		useTcp=tcp;
		resetConnection();
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


	int getRow_linux(){
		int row_delect=-1;
		String cme_list_rule="iptables -L -n --line-number";
		//String [] cmd={"netsh","advfirewall set allprofiles state on"};
		Thread errorReadThread=null;
		try {
			final Process p = Runtime.getRuntime().exec(cme_list_rule,null);

			errorReadThread=new Thread(){
				public void run(){
					InputStream is=p.getErrorStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							if (line == null){ 
								break;
							}else{ 
								//System.out.println("erroraaa "+line);
							}
						} catch (IOException e) {
							e.printStackTrace();
							//error();
							break;
						}
					}
				}
			};
			errorReadThread.start();



			InputStream is=p.getInputStream();
			BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
			while (true){
				String line; 
				try {
					line = localBufferedReader.readLine();
				//	System.out.println("standaaa "+line);
					if (line == null){ 
						break;
					}else{ 
						if(line.contains("tcptun_fs")){
							int index=line.indexOf("   ");
							if(index>0){
								String n=line.substring(0, index);
								try {
									if(row_delect<0){
										//System.out.println("standaaabbb "+line);
										row_delect=Integer.parseInt(n);
									}
								} catch (Exception e) {

								}
							}
						};
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}


			errorReadThread.join();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			//error();
		}
		return row_delect;
	}
	
	void resetConnection(){
		synchronized (syn_process) {
			
		}
	}
	
	public void onProcessClose(ClientProcessorInterface process){
		synchronized (syn_process) {
			processTable.remove(process);
		}
	}

	synchronized public void closeAndTryConnect(){
		close();
		//testPool();
	}

	public void close(){
		//closeAllProxyRequest();
		//poolManage.close();
		//CSocketPool.closeAll();
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

	static void runCommand(String command){
		Thread standReadThread=null;
		Thread errorReadThread=null;
		try {
			final Process p = Runtime.getRuntime().exec(command,null);
			standReadThread=new Thread(){
				public void run(){
					InputStream is=p.getInputStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							//System.out.println("stand "+line);
							if (line == null){ 
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}
					}
				}
			};
			standReadThread.start();

			errorReadThread=new Thread(){
				public void run(){
					InputStream is=p.getErrorStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							if (line == null){ 
								break;
							}else{ 
								//System.out.println("error "+line);
							}
						} catch (IOException e) {
							e.printStackTrace();
							//error();
							break;
						}
					}
				}
			};
			errorReadThread.start();
			standReadThread.join();
			errorReadThread.join();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			//error();
		}
	}

	public boolean isUseTcp() {
		return useTcp;
	}

	public void setUseTcp(boolean useTcp) {
		this.useTcp = useTcp;
	}
}

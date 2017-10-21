// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.pcap4j.core.Pcaps;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import net.fs.rudp.Route;
import net.fs.utils.LogOutputStream;
import net.fs.utils.MLog;
import net.miginfocom.swing.MigLayout;

public class ClientUI implements ClientUII, WindowListener {
    MapClient mapClient;

    ClientConfig config = null;

    String configFilePath = "client_config.json";

    String logoImg = "img/offline.png";

    String offlineImg = "img/offline.png";

    String name = "FinalSpeed";

    private TrayIcon trayIcon;

    private SystemTray tray;

    boolean checkingUpdate = false;

    String domain = "";

    //public static ClientUI ui;

    boolean ky = true;

    String errorMsg = "保存失败请检查输入信息!";

    boolean capSuccess = false;
    Exception capException = null;
    boolean b1 = false;

    String systemName = null;

    public boolean osx_fw_pf = false;

    public boolean osx_fw_ipfw = false;


    LogFrame logFrame;
    
    LogOutputStream los;
    
    boolean tcpEnable=true;
    
    {
        domain = "ip4a.com";
    }

    ClientUI(final boolean aaa,boolean bbb) {
        	 los=new LogOutputStream(System.out);
             System.setOut(los);
             System.setErr(los);
        
        systemName = System.getProperty("os.name").toLowerCase();
        MLog.info("System: " + systemName + " " + System.getProperty("os.version"));
        
        //ui = this;
        loadConfig();

        boolean tcpEnvSuccess=true;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Pcaps.findAllDevs();
                    b1 = true;
                } catch (Exception e3) {
                    e3.printStackTrace();

                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        //JOptionPane.showMessageDialog(mainFrame,System.getProperty("os.name"));
        if (!b1) {
        	tcpEnvSuccess=false;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        String msg = "启动失败,请先安装libpcap,否则无法使用tcp协议";
                        if (systemName.contains("windows")) {
                            msg = "启动失败,请先安装winpcap,否则无法使用tcp协议";
                        }
                        MLog.println(msg);
                        if (systemName.contains("windows")) {
                            try {
                                Process p = Runtime.getRuntime().exec("winpcap_install.exe", null);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            tcpEnable=false;
                            //System.exit(0);
                        }
                    }

                });
            } catch (InvocationTargetException e2) {
                e2.printStackTrace();
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }


        try {
            mapClient = new MapClient(this,tcpEnvSuccess);
        } catch (final Exception e1) {
            e1.printStackTrace();
            capException = e1;
            //System.exit(0);
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {

                    if (!mapClient.route_tcp.capEnv.tcpEnable) {
                        //JOptionPane.showMessageDialog(mainFrame,"无可用网络接口,只能使用udp协议.");
                    }

                    //System.exit(0);
                }

            });
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }

        mapClient.setUi(this);

        mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getRemotePort(), null, null, config.isDirect_cn(), config.getProtocal().equals("tcp"),
                null);

        setSpeed(config.getDownloadSpeed(), config.getUploadSpeed());

    }
    
    String getServerAddressFromConfig(){
    	 String server_addressTxt = config.getServerAddress();
         if (config.getServerAddress() != null && !config.getServerAddress().equals("")) {
             if (config.getServerPort() != 150
                     && config.getServerPort() != 0) {
                 server_addressTxt += (":" + config.getServerPort());
             }
         }
         return server_addressTxt;
    }

    void setSpeed(int downloadSpeed, int uploadSpeed) {
        config.setDownloadSpeed(downloadSpeed);
        config.setUploadSpeed(uploadSpeed);
//        int s1 = (int) ((float) downloadSpeed * 1.1f);
//        text_ds.setText(" " + Tools.getSizeStringKB(s1) + "/s ");
//        int s2 = (int) ((float) uploadSpeed * 1.1f);
//        text_us.setText(" " + Tools.getSizeStringKB(s2) + "/s ");
        Route.localDownloadSpeed = downloadSpeed;
        Route.localUploadSpeed = config.uploadSpeed;

        saveConfig();
    }


    void exit() {
        System.exit(0);
    }

    void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    ClientConfig loadConfig() {
        ClientConfig cfg = new ClientConfig();
        if (!new File(configFilePath).exists()) {
            JSONObject json = new JSONObject();
            try {
                saveFile(json.toJSONString().getBytes(), configFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            	JSONArray list=json.getJSONArray("recent_address_list");
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

    void saveConfig() {
        Thread thread = new Thread() {
            public void run() {
                boolean success = false;
                try {
                    int serverPort = 150;
                    String protocal = "tcp";
                    protocal = "udp";  // default udp

                    JSONObject json = new JSONObject();
                    json.put("server_address", config.getServerAddress());
                    json.put("server_port", serverPort);
                    json.put("download_speed", config.getDownloadSpeed());
                    json.put("upload_speed", config.getUploadSpeed());
                    json.put("socks5_port", config.getSocks5Port());
                    json.put("protocal", protocal);
                    json.put("auto_start", config.isAutoStart());

                    json.put("recent_address_list", config.getRecentAddressList());
                    
                    saveFile(json.toJSONString().getBytes("utf-8"), configFilePath);
                    config.setServerPort(serverPort);
                    config.setProtocal(protocal);
                    success = true;

                    String realAddress = config.getServerAddress();
                    if (realAddress != null) {
                        realAddress = realAddress.replace("[", "");
                        realAddress = realAddress.replace("]", "");
                    }

                    boolean tcp = protocal.equals("tcp");

                    mapClient.setMapServer(realAddress, serverPort, 0, null, null, config.isDirect_cn(), tcp,
                            null);
                    mapClient.closeAndTryConnect();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!success) {
                    }
                }


            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String readFileUtf8(String path) throws Exception {
        String str = null;
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            File file = new File(path);

            int length = (int) file.length();
            byte[] data = new byte[length];

            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            dis.readFully(data);
            str = new String(data, "utf-8");

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return str;
    }

    void saveFile(byte[] data, String path) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(data);
        } catch (Exception e) {
            if (systemName.contains("windows")) {
                JOptionPane.showMessageDialog(null, "保存配置文件失败,请尝试以管理员身份运行! " + path);
                System.exit(0);
            }
            throw e;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

//    public void updateUISpeed(int conn, int downloadSpeed, int uploadSpeed) {
//        String string =
//                " 下载:" + Tools.getSizeStringKB(downloadSpeed) + "/s"
//                        + " 上传:" + Tools.getSizeStringKB(uploadSpeed) + "/s";
//        if (downloadSpeedField != null) {
//            downloadSpeedField.setText(string);
//        }
//    }

    JButton createButton(String name) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 5, 0, 5));
        button.setFocusPainted(false);
        return button;
    }
    
    JButton createButton_Link(String name,final String url) {
        JButton button = new JButton(name);
        Color c = new Color(0,0,255);
        button.setBackground(c);  
        button.setForeground(new Color(100,100,255));
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setMargin(new Insets(0, 2, 0, 2));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openUrl(url);
            }
        });
        return button;
    }
   
	public static void setAutoRun(boolean run) {
		String s = new File(".").getAbsolutePath();
		String currentPaht = s.substring(0, s.length() - 1);
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(currentPaht, "\\");
		while (st.hasMoreTokens()) {
			sb.append(st.nextToken());
			sb.append("\\\\");
		}
		ArrayList<String> list = new ArrayList<String>();
		list.add("Windows Registry Editor Version 5.00");
		String name="fsclient";
//		if(PMClientUI.mc){
//			name="wlg_mc";
//		}
		if (run) {
			list.add("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run]");
			list.add("\""+name+"\"=\"" + sb.toString() + "finalspeedclient.exe -min" + "\"");
		} else {
			list.add("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run]");
			list.add("\""+name+"\"=-");
		}

		File file = null;
		try {
			file = new File("import.reg");
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			for (int i = 0; i < list.size(); i++) {
				String ss = list.get(i);
				if (!ss.equals("")) {
					pw.println(ss);
				}
			}
			pw.flush();
			pw.close();
			Process p = Runtime.getRuntime().exec("regedit /s " + "import.reg");
			p.waitFor();
		} catch (Exception e1) {
			// e1.printStackTrace();
		} finally {
			if (file != null) {
				file.delete();
			}
		}
	}

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }


    @Override
    public boolean login() {
        return false;
    }


    @Override
    public boolean updateNode(boolean testSpeed) {
        return true;

    }

    public boolean isOsx_fw_pf() {
        return osx_fw_pf;
    }

    public void setOsx_fw_pf(boolean osx_fw_pf) {
        this.osx_fw_pf = osx_fw_pf;
    }

    public boolean isOsx_fw_ipfw() {
        return osx_fw_ipfw;
    }

    public void setOsx_fw_ipfw(boolean osx_fw_ipfw) {
        this.osx_fw_ipfw = osx_fw_ipfw;
    }

}

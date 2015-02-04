package com.aaronjwood.portauthority.runnable;

import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ScanPortsRunnable implements Runnable {

    private static final String TAG = "ScanPortsRunnable";

    private String ip;
    private int startPort;
    private int stopPort;
    private HostAsyncResponse delegate;

    /**
     * Constructor to set the necessary data to perform a port scan
     *
     * @param ip        IP address
     * @param startPort Port to start scanning at
     * @param stopPort  Port to stop scanning at
     * @param delegate  Called when this chunk of ports has finished scanning
     */
    public ScanPortsRunnable(String ip, int startPort, int stopPort, HostAsyncResponse delegate) {
        this.ip = ip;
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.delegate = delegate;
    }

    /**
     * Starts the port scan
     */
    @Override
    public void run() {
        for(int i = this.startPort; i <= this.stopPort; i++) {
            try {
                this.delegate.processFinish(1);
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.connect(new InetSocketAddress(this.ip, i), 3500);

                char[] buffer = new char[1024];
                HashMap<Integer, String> portData = new HashMap<>();

                if(i == 80 || i == 443 || i == 22) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("GET / HTTP/1.1\r\n\r\n");

                    String data = null;

                    if(i == 22) {
                        data = in.readLine();
                    }
                    else if(i == 80 || i == 443) {
                        in.read(buffer, 0, 1024);
                        data = new String(buffer).toLowerCase();
                        if(data.indexOf("apache") != -1 || data.indexOf("httpd") != -1) {
                            data = "Apache";
                        }
                        else if(data.indexOf("iis") != -1 || data.indexOf("microsoft") != -1) {
                            data = "IIS";
                        }
                        else if(data.indexOf("nginx") != -1) {
                            data = "Nginx";
                        }
                    }

                    portData.put(i, data);
                    out.close();
                    in.close();
                }
                else {
                    portData.put(i, null);
                }

                socket.close();

                this.delegate.processFinish(portData);
            }
            catch(SocketException e) {
                Log.e(TAG, e.getMessage());
            }
            catch(IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}

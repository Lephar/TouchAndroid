package org.arch.touch;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.net.Socket;

public class LaunchActivity extends Activity {

    static DataOutputStream stream = null;
    static boolean connected;
    Thread[] thread;
    TextView logView;
    String log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipRaw = wifi.getConnectionInfo().getIpAddress();
        final String ipHead = (ipRaw & 0xFF) + "." + (ipRaw >> 8 & 0xFF) + "." + (ipRaw >> 16 & 0xFF) + ".";
        final int ipTail = ipRaw >> 24 & 0xFF;
        final EditText ipBox = findViewById(R.id.editTextIP);
        final EditText portBox = findViewById(R.id.editTextPort);
        ipBox.setHint(ipHead + ipTail);
        final int defPort = 10110;
        logView = findViewById(R.id.textViewLog);
        thread = new Thread[255];
        connected = false;

        findViewById(R.id.buttonConnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread[0] = new Thread() {
                    @Override
                    public void run() {
                        String ip = ipBox.getText().toString();
                        int port = Integer.parseInt(portBox.getText().toString());
                        try {
                            addLog("Connecting to " + ip + ":" + port + "...");
                            Socket socket = new Socket(ip, port);
                            stream = new DataOutputStream(socket.getOutputStream());
                            connected = true;
                            startActivity(new Intent(LaunchActivity.this, MainActivity.class));
                            LaunchActivity.this.finish();
                        } catch (Exception exception) {
                            addLog("Can't connect to " + ip + ":" + port + "!");
                        }
                    }
                };
                thread[0].start();
            }
        });

        thread[1] = new Thread() {
            @Override
            public void run() {
                while (!connected) {
                    try {
                        if (thread[0] != null)
                            thread[0].join();
                    } catch (Exception exception) {
                    }
                    for (int i = 2; i < thread.length; i++) {
                        final int ipVar = i;
                        thread[i] = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    if (ipVar != ipTail) {
                                        addLog("Scanning " + ipHead + ipVar + ":" + defPort + "...");
                                        Socket socket = new Socket(ipHead + ipVar, defPort);
                                        stream = new DataOutputStream(socket.getOutputStream());
                                        connected = true;
                                        startActivity(new Intent(LaunchActivity.this, MainActivity.class));
                                        LaunchActivity.this.finish();
                                    }
                                } catch (Exception exception) {
                                    addLog("No server at " + ipHead + ipVar + ":" + defPort + ".");
                                }
                            }
                        };
                        thread[i].start();
                    }
                    try {
                        for (int i = 2; i < thread.length; i++)
                            thread[i].join();
                    } catch (Exception exception) {
                    }
                }
            }
        };
        thread[1].start();
    }

    synchronized void addLog(String lastLog) {
        log = lastLog + "\n" + log;
        if (log.length() > 2048)
            log = log.substring(0, 2048);
        logView.setText(log);
    }
}

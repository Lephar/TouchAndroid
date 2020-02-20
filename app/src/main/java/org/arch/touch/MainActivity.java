package org.arch.touch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    float currentPressure, maxPressure;
    long currentTime, startTime, eventTime;
    int op, currentX, currentY, previousX, previousY;
    boolean connected, hold, move;
    LinkedBlockingQueue<Integer> queue;
    DataOutputStream stream = LaunchActivity.stream;
    TextView logViewLeft, logViewRight;
    String logLeft, logRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN);

        logLeft = logRight = "";
        logViewLeft = findViewById(R.id.textViewLogLeft);
        logViewRight = findViewById(R.id.textViewLogRight);
        queue = new LinkedBlockingQueue<>();
        connected = true;
        hold = move = false;

        new Thread() {
            @Override
            public void run() {
                while (connected) {
                    try {
                        op = queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
                        stream.writeInt(op);
                        if (op == 0) {
                            stream.writeInt(queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS));
                            stream.writeInt(queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS));
                        }
                    } catch (Exception exception) {
                        connected = false;
                        startActivity(new Intent(MainActivity.this, LaunchActivity.class));
                        MainActivity.this.finish();
                    }
                }
            }
        }.start();
    }

    void addLogLeft(String lastLog) {
        logLeft = logLeft + "\n" + lastLog;
        if (logLeft.length() > 2048)
            logLeft = logLeft.substring(logLeft.length() - 2048);
        logViewLeft.setText(logLeft);
    }


    void addLogRight(String lastLog) {
        logRight = logRight + "\n" + lastLog;
        if (logRight.length() > 2048)
            logRight = logRight.substring(logRight.length() - 2048);
        logViewRight.setText(logRight);
    }

    public boolean onTouchEvent(MotionEvent event) {
        try {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                eventTime = event.getEventTime();
                if (eventTime - startTime < 160) {
                    if (maxPressure < 0.56) {
                        queue.put(1);
                        queue.put(2);
                        addLogLeft("Left click");
                    } else {
                        queue.put(3);
                        queue.put(4);
                        addLogLeft("Right click");
                    }
                } else if (maxPressure > 0.72) {
                    queue.put(2);
                    hold = false;
                    addLogLeft("Stop move");
                    addLogLeft("Left release");
                } else {
                    move = false;
                    addLogLeft("Stop move");
                }
                maxPressure = 0;
            } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startTime = event.getEventTime();
                currentX = previousX = (int) event.getX();
                currentY = previousY = (int) event.getY();
            } else {
                previousX = currentX;
                previousY = currentY;
                currentX = (int) event.getX();
                currentY = (int) event.getY();
                currentPressure = event.getPressure();
                currentTime = event.getEventTime();
                if (maxPressure < currentPressure)
                    maxPressure = currentPressure;
                if (!hold && maxPressure > 0.72 && currentTime - startTime > 160) {
                    hold = true;
                    queue.put(1);
                    addLogLeft("Left hold");
                }
                if (!move && currentTime - startTime > 160) {
                    move = true;
                    addLogLeft("Start move");
                }
                queue.put(0);
                queue.put(currentX - previousX);
                queue.put(currentY - previousY);
                addLogRight("Move " + (currentX - previousX) + " " + (currentY - previousY));
            }
        } catch (Exception exception) {
        }
        return false;
    }
}

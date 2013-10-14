package com.runescape.util;

import com.runescape.Game;

public class MouseCapturer implements Runnable {

    public Game client;
    public Object objectLock = new Object();
    public int[] coordsY = new int[500];
    public boolean capturing = true;
    public int[] coordsX = new int[500];
    public int capturedEvents;

    @Override
    public void run() {
        while (capturing) {
            synchronized (objectLock) {
                if (capturedEvents < 500) {
                    coordsX[capturedEvents] = client.mouseEventX;
                    coordsY[capturedEvents] = client.mouseEventY;
                    capturedEvents++;
                }
            }
            try {
                Thread.sleep(50L);
            } catch (Exception exception) {
            }
        }
    }

    public MouseCapturer(Game client) {
        this.client = client;
    }
}

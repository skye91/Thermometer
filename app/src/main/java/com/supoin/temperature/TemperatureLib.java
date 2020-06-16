package com.supoin.temperature;

public class TemperatureLib {

    private static boolean isRunning = false;
    static {
        System.loadLibrary("native-lib");
    }

    public static native void open();
    public static native void close();
    public static native float[] readData();
    public static native float readDataENV();

    public static void startRead(final Callback cb){
        if(isRunning) return;
        isRunning = true;
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (isRunning){
                    cb.onData(readData());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public static float getEnvTem(){
        return readDataENV();
    }

    public static void stopRead(){
        if(!isRunning) return;
        isRunning = false;
    }

    public interface Callback{
        void onData(float[] data);
    }

}

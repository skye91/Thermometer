package com.supoin.thermometer.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.supoin.temperature.TemperatureLib;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.supoin.thermometer.service.DataUtils.FACE_CENTER;
import static com.supoin.thermometer.service.DataUtils.FACE_DOWN;
import static com.supoin.thermometer.service.DataUtils.FACE_LEFT;
import static com.supoin.thermometer.service.DataUtils.FACE_RIGHT;
import static com.supoin.thermometer.service.DataUtils.FACE_UP;

public class ResService extends Service {

    private static final String TAG = "ResService";

    private static final int IMG_WIDTH = 1920;//图面宽度
    private static final int IMG_HEIGHT = 1080;//图片高度
    private static final int IMG_ORIENTATION = 90;//图片旋转角度

    private static FaceEngine faceEngine = null;
    private static TextToSpeech textToSpeech = null;
    private static byte[] camData = null;

    private static DataCallback dataCallback;
    private static int distance = 0;
    private static double temperature = 0f;
    private static List<FaceInfo> faceInfoList = null;
    private static int face3d = -1;
    private static List<byte[]> saveFaceList = null;
    private static List<byte[]> saveFaceListMask = null;
    private static boolean canRecognizeFace = false;
    private static int needFaceDirection = -1;
    private static String currFaceName = "";
    private static boolean started = false;

    @SuppressLint("HandlerLeak")
    private static Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(dataCallback == null) return;
            int what = msg.what;
            switch (what){
                case 1:
                    dataCallback.onDistanceResult(distance);
                    break;
                case 2:
                    dataCallback.onTemperatureResult(temperature);
                    break;
                case 3:
                    dataCallback.onFaceInfoList(faceInfoList);
                case 4:
                    dataCallback.onFace3DAngle(face3d);
                    break;
                case 5:
                    dataCallback.onRecordFaceFeature(needFaceDirection);
                    break;
                case 6:
                    dataCallback.onFaceResult(currFaceName);
                    dataCallback.onAllResult(currFaceName,distance,temperature);
                    break;
            }
        }
    };

    public ResService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!started){
            initFaceEngine();
            initTTS();
            initFaceData();
//            initTemperatureThread();
            initTemperature614Thread();
            initDistanceThread();
            initFaceThread();
            started = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initFaceThread() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (true){
                    //人脸检测
                    if(faceEngine == null || camData == null) continue;
                    List<FaceInfo> infoList = new LinkedList<>();
                    int code = faceEngine.detectFaces(camData,IMG_WIDTH,IMG_HEIGHT,FaceEngine.CP_PAF_NV21,infoList);
                    if(code == ErrorInfo.MOK && infoList.size() == 1){
                        faceInfoList = infoList;
                        handler.sendEmptyMessage(3);
                        //人脸属性检测
                        code = faceEngine.process(camData,IMG_WIDTH,IMG_HEIGHT,FaceEngine.CP_PAF_NV21,infoList,FaceEngine.ASF_FACE3DANGLE);
                        if(code == ErrorInfo.MOK){
                            List<Face3DAngle> face3DAngleList = new ArrayList<>();
                            faceEngine.getFace3DAngle(face3DAngleList);
                            float yaw = face3DAngleList.get(0).getYaw();
                            float pitch = face3DAngleList.get(0).getPitch();

                            if(Math.abs(yaw)>Math.abs(pitch)){
                                if(Math.abs(yaw)<10){
                                    face3d = FACE_CENTER;
                                }else{
                                    face3d = yaw>0?FACE_LEFT:FACE_RIGHT;
                                }
                            }else{
                                if(Math.abs(pitch)<10){
                                    face3d = FACE_CENTER;
                                }else{
                                    face3d = pitch>0?FACE_UP:FACE_DOWN;
                                }
                            }
                            handler.sendEmptyMessage(4);
                            if(saveFaceList != null && saveFaceList.size() != 5){
                                canRecognizeFace = false;
                                if(saveFaceList.size() == face3d){
                                    FaceFeature faceFeature = new FaceFeature();
                                    int extractCode = faceEngine.extractFaceFeature(camData,IMG_WIDTH,IMG_HEIGHT,FaceEngine.CP_PAF_NV21,faceInfoList.get(0),faceFeature);
                                    if(extractCode == ErrorInfo.MOK){
                                        saveFaceList.add(faceFeature.getFeatureData());
                                    }
                                }
                                if(saveFaceList.size() != 5){
                                    needFaceDirection = saveFaceList.size();
                                    handler.sendEmptyMessage(5);
                                }else{
                                    needFaceDirection = -2;
                                    handler.sendEmptyMessage(5);
                                }
                            }
                            if(saveFaceListMask != null && saveFaceListMask.size() != 5){
                                if(saveFaceListMask.size() == 0) saveFaceListMask.add(new byte[]{0});
                                canRecognizeFace = false;
                                if(saveFaceListMask.size() == face3d){
                                    FaceFeature faceFeature = new FaceFeature();
                                    int extractCode = faceEngine.extractFaceFeature(camData,IMG_WIDTH,IMG_HEIGHT,FaceEngine.CP_PAF_NV21,faceInfoList.get(0),faceFeature);
                                    if(extractCode == ErrorInfo.MOK){
                                        saveFaceListMask.add(faceFeature.getFeatureData());
                                    }
                                }
                                if(saveFaceListMask.size() != 5){
                                    needFaceDirection = saveFaceListMask.size();
                                    handler.sendEmptyMessage(5);
                                }else{
                                    needFaceDirection = -1;
                                    handler.sendEmptyMessage(5);
                                }
                            }
                            if(canRecognizeFace){
                                FaceFeature faceFeature = new FaceFeature();
                                int extractCode = faceEngine.extractFaceFeature(camData,IMG_WIDTH,IMG_HEIGHT,FaceEngine.CP_PAF_NV21,faceInfoList.get(0),faceFeature);
                                if(extractCode == ErrorInfo.MOK){
                                    currFaceName = DataUtils.getRecognizeName(faceEngine,faceFeature);
                                    handler.sendEmptyMessage(6);
                                }
                            }
                        }
                    }else{
                        faceInfoList = null;
                        handler.sendEmptyMessage(3);
                    }
                    camData = null;

                }
            }
        }.start();
    }

    private void initDistanceThread() {
        DataUtils.enableDistanceSensor();
        new Thread(){
            @Override
            public void run() {
                super.run();
                while(true){
                    distance = DataUtils.getDistance();
                    handler.sendEmptyMessage(1);
                    DataUtils.sleep(200);
                }
            }
        }.start();
    }

    private void initTemperatureThread() {
        TemperatureLib.open();
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (true){
                    if(distance != 0) {
                        temperature = DataUtils.getTemperature(distance);
                        handler.sendEmptyMessage(2);
                    }
                    DataUtils.sleep(500);
                }
            }
        }.start();
    }
    private void initTemperature614Thread() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (true){
                    double temp = DataUtils.get614Temperature(distance);
                    if (temp != 0 && distance != 0) {
                        temperature = temp;
                        handler.sendEmptyMessage(2);
                    }
                    DataUtils.sleep(500);
                }
            }
        }.start();
    }

    private static void initFaceData() {
        DataUtils.refreshFaceData();
    }

    //初始化TTS
    private void initTTS() {
        textToSpeech = new TextToSpeech(ResService.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    textToSpeech.setPitch(1.0f);
                    int res = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                    if(res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED)
                        Log.d(TAG,"TTS language not support");
                }else{
                    Log.d(TAG,"TTS init failed");
                }
            }
        });
    }

    //初始化人脸的库
    private void initFaceEngine(){
        int code = FaceEngine.activeOnline(ResService.this,"BCpL8GDLbEQM9dA6QCU8BePnAwFaTfwBWn3g6bPj76EQ","7pP4KhSQvkARhyUfH7drdiYF1s4hgyF7wf8RNV26rNrC");
        if(code == ErrorInfo.MOK || code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            Log.d(TAG,"FaceEngine active ok");
            faceEngine = new FaceEngine();
            code = faceEngine.init(ResService.this, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,16,1,
                    FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE3DANGLE);
            if(code == ErrorInfo.MOK){
                Log.d(TAG,"FaceEngine init ok");
            }else{
                faceEngine = null;
                Log.d(TAG,"FaceEngine init failed");
            }
        }else{
            Log.d(TAG,"FaceEngine active failed");
        }
    }

    public static void setCamData(byte[] data){
        camData = data;
    }

    //播放TTS
    public static void speek(String text){
        if(textToSpeech == null) return;
        textToSpeech.setPitch(1.0f);
        textToSpeech.setSpeechRate(1.0f);
        textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }

    //播放TTS并等待，播放完毕之前不执行动作
    public static void speekSync(String text){
        if(textToSpeech == null || textToSpeech.isSpeaking()) return;
        textToSpeech.setPitch(1.0f);
        textToSpeech.setSpeechRate(1.0f);
        textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }

    public static void startRecordFaceFeature(boolean hasMask){
        if(hasMask){
            saveFaceListMask = new ArrayList<>();
        }else{
            saveFaceList = new ArrayList<>();
        }
    }

    public static boolean saveFaceToFile(String name){
        if(name == null || "".equals(name)) return false;
        DataUtils.saveFaceFeature(name,saveFaceList,saveFaceListMask);
        stopSaveFace();
        initFaceData();
        return true;
    }

    public static void deleteFaceFile(String name){
        DataUtils.deleteFaceFeature(name);
        initFaceData();
    }

    public static void stopSaveFace(){
        saveFaceList = null;
        saveFaceListMask = null;
    }

    public static void setRecognizeFace(boolean status){
        canRecognizeFace = status;

    }

    public static String getCurrDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日");
//        return sdf.format(new Date())+DataUtils.getCurrWeek();
        return sdf.format(new Date());
    }

    public static String getCurrTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date());
    }

    public static String getCurrLocation(){
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url("http://ip.ws.126.net/ipquery").build();
            Response response = client.newCall(request).execute();
            String[] res = response.body().string().split("\"");
            return res[1]+","+res[3];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void setDataCallback(DataCallback cb){
        dataCallback = cb;
    }

    public abstract static class DataCallback{
        public abstract void onFaceResult(String name);
        public abstract void onDistanceResult(int distance);
        public abstract void onTemperatureResult(double temperature);
        public abstract void onAllResult(String name, int distance, double temperature);
        public abstract void onRecordFaceFeature(int status);
        public abstract void onFaceInfoList(List<FaceInfo> faceInfoList);
        public abstract void onFace3DAngle(int Angle);
    }

}

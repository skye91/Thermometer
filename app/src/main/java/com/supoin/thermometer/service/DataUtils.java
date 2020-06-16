package com.supoin.thermometer.service;

import android.util.Log;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceSimilar;
import com.supoin.temperature.TemperatureLib;
import com.supoin.temperature.entity.PointAreaEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DataUtils {


    public static final int FACE_UP = 0;
    public static final int FACE_RIGHT = 1;
    public static final int FACE_DOWN = 2;
    public static final int FACE_LEFT = 3;
    public static final int FACE_CENTER = 4;

    public static String NAME = "";

    private static final String faceDir = "/sdcard/FaceFeature/";

    public static List<FaceFeature> featureList= null;
    public static List<String> featureNameList = null;

    public static void saveFaceFeature(String name, List<byte[]> featureList, List<byte[]> featureListMask){
        if(featureList !=null && featureList.size() == 5){
            saveFile(name+"-center-nomask",featureList.get(0));
            saveFile(name+"-up-nomask",featureList.get(1));
            saveFile(name+"-right-nomask",featureList.get(2));
            saveFile(name+"-down-nomask",featureList.get(3));
            saveFile(name+"-left-nomask",featureList.get(4));
        }

        if(featureListMask != null && featureListMask.size() == 5){
            saveFile(name+"-center-mask",featureListMask.get(0));
//            saveFile(name+"-up-mask",featureListMask.get(1));
            saveFile(name+"-right-mask",featureListMask.get(2));
            saveFile(name+"-down-mask",featureListMask.get(3));
            saveFile(name+"-left-mask",featureListMask.get(4));
        }
    }

    public static void deleteFaceFeature(String name){
        File baseFile = new File(faceDir);
        String[] fileList = baseFile.list();
        if(fileList != null && fileList.length != 0){
            for(String file:fileList){
                try {
                    if(file.startsWith(name)){
                        File faceFile = new File(faceDir+file);
                        faceFile.delete();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static void saveFile(String name, byte[] data){
        try{
            File dir = new File(faceDir);
            dir.mkdirs();
            File file = new File(faceDir+name);
            if(!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void refreshFaceData(){
        featureList = null;
        featureNameList = null;
        new Thread(){
            @Override
            public void run() {
                super.run();
                List<FaceFeature> tempFeatureList= null;
                List<String> tempFeatureNameList = null;

                File baseFile = new File(faceDir);
                String[] fileList = baseFile.list();
                if(fileList != null && fileList.length != 0){
                    tempFeatureList = new ArrayList<>();
                    tempFeatureNameList = new ArrayList<>();
                    for(String file:fileList){
                        try {
                            tempFeatureNameList.add(file.split("-")[0]);
                            byte[] data = new byte[(int) new File(faceDir+file).length()];
                            new FileInputStream(faceDir + file).read(data);
                            tempFeatureList.add(new FaceFeature(data));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    featureList = tempFeatureList;
                    featureNameList = tempFeatureNameList;
                }
            }
        }.start();
    }

    public static String getRecognizeName(FaceEngine engine, FaceFeature feature){
        if(featureList == null || featureNameList == null) return "";
        FaceSimilar faceSimilar = new FaceSimilar();
        float maxSimilar = 0f;
        int maxSimilarIndex = -1;
        for (int i = 0; i < featureList.size(); i++) {
            int compareCode = engine.compareFaceFeature(feature,featureList.get(i), faceSimilar);
            if (compareCode == ErrorInfo.MOK) {
                if (faceSimilar.getScore() > maxSimilar) {
                    maxSimilar = faceSimilar.getScore();
                    maxSimilarIndex = i;
                }
            }
        }
        return maxSimilar>0.98f?featureNameList.get(maxSimilarIndex):"";
    }

    public static void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void enableDistanceSensor(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    FileOutputStream fos = new FileOutputStream(new File("/sys/devices/virtual/input/input1/enable_ps_sensor"));
                    fos.write("0".getBytes());
                    Thread.sleep(100);
                    fos.write("1".getBytes());
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //读取距离值并返回
    public static int getDistance(){
        int distance = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/devices/virtual/input/input1/RangeMilliMeter"));
            String distance_str = br.readLine().substring(0,8);
            if(distance_str.startsWith("0000")){
                distance = Integer.parseInt(distance_str.substring(distance_str.length()-4),16);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distance/10;
    }

    public static double getTemperature(int distance){
        float[] data = TemperatureLib.readData();
        PointAreaEntity mPointAreaEntity = new PointAreaEntity();
        double temperature = dataArea(splitArrayData(data,mPointAreaEntity)) + 0.7589*distance/10+0.2395;
        if (temperature < 34.90) temperature = temperature +2.0143;
        return temperature;
    }
    public static double get614Temperature(int distance){
        //90614温度值
        double temperature = get614TemperatureFile();
        if (temperature  == 0) return 0;
        //函数：y=0.4398x+0.2875
        temperature = temperature + 0.4398*distance/10+0.2875;
        return temperature;
    }

    //90614读取温度值并返回/sys/class/mlx906xx/device/temperature
    private static double get614TemperatureFile(){
        double temp = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/class/mlx906xx/device/temperature"));
            String temp_str = br.readLine();
            temp = Double.parseDouble(temp_str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    //拆分数组按效果图
    private static ArrayList<Float> splitArrayData(float[] data, PointAreaEntity areaEntity) {
        ArrayList<Float> mCenter;
        if (data == null) return null;
        ArrayList<Float> temp = new ArrayList<>();//32一组数据，临时对数据
        for (int i = 0; i < data.length; i++) {
            int row = i % 32;
            int col = i / 32;
            float d = data[i];
            temp.add(d);
            if (temp.size() == 32) {
                //                System.out.println(temp);
                temp.clear();
            }
            if (row < 6 && col < 6) {//left_top
                if (col == 0) {
                    areaEntity.left_top.add(d);
                    continue;
                } else if (col == 1) {
                    if (row < 5) {
                        areaEntity.left_top.add(d);
                        continue;
                    }
                } else if (col == 2) {
                    if (row < 4) {
                        areaEntity.left_top.add(d);
                        continue;
                    }
                } else if (col == 3) {
                    if (row < 3) {
                        areaEntity.left_top.add(d);
                        continue;
                    }
                } else if (col == 4) {
                    if (row < 2) {
                        areaEntity.left_top.add(d);
                        continue;
                    }
                } else {
                    if (row < 1) {
                        areaEntity.left_top.add(d);
                        continue;
                    }
                }
            }
            if (row < 6 && (col > (23 - 6))) {//left_bottom
                if (col == (23 - 5)) {
                    if (row < 1) {
                        areaEntity.left_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 4)) {
                    if (row < 2) {
                        areaEntity.left_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 3)) {
                    if (row < 3) {
                        areaEntity.left_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 2)) {
                    if (row < 4) {
                        areaEntity.left_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 1)) {
                    if (row < 5) {
                        areaEntity.left_bottom.add(d);
                        continue;
                    }
                } else {
                    areaEntity.left_bottom.add(d);
                    continue;
                }
            }

            if ((row > (31 - 6)) && col < 6) {//right_top
                if (col == 0) {
                    areaEntity.right_top.add(d);
                    continue;
                } else if (col == 1) {
                    if (row > (31 - 5)) {
                        areaEntity.right_top.add(d);
                        continue;
                    }
                } else if (col == 2) {
                    if (row > (31 - 4)) {
                        areaEntity.right_top.add(d);
                        continue;
                    }
                } else if (col == 3) {
                    if (row > (31 - 3)) {
                        areaEntity.right_top.add(d);
                        continue;
                    }
                } else if (col == 4) {
                    if (row > (31 - 2)) {
                        areaEntity.right_top.add(d);
                        continue;
                    }
                } else {
                    if (row > (31 - 1)) {
                        areaEntity.right_top.add(d);
                        continue;
                    }
                }
            }
            if ((row > (31 - 6)) && (col > (23 - 6))) {//right_bottom
                if (col == (23 - 5)) {
                    if (row > (31 - 1)) {
                        areaEntity.right_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 4)) {
                    if (row > (31 - 2)) {
                        areaEntity.right_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 3)) {
                    if (row > (31 - 3)) {
                        areaEntity.right_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 2)) {
                    if (row > (31 - 4)) {
                        areaEntity.right_bottom.add(d);
                        continue;
                    }
                } else if (col == (23 - 1)) {
                    if (row > (31 - 5)) {
                        areaEntity.right_bottom.add(d);
                        continue;
                    }
                } else {
                    areaEntity.right_bottom.add(d);
                    continue;
                }
            }

            if ((row > 7 && row < 24) && (col > 3 && col < 20)) {//center
                areaEntity.center.add(d);
                continue;
            }

            areaEntity.other.add(d);
        }

        mCenter = areaEntity.center;
        float sum = 0;
        float MAX =0;
        float MIN = 100;

        for (Float aFloat : areaEntity.center){
            sum += aFloat;
            if (aFloat > MAX){
                MAX = aFloat;
            }
            if(aFloat < MIN){
                MIN = aFloat;
            }
        }

        String mAvg = String.format("%.2f", sum / areaEntity.center.size());
        String mMax = String.format("%.2f℃", MAX);
        String mMi = String.format("%.2f℃", MIN);

        return mCenter;

    }

    //去掉边缘温度数据
    private static float dataArea(ArrayList<Float> list){
        if(list == null) return 0;
        ArrayList<Float> temp = new ArrayList<>();//32一组数据，临时对数据
        Float sum2 = Float.valueOf(0);
        for(int i = 0;i < list.size(); i++){
            float d = list.get(i);
            temp.add(d);
            //                data1 ++;
            //                Log.d("data","data="+data3);
            if (d > 38.0 || d < 30.0) list.remove(i);
        }

        for (int i = 0;i < list.size(); i++){
            sum2 = sum2 + list.get(i);
        }
        return sum2 / list.size();
    }

    public static String getCurrWeek(){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String today = "";
        switch (day){
            case 2:
                today = "星期一";
                break;
            case 3:
                today = "星期二";
                break;
            case 4:
                today = "星期三";
                break;
            case 5:
                today = "星期四";
                break;
            case 6:
                today = "星期五";
                break;
            case 7:
                today = "星期六";
                break;
            case 1:
                today = "星期七";
                break;
        }
        return today;
    }

}

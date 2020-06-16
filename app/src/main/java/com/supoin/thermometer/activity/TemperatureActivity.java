package com.supoin.thermometer.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.arcsoft.face.FaceInfo;
import com.supoin.thermometer.R;
import com.supoin.thermometer.appTools.FileUnit;
import com.supoin.thermometer.appTools.Tools;
import com.supoin.thermometer.service.DataUtils;
import com.supoin.thermometer.service.ResService.DataCallback;
import com.supoin.thermometer.widget.DrawInfo;
import com.supoin.thermometer.widget.FaceRectView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.supoin.thermometer.R.drawable.face_icon_red;
import static com.supoin.thermometer.R.drawable.ic_face_icon;
import static com.supoin.thermometer.service.ResService.getCurrDate;
import static com.supoin.thermometer.service.ResService.getCurrLocation;
import static com.supoin.thermometer.service.ResService.getCurrTime;
import static com.supoin.thermometer.service.ResService.setDataCallback;
import static com.supoin.thermometer.service.ResService.setRecognizeFace;
import static com.supoin.thermometer.service.ResService.speekSync;

public class TemperatureActivity extends BaseActivity {

    private FaceRectView mFaceRectView;
    private TextView tvNameStatus;
    private TextView tvMaskTips;
    private ImageView imageFaceIcon;
    private TextView tvMaskStatus;
    private TextView tvThirdTips;
    private StringBuilder stringBuilder;
    private String currLocation;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            currLocation = getCurrLocation();
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setView();
    }

    private void initFaceTemperature() {
        setRecognizeFace(true);
        setDataCallback(new DataCallback(){
            @Override
            public void onFaceResult(String name) {
            }

            @Override
            public void onDistanceResult(int distance) {
            }

            @Override
            public void onTemperatureResult(double temperature) {
            }

            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void onAllResult(String name, int distance, double temperature) {
                if (distance != 0) {
                    if (distance < 35){
                        speekSync("远一些");
                        tvMaskTips.setText("远一些");

                    }else if (distance > 60){
                        speekSync("请靠近");
                        tvMaskTips.setText("请靠近");
                    }else {
                        if (temperature < 37.3 && temperature > 35){
                            tvNameStatus.setTextColor(Color.BLUE);
                            tvMaskStatus.setText("请保持 正在测温");
                            Log.e("onAllResult","----"+"name:"+name+",distance:"+distance+",temperature:"+temperature);
                            tvMaskTips.setText(name + "体温"+String.format("%.2f℃", temperature));
                            speekSync(name + "体温"+String.format("%.2f℃", temperature));
                            tvNameStatus.setText(name + "体温"+String.format("%.2f℃", temperature));
                            if (!name.equals("")){
                                String currDate = getCurrDate();
                                String currTime = getCurrTime();
                                stringBuilder.append(currDate);
                                stringBuilder.append(currTime);
                                stringBuilder.append(","+name);
                                stringBuilder.append(","+String.format("%.2f℃", temperature));
                                stringBuilder.append(","+distance+"cm");
                                stringBuilder.append("\n");
                            }
                        }else if (temperature < 35){
                            tvNameStatus.setTextColor(Color.BLUE);
                            speekSync("将脸放入测温区");
                            tvMaskTips.setText("将脸放入测温区");
                            tvNameStatus.setText(name + "体温"+String.format("%.2f℃", temperature));
                        }else if (temperature > 37.3){
                            speekSync("温度偏高，调整再次测温");
                            tvMaskTips.setText("温度偏高，调整再次测温");
                            tvNameStatus.setTextColor(Color.RED);
                            tvNameStatus.setText(name + "体温"+String.format("%.2f℃", temperature));
                        }

                    }
                }
            }

            @Override
            public void onRecordFaceFeature(int status) {
                if (status != DataUtils.FACE_CENTER){
                    //需要正脸
                    speekSync(getString(R.string.face_center));
                    tvMaskTips.setText(R.string.face_center);
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFaceInfoList(List<FaceInfo> faceInfoList) {
                if (faceInfoList != null){
                    List<DrawInfo> drawInfoList = new ArrayList<>();
                    if (mFaceRectView != null && drawHelper != null && faceInfoList.size() >=1){
                        drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(0).getRect()), Color.WHITE));
                        drawHelper.draw(mFaceRectView, drawInfoList);
                        imageFaceIcon.setImageResource(face_icon_red);
                    }
                }else {
                    if (stringBuilder.length() > 0){
                        imageFaceIcon.setImageResource(ic_face_icon);
                        tvMaskStatus.setText(getCurrDate()+getCurrTime()+" 测温完成");
                        tvThirdTips.setText("位置:"+currLocation);
                    }else {
                        tvMaskStatus.setText("");
                        tvMaskTips.setText("");
                    }
                    drawHelper.draw(mFaceRectView, null);
                }

            }

            @Override
            public void onFace3DAngle(int Angle) {
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        initFaceTemperature();
        stringBuilder = new StringBuilder();
        new Thread(runnable).start();
    }

    //设置布局显示隐藏
    @SuppressLint("SetTextI18n")
    private void setView() {
        mFaceRectView = findViewById(R.id.face_rect_view);
        tvMaskTips = findViewById(R.id.tv_mask_tips);
        tvMaskTips.setTextColor(Color.GREEN);
        TextView tvTurnFace = findViewById(R.id.tv_turn_face);
        tvTurnFace.setVisibility(View.GONE);
        tvMaskStatus = findViewById(R.id.tv_mask_status);
        tvMaskStatus.setTextSize(22);
        tvMaskStatus.setTextColor(Color.BLUE);
        imageFaceIcon = findViewById(R.id.image_face_icon);
        tvNameStatus = findViewById(R.id.tv_name_status);
        tvNameStatus.setTextSize(28);
        tvNameStatus.setTextColor(Color.BLUE);
        tvMaskTips.setText("将脸放入测温区");
        speekSync("将脸放入测温区");
        Button btnClick = findViewById(R.id.btn_Click);
        btnClick.setVisibility(View.GONE);
        tvThirdTips = findViewById(R.id.tv_mask_over);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setRecognizeFace(false);
        String fileName =sdf.format(nowtime);
        Tools.WriteData2File(FileUnit.rootFolder,fileName,stringBuilder);
    }

    Date nowtime=new Date();
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 hh时mm分ss秒");
}

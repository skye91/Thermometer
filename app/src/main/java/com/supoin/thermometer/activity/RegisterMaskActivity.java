package com.supoin.thermometer.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.arcsoft.face.FaceInfo;
import com.supoin.thermometer.R;
import com.supoin.thermometer.service.DataUtils;
import com.supoin.thermometer.service.ResService;
import com.supoin.thermometer.widget.DrawInfo;
import com.supoin.thermometer.widget.FaceRectView;

import java.util.ArrayList;
import java.util.List;

import static com.supoin.thermometer.R.drawable.face_icon_red;
import static com.supoin.thermometer.R.drawable.ic_face_icon;
import static com.supoin.thermometer.service.DataUtils.NAME;
import static com.supoin.thermometer.service.ResService.saveFaceToFile;
import static com.supoin.thermometer.service.ResService.setDataCallback;
import static com.supoin.thermometer.service.ResService.setRecognizeFace;
import static com.supoin.thermometer.service.ResService.speek;
import static com.supoin.thermometer.service.ResService.speekSync;
import static com.supoin.thermometer.service.ResService.startRecordFaceFeature;
import static com.supoin.thermometer.service.ResService.stopSaveFace;

public class RegisterMaskActivity extends BaseActivity {

    private FaceRectView mFaceRectView;
    private TextView tvMaskTips;
    private TextView tvRegisterStatus;
    private TextView tvRegisterMaskStatus;
    private Button btnClick;
    private ImageView imageFaceIcon;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //戴口罩注册人脸信息
        initFaceTemperature();
    }

    private void initFaceTemperature() {
        final int[] count = {0};
        setRecognizeFace(true);
        startRecordFaceFeature(true);//戴口罩
        setDataCallback(new ResService.DataCallback(){

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
            }

            @Override
            public void onRecordFaceFeature(int status) {

                if (status == DataUtils.FACE_CENTER){
                    //需要正脸
                    speekSync(getString(R.string.face_center));
                    tvMaskTips.setText(R.string.face_center);
                }else if (status == DataUtils.FACE_UP){
                    count[0]++;
                    if (count[0] > 3){
                        tvRegisterStatus.setText("根据提示录入面部信息");
                        //需要朝上的脸
                        speekSync("请摘掉口罩将脸朝上录入面部信息");
                    }else {
                        //需要朝上的脸
                        speekSync(getString(R.string.face_up));
                        tvMaskTips.setText(R.string.face_up);
                    }


                }else if (status == DataUtils.FACE_RIGHT){
                    //需要朝右的脸
                    speekSync(getString(R.string.face_right));
                    tvMaskTips.setText(R.string.face_right);

                }else if (status == DataUtils.FACE_DOWN){
                    //需要朝下的脸
                    speekSync(getString(R.string.face_down));
                    tvMaskTips.setText(R.string.face_down);
                }else if (status == DataUtils.FACE_LEFT){
                    //需要朝左的脸
                    speekSync(getString(R.string.face_left));
                    tvMaskTips.setText(R.string.face_left);
                }else if(status == -1){
                    //戴口罩的人脸信息注册完成
                    speek("摘掉口罩,点击进入下一步");
                    tvMaskTips.setText("戴口罩面部信息注册成功");
                    tvMaskTips.setText("摘掉口罩进入下一步");
                    tvRegisterMaskStatus.setText("请摘掉口罩");
                    tvRegisterMaskStatus.setVisibility(View.VISIBLE);
                    tvRegisterStatus.setText("点击进入下一步");
                    tvRegisterStatus.setVisibility(View.VISIBLE);
                    btnClick.setVisibility(View.VISIBLE);
                    //注册完成写入文件
                    saveFaceToFile(NAME);
                } else if (status == -2){
                    //戴口罩的人脸信息注册完成
                    speek("注册完成");
                    tvMaskTips.setText("面部信息注册成功");
                    tvRegisterMaskStatus.setText("注册完成");
                    tvRegisterStatus.setText("点击完成进入测温");
                    tvRegisterStatus.setVisibility(View.VISIBLE);
                    btnClick.setVisibility(View.VISIBLE);
                    //注册完成写入文件
                    saveFaceToFile(NAME);
                    btnClick.setVisibility(View.VISIBLE);
                    btnClick.setText("完成");

                }
            }

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
                    drawHelper.draw(mFaceRectView, null);
                    imageFaceIcon.setImageResource(ic_face_icon);
                }
            }

            @Override
            public void onFace3DAngle(int Angle) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSaveFace();
    }

    private void initView() {
        btnClick = findViewById(R.id.btn_Click);
        mFaceRectView = findViewById(R.id.face_rect_view);
        tvMaskTips = findViewById(R.id.tv_mask_tips);
        tvMaskTips.setTextColor(Color.GREEN);
        speekSync("请戴口罩，根据提示录入面部信息");
        tvMaskTips.setText("请戴口罩，根据提示录入面部信息");
        tvRegisterStatus = findViewById(R.id.tv_name_status);
        tvRegisterMaskStatus = findViewById(R.id.tv_mask_status);
        btnClick.setVisibility(View.INVISIBLE);
        btnClick.setOnClickListener(this);
        imageFaceIcon = findViewById(R.id.image_face_icon);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        btnClick.setVisibility(View.INVISIBLE);
        if (btnClick.getText().toString().equals("下一步")){
            startRecordFaceFeature(false);//不戴口罩
        }else if (btnClick.getText().toString().equals("完成")){
            RegisterMaskActivity.this.finish();
        }
    }


}

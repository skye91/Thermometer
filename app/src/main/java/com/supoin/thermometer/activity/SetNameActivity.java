package com.supoin.thermometer.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.supoin.thermometer.R;

import static com.supoin.thermometer.service.DataUtils.NAME;
import static com.supoin.thermometer.service.ResService.deleteFaceFile;

public class SetNameActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etGetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_name);
        initView();
    }

    private void initView() {
        Button btnToRegister = findViewById(R.id.btn_to_register);
        btnToRegister.setOnClickListener(this);
        etGetName = findViewById(R.id.et_getName);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_to_register){
            EditText etName = findViewById(R.id.et_setName);
            String getName = etName.getText().toString().trim();
            NAME = getName;
            if (!etName.getText().toString().equals("")){
                Intent intentRegister = new Intent(this,RegisterMaskActivity.class);
                startActivity(intentRegister);
            }else {
                Toast.makeText(this, "注册，请先输入姓名！", Toast.LENGTH_SHORT).show();
            }

        }

    }

    //进入测温页面
    public void TemperatureMeasure(View view) {
        Intent intentTemperature = new Intent(this, TemperatureActivity.class);
        startActivity(intentTemperature);
    }

    //删除注册信息
    public void DeleteRegister(View view) {
        String name = etGetName.getText().toString().trim();
        if (!name.equals("")){
            deleteFaceFile(name);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
}

package com.supoin.thermometer.appTools;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;


/**
 * Created by Skye on 2017/8/11.
 * 这里实现一个写文件的方法
 */
public class Tools {

    /**
     * 追加数据到文件
     */
    public static void WriteData2File(String folderName, String fileName, StringBuilder data) {

        //创建文件对象，并指定文件存储路径
        File storageFolder = new File(Environment.getExternalStorageDirectory(), FileUnit.rootFolder);


        //判断文件是否存在
        if (!storageFolder.exists()) {
            //不存在则创建根目录
            storageFolder.mkdirs();
        }
        //创建二级文件对象
        File secondFolder = new File(storageFolder, folderName);
        if (!secondFolder.exists()) {
            //不存在则创建二级目录
            secondFolder.mkdirs();
        }

        try {

            //创建当前文件名
            File curFile = new File(secondFolder, fileName + ".csv");
            //写数据到当前文件
            FileWriter fileWriter = new FileWriter(curFile, true);
            //追加数据到文件
            fileWriter.append(data);

            //关闭写操作
            fileWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}



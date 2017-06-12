package com.example.tomcat.imageorientation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_CAMERA = 1;
    private final int CAMERA_PHOTO = 1;
    private final int ALBUM_PHOTO = 0;
    private final int IMG_WIDTH = 800;
    private final int IMG_HEIGHT = 600;


    ImageView   imgView;
    TextView    textView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate() ...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initControl();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                this.REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK)
        {
            Log.d(TAG, "resultCode: " + resultCode);
            return;
        }

        switch (requestCode)
        {
            case CAMERA_PHOTO:     //拍照取得圖片
                getCameraTake(data);
                break;

            case ALBUM_PHOTO:     // 相冊取得圖片
                getAlbumPhoto(data);
                break;

            default:
                Log.e(TAG, "Error!! NO method to get photo.");
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult(), requestCode: " + requestCode);

        switch (requestCode)
        {
            case REQUEST_CAMERA:
            {
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                    Toast.makeText(getApplicationContext(), "Permission granted",
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Permission denied",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initView()
    {
        Log.d(TAG, "initView()...");
        imgView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        //test  teset 2 code
    }

    private void initControl()
    {
        Log.d(TAG, "initControl()...");
    }

    public void imageOnClick(View view)
    {
        final CharSequence[] items = {"相 簿", "拍 照"};
        //final CharSequence[] items = {"Album", "Camera"};

        Log.d(TAG, "imageOnClick()...");
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("選擇照片").
                setItems(items, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // TODO Auto-generated method stub
                        //這裡item是根據選擇的方式，
                        // 在items數據裡面定義了兩種方式，拍照的下標為1所以就調用拍照方法
                        switch (which)
                    {
                        case ALBUM_PHOTO:
                            Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                            getImage.addCategory(Intent.CATEGORY_OPENABLE);
                            getImage.setType("image/*");
                            startActivityForResult(getImage, ALBUM_PHOTO);
                            break;

                        case CAMERA_PHOTO:
                            Intent getCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                            startActivityForResult(getCamera, CAMERA_PHOTO);
                            break;

                        default:
                            Log.e(TAG, "Error!! no get photo method.");
                            break;

                    }
                    }
                }).create();
        dlg.show();
    }

    private String currentDateTime()
    {
        Calendar    mCal = Calendar.getInstance();
        int[]       tmp = new int[7];
        Log.d(TAG, "currentDateTime()...");

        tmp[0] = mCal.get(Calendar.YEAR);
        tmp[1] = mCal.get(Calendar.MONTH)+1;
        tmp[2] = mCal.get(Calendar.DATE);
        tmp[3] = mCal.get(Calendar.HOUR_OF_DAY);
        tmp[4] = mCal.get(Calendar.MINUTE);
        tmp[5] = mCal.get(Calendar.SECOND);
        tmp[6] = mCal.get(Calendar.WEEK_OF_MONTH);

        return (String.format("%04d%02d%02d%02d%02d%02d",
                tmp[0], tmp[1], tmp[2], tmp[3], tmp[4], tmp[5]) );
    }

    private void getAlbumPhoto(Intent data)
    {
        Bitmap testBMP = null;
        String pngPathName = "";
        Uri selectedImage = data.getData();
        Log.d(TAG, "getPhotoAlbum(), album extras: " + selectedImage.toString());

        if (data!= null)
        {
            String selectFilePath = ImageFilePath.getPath(this.getBaseContext(), selectedImage);
            //Log.d(TAG, "selectFilePath: " + selectFilePath);

            //testBMP = ImgFunction.getOriententionBitmap(selectFilePath);
            int degree = ImgFunction.getOrientention(selectFilePath);
            testBMP = ImgFunction.resizeBitmap(selectedImage, this.getBaseContext());
            testBMP = ImgFunction.rotateImage(testBMP, degree);

            int fileSize = ImgFunction.sizeOf(testBMP);
            Log.w(TAG,  "selectFilePath: " + selectFilePath + ", degree: " + degree +
                        ", BMP size: " + fileSize + " bytes.");

            testBMP = resize(testBMP, IMG_WIDTH, IMG_WIDTH);
            pngPathName = saveImagePNGFile(testBMP);

            Log.d(TAG, "photo saved file: " + pngPathName +
                    ", size: " + ImgFunction.sizeOf(testBMP) + " bytes");
            textView.setText(pngPathName);
            imgView.setImageBitmap(testBMP);
        }
        else
        {
            Log.e(TAG, "Error!! data is NULL.");
        }

    }


    private void getAlbumPhoto_old(Intent data)
    {
        Bitmap testBMP = null;
        String pngPathName = "";
        Uri selectedImage = data.getData();
        Log.d(TAG, "getPhotoAlbum(), album extras: " + selectedImage.toString());

        if (data!= null)
        {
            String selectFilePath = ImageFilePath.getPath(this.getBaseContext(), selectedImage);
            Log.d(TAG, "selectFilePath: " + selectFilePath);
            testBMP = ImgFunction.getOriententionBitmap(selectFilePath);
            testBMP = resize(testBMP, IMG_WIDTH, IMG_WIDTH);

            pngPathName = saveImagePNGFile(testBMP);
            Log.d(TAG, "photo saved file: " + pngPathName +
                        ", size: " + testBMP.getByteCount() + " bytes");
            textView.setText(pngPathName);
            imgView.setImageBitmap(testBMP);
        }
        else
        {
            Log.e(TAG, "Error!! data is NULL.");
        }

    }

    private void getCameraTake(Intent data)
    {
        Log.d(TAG, "getPhotoCameraTake() ...");
        Bitmap myBitmap = null;
        String pngPathName = "";

        try
        {
            //Bundle extras = data.getExtras();
            //myBitmap = (Bitmap) extras.get("data");
            myBitmap = (Bitmap) data.getExtras().get("data");
            Uri bmpUri = data.getData();
            Log.d(TAG,  "camera extras: " + data.getExtras().toString() +
                        ", bmpUri: " + bmpUri +
                        ", raw data in memory size: " + myBitmap.getByteCount());
            myBitmap = ImgFunction.rotateImageIfRequired(myBitmap, this.getBaseContext(), bmpUri);
            myBitmap = resize(myBitmap, IMG_WIDTH, IMG_HEIGHT);

            pngPathName = saveImagePNGFile(myBitmap);
            textView.setText(pngPathName);
            Log.d(TAG,  "camera saved file: " + pngPathName +
                        ", size: " + myBitmap.getByteCount() + " bytes");

            imgView.setImageBitmap(myBitmap);
        }
        catch (Exception e)
        {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private Bitmap resize(Bitmap image, int maxWidth, int maxHeight)
    {
        if (maxHeight > 0 && maxWidth > 0)
        {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            }
            else
            {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        }
        else
        {
            return image;
        }
    }



    private String saveImagePNGFile(Bitmap image)
    {
        String fileName = currentDateTime() + ".png";
        String filePath = "/sdcard/mt24hr/" + fileName;
        Log.d(TAG, "getPhotoAlbum(), file Path: " + filePath);
        FileOutputStream baos = null;
        try
        {
            baos = new FileOutputStream(filePath);
            image.compress(Bitmap.CompressFormat.PNG, 100, baos);
            baos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return filePath;
    }

}




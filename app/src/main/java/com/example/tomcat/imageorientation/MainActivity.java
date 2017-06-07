package com.example.tomcat.imageorientation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import static com.example.tomcat.imageorientation.ImgFunction.*;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_CAMERA = 1;
    private final int CAMERA_PHOTO = 1;
    private final int ALBUM_PHOTO = 0;

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
                                Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                                startActivityForResult(getImageByCamera, CAMERA_PHOTO);
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
        final int SCAL_BASE = (1024 * 800);
        Uri selectedImage = data.getData();
        Log.d(TAG, "getPhotoAlbum(), album extras: " + selectedImage.toString());

        try
        {
            testBMP = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
            String[] filePathArrays = {"_data"};
            CursorLoader cursorLoader = new CursorLoader(this.getBaseContext(),
                    selectedImage, filePathArrays, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            if (cursor.getCount() != 0)
            {
                cursor.moveToFirst();
                String imgFilePath = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                textView.setTextColor(Color.BLACK);
                textView.setText(imgFilePath);
                Log.d(TAG, "getPhotoAlbum(), imgFilePath: " + imgFilePath);
                cursor.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        int degree = ImgFunction.getOrientation(this, selectedImage);
        Log.w(TAG, "uri get degree: " + degree);
        imgView.setImageBitmap(testBMP);
    }

    private void getCameraTake(Intent data)
    {
        Log.d(TAG, "getPhotoCameraTake() ...");
        Bitmap myBitmap = null;
        //imgFilePath += Utils.getPhotoFileName();
        String tmpFileName = "/sdcard/mt24hr/" + currentDateTime() + ".jpg";
        File tempFile = new File(tmpFileName);
        Log.d(TAG, "tmpFileName: " + tmpFileName);
        //String filePath = "";

        try
        {
            Bundle extras = data.getExtras();
            Log.d(TAG, "camera extras: " + extras.toString());

            myBitmap = (Bitmap) extras.get("data");
            tempFile.createNewFile();
            FileOutputStream baos = new FileOutputStream(tempFile);
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            baos.close();
            Log.d(TAG, "camera tmpFileName: " + tmpFileName +
                    ", bmp size: " + myBitmap.getByteCount());

            boolean rotationFlag = needRotate(this, tempFile.getAbsolutePath());
            Log.d(TAG, "rotationFlag: " + rotationFlag);

            ExifInterface exif = null;
            try {
                exif = new ExifInterface(tmpFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            Log.w(TAG, "222 orientation: " + orientation);
            myBitmap = rotateBitmap(myBitmap, orientation);
            imgView.setImageBitmap((myBitmap));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // TODO: handle exception
        }
        //myBitmap = ivPicture.setImageBitmap(myBitmap);
        //ivPicture.setImageBitmap(Utils.getRoundedShape(myBitmap));
    }

}


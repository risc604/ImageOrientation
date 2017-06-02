package com.example.tomcat.imageorientation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_CAMERA = 1;


    ImageView   imgView;
    TextView    textView;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate() ...");

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
            case 0:     // 相冊取得圖片
                getPhotoAlbum(data);
                break;

            case 1:     //拍照取得圖片
                getPhotoCameraTake(data);
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
        imgView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
        Log.d(TAG, "initView()...");
    }

    private void initControl()
    {
        Log.d(TAG, "initControl()...");
    }

    public void imageOnClick(View view)
    {
        final CharSequence[] items = {"相册", "拍照"};
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
                        if (which == 1)
                        {
                            Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                            startActivityForResult(getImageByCamera, 1);
                        }
                        else
                        {
                            Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                            getImage.addCategory(Intent.CATEGORY_OPENABLE);
                            getImage.setType("image/jpeg");
                            startActivityForResult(getImage, 0);
                        }
                    }
                }).create();
        dlg.show();
    }

    private void getPhotoAlbum(Intent data)
    {
        byte[] mContent;
        final int SCAL_BASE = (1024 * 800);
        Uri selectedImage = data.getData();
        Log.d(TAG, "getPhotoAlbum(), album extras: " + selectedImage.toString());

        //方式一
        /*try
        {
             //獲得圖片的uri
            Uri orginalUri = data.getData();
              //將圖片内容解析成字節數組
            mContent = readStream(contentResolver.openInputStream(Uri.parse(orginalUri.toString())));
             //將字節數組轉換為ImageView可調用的Bitmap對象
            myBitmap  =getPicFromBytes(mContent,null);
              ////把得到的圖片绑定在控件上顯示
            imageView.setImageBitmap(myBitmap);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // TODO: handle exception
        }*/

        //方式二
        try
        {
            mContent = readStream(getApplication().getContentResolver()
                    .openInputStream(Uri.parse(selectedImage.toString())));
            Log.d(TAG, "album mContent: " + mContent.toString() + ", file length: " + mContent.length);

            //Bitmap tmpBMP = BitmapFactory.decodeFile(picturePath);
            int scalMultip = mContent.length / SCAL_BASE;
            Bitmap tmpBMP = getPicFromBytes(mContent, null, scalMultip);
            Log.d(TAG,  "tmpBPM byte counts: " + tmpBMP.getByteCount() +
                    ", scalMultip: " + scalMultip);

            //Log.d(TAG, "resizeBitmap byte counts: " + tmpBMP.getByteCount());
            String fileName = Environment.getExternalStorageState() + "/mt24hr/" + Calendar.getInstance();
            FileOutputStream baos = new FileOutputStream(fileName);
            tmpBMP.compress(Bitmap.CompressFormat.PNG, 100, baos);
            baos.close();
            Log.d(TAG, "getPhotoAlbum(), file name: " + fileName);

            modifyOrientation(tmpBMP, fileName);
            imgView.setImageBitmap(tmpBMP);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public byte[] readStream(InputStream inStream) throws Exception
    {
        final int BUFFER_SIZE = 1024;

        byte[] buffer = new byte[BUFFER_SIZE];
        int len = -1;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while ((len = inStream.read(buffer)) != -1)
        {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

    public Bitmap getPicFromBytes(byte[] bytes, BitmapFactory.Options opts, int scalSize)
    {
        Bitmap tmpBMP;

        opts = new BitmapFactory.Options();
        opts.inSampleSize = scalSize;
        if (bytes != null)
        {
            if (opts != null)
            {
                tmpBMP = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            }
            else
            {
                tmpBMP = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            }
            //return getBMPRotation(tmpBMP);
            return tmpBMP;
        }
        return null;
    }

    //public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException
    private Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException
    {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(   ExifInterface.TAG_ORIENTATION,
                                                ExifInterface.ORIENTATION_NORMAL);

        Log.w(TAG, "modifyOrientation(), orientation: " + orientation);
        switch (orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    //public static Bitmap rotate(Bitmap bitmap, float degrees)
    public static Bitmap rotate(Bitmap bitmap, float degrees)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        Log.w(TAG, "rotate(), degree: " + degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        Log.w(TAG, "flip(),  matrix: " + matrix);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    private void getPhotoCameraTake(Intent data)
    {
        Log.d(TAG, "getPhotoCameraTake() ...");
        Bitmap myBitmap = null;
        //imgFilePath += Utils.getPhotoFileName();
        String tmpFileName = Calendar.getInstance().toString();
        File tempFile = new File(tmpFileName);
        //String filePath = "";

        try
        {
            Bundle extras = data.getExtras();
            Log.d(TAG, "camera extras: " + extras.toString());

            myBitmap = (Bitmap) extras.get("data");

            tempFile.createNewFile();
            FileOutputStream baos = new FileOutputStream(tempFile);
            myBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            baos.close();
            Log.d(TAG, "camera tmpFileName: " + tmpFileName +
                    ", bmp size: " + myBitmap.getByteCount());

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


package com.example.tomcat.imageorientation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                        ///if (which == 1)
                        ///{
                        ///    Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                        ///    startActivityForResult(getImageByCamera, ALBUM_PHOTO);
                        ///}
                        ///else
                        ///{
                        ///    Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                        ///    getImage.addCategory(Intent.CATEGORY_OPENABLE);
                        ///    getImage.setType("image/*");
                        ///    startActivityForResult(getImage, CAMERA_PHOTO);
                        ///}
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
        byte[] mContent;
        //String imgFilePath = "";
        Bitmap testBMP = null;
        final int SCAL_BASE = (1024 * 800);
        Uri selectedImage = data.getData();
        Log.d(TAG, "getPhotoAlbum(), album extras: " + selectedImage.toString() +
                ", selectedImage path: " + selectedImage.getPath());

        if (data!= null)
        {
            String selectFilePath = ImageFilePath.getPath(this.getBaseContext(), selectedImage);
            Log.d(TAG, "selectFilePath: " + selectFilePath);
            //textView.setTextSize(16);
            textView.setText(selectFilePath);
            testBMP = ImgFunction.getOriententionBitmap(selectFilePath);
            testBMP = getResizedBitmap(testBMP, 800, 600);

            String fileName = currentDateTime() + ".png";
            String filePath = "/sdcard/mt24hr/" + fileName;
            Log.d(TAG, "getPhotoAlbum(), file Path: " + filePath);
            FileOutputStream baos = null;
            try
            {
                baos = new FileOutputStream(filePath);
                testBMP.compress(Bitmap.CompressFormat.PNG, 100, baos);
                baos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


        }
        imgView.setImageBitmap(testBMP);

        //ContentResolver resolver = getContentResolver();

        ///try
        ///{
        ///    testBMP = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
        ///    String[] filePathArrays = {"_data"};
        ///    //Cursor  cursor = resolver.query(selectedImage, filePathArrays, null, null, null);
        ///    CursorLoader cursorLoader = new CursorLoader(this.getBaseContext(),
        ///            selectedImage, filePathArrays, null, null, null);
        ///    Cursor cursor = cursorLoader.loadInBackground();
        ///    if (cursor.getCount() != 0) {
        ///        cursor.moveToFirst();
        ///
        ///    //int arrayindex = cursor.getColumnIndexOrThrow(filePathArrays[0]);
        ///    String imgFilePath = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
        ///        textView.setTextColor(Color.BLACK);
        ///        textView.setText(imgFilePath);
        ///    Log.d(TAG, "getPhotoAlbum(), imgFilePath: " + imgFilePath);
        ///    cursor.close();
        ///    }
        ///}
        ///catch (IOException e)
        ///{
        ///    e.printStackTrace();
        ///}


        ///try
        ///{
        ///    testBMP = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
        ///    String[] filePathArrays = {MediaStore.Images.Media.DATA};
        ///    //Cursor  cursor = resolver.query(selectedImage, filePathArrays, null, null, null);
        ///    Cursor  cursor = getContentResolver().query(selectedImage, filePathArrays, null, null, null);
        ///    cursor.moveToFirst();
        ///
        ///    int arrayindex = cursor.getColumnIndexOrThrow(filePathArrays[0]);
        ///    imgFilePath = cursor.getString(arrayindex);
        ///    textView.setText(imgFilePath);
        ///    Log.d(TAG, "getPhotoAlbum(), arrayindex: " + arrayindex +
        ///                ", imgFilePath: " + imgFilePath);
        ///    cursor.close();
        ///}
        ///catch (IOException e)
        ///{
        ///    e.printStackTrace();
        ///}
        ////方式一
        ///*try
        //{
        //     //獲得圖片的uri
        //    Uri orginalUri = data.getData();
        //      //將圖片内容解析成字節數組
        //    mContent = readStream(contentResolver.openInputStream(Uri.parse(orginalUri.toString())));
        //     //將字節數組轉換為ImageView可調用的Bitmap對象
        //    myBitmap  =getPicFromBytes(mContent,null);
        //      ////把得到的圖片绑定在控件上顯示
        //    imageView.setImageBitmap(myBitmap);
        //}
        //catch (Exception e)
        //{
        //    e.printStackTrace();
        //    // TODO: handle exception
        //}*/


        //int degree = ImgFunction.getOrientation(this, selectedImage);
        //Log.w(TAG, "uri get degree: " + degree);
        //imgView.setImageBitmap(testBMP);

        /////方式二
        ///try
        ///{
        ///    mContent = readStream(getApplication().getContentResolver()
        ///            .openInputStream(Uri.parse(selectedImage.toString())));
        ///    Log.d(TAG, "album mContent: " + mContent.toString() + ", file length: " + mContent.length);
        ///
        ///    //Bitmap tmpBMP = BitmapFactory.decodeFile(picturePath);
        ///    int scalMultip = mContent.length / SCAL_BASE;
        ///    Bitmap tmpBMP = getPicFromBytes(mContent, null, scalMultip);
        ///    Log.d(TAG,  "tmpBPM byte counts: " + tmpBMP.getByteCount() +
        ///            ", scalMultip: " + scalMultip);
        ///
        ///    //Log.d(TAG, "resizeBitmap byte counts: " + tmpBMP.getByteCount());
        ///    String fileName = currentDateTime() + ".jpg";
        ///    Log.d(TAG, "getPhotoAlbum(), fileName: " + fileName);
        ///    String filePath = "/sdcard/mt24hr/" + fileName;
        ///    Log.d(TAG, "getPhotoAlbum(), file Path: " + filePath);
        ///    FileOutputStream baos = new FileOutputStream(filePath);
        ///    tmpBMP.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        ///    baos.close();
        ///
        ///    boolean rotationFlag = needRotate(this, filePath);
        ///
        ///    File imgFile = new File(filePath);
        ///    Log.w(TAG, "imgFile size: " + imgFile.length() + " bytes, rotationFlag: " + rotationFlag);
        ///
        ///    //ExifInterface exif = null;
        ///    //try {
        ///    //    exif = new ExifInterface(filePath);
        ///    //} catch (IOException e) {
        ///    //    e.printStackTrace();
        ///    //}
        ///    //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ///    //        ExifInterface.ORIENTATION_UNDEFINED);
        ///
        ///    //Log.w(TAG, "111 orientation: " + orientation);
        ///    //tmpBMP = rotateBitmap(tmpBMP, orientation);
        ///
        ///    //tmpBMP = getOriententionBitmap(filePath);
        ///
        ///    //if (imgFile!=null) {
        ///    //    modifyOrientation(tmpBMP, imgFile.getAbsolutePath());
        ///    //}
        ///    //else
        ///    //{
        ///    //    Log.e(TAG, "Error!! NOT found image file in path: " + filePath);
        ///    //}
        ///    imgView.setImageBitmap(tmpBMP);
        ///}
        ///catch (Exception e)
        ///{
        ///    e.printStackTrace();
        ///}
    }

    public byte[] readStream(InputStream inStream) throws Exception
    {
        final int BUFFER_SIZE = 1024;
        Log.d(TAG, "readStream()...");

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
        Log.d(TAG, "getPicFromBytes()...");

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

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}


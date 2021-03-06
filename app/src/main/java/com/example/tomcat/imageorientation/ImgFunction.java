package com.example.tomcat.imageorientation;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by tomcat on 2017/6/3.
 */

public class ImgFunction
{
    private final static String TAG = ImgFunction.class.getSimpleName();

    public ImgFunction(){}

    public static Bitmap getOriententionBitmap(String filePath)
    {
        Bitmap myBitmap = null;
        try
        {
            File f = new File(filePath);
            ExifInterface exif = new ExifInterface(f.getPath());
            int orientation = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.d(TAG, "orientation: " + orientation);
            int angle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
            {
                angle = 90;
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            {
                angle = 180;
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
            {
                angle = 270;
            }

            Matrix mat = new Matrix();
            mat.postRotate(angle);

            Bitmap bmp1 = BitmapFactory.decodeStream(new FileInputStream(f), null, null);
            myBitmap = Bitmap.createBitmap( bmp1, 0, 0, bmp1.getWidth(),
                    bmp1.getHeight(), mat, true);
            Log.d(TAG, "myBitmap: " + myBitmap.getByteCount() + " bytes.");
        }
        catch (IOException e)
        {
            Log.w("TAG", "-- Error in setting image");
        }
        catch(OutOfMemoryError oom)
        {
            Log.w("TAG", "-- OOM Error in setting image");
        }
        return myBitmap;
    }

    public static Bitmap rotateImageIfRequired(Bitmap img, Context context, Uri selectedImage) throws IOException
    {
        if (selectedImage.getScheme().equals("content"))
        {
            String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor c = context.getContentResolver().query(selectedImage, projection, null, null, null);
            if (c.moveToFirst())
            {
                final int rotation = c.getInt(0);
                Log.w(TAG, "rotation: " + rotation);

                c.close();
                return rotateImage(img, rotation);
            }
            return img;
        }
        else
        {
            ExifInterface ei = new ExifInterface(selectedImage.getPath());
            int orientation = ei.getAttributeInt(   ExifInterface.TAG_ORIENTATION,
                                                    ExifInterface.ORIENTATION_NORMAL);
            //Timber.d("orientation: %s", orientation);
            Log.d(TAG, "orientation: " + orientation);

            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
    }

    public static Bitmap rotateImage(Bitmap img, int degree)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        return rotatedImg;
    }

    public static int getOrientention(String filePath)
    {
        File f = new File(filePath);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(f.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        int orientation = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        Log.d(TAG, "orientation: " + orientation);

        int angle = 0;

        switch (orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                angle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                angle = 270;
                break;

            default:
                Log.e(TAG, "Error !! orientation: " + orientation);
                break;
        }

        return angle;
    }

    public static Bitmap resizeBitmap(Uri uri, Context context)
    {
        ContentResolver cr = context.getContentResolver();
        Bitmap bitmap = null;
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true; //只取bitmap的長寬，不取得整張bitmap

        try
        {
            bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri), null, option);
        }
        catch (FileNotFoundException e1)
        {
            e1.printStackTrace();
        }

        final int newSize = 300;
        int width = option.outWidth;
        int height = option.outHeight;
        int scale = 1;
        while (true)
        {
            if ((width / 4 < newSize) || (height / 4 < newSize))
                break;
            width /= 4;
            height /= 4;
            scale++;
        }

        option = new BitmapFactory.Options();
        option.inSampleSize = scale;
        //經過resize後才把bitmap整張圖取出來
        try
        {
            bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri), null, option);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        return bitmap;

    }

    public static int sizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        } else if (Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT){
            return data.getByteCount();
        } else{
            return data.getAllocationByteCount();
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        Log.i(TAG, "height: " + height);
        Log.i(TAG, "width: " + width);
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth)
            {
                inSampleSize *= 2;
            }

            long totalPixels = width * height / inSampleSize;

            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap)
            {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromFile(String imagePath, int requestWidth, int requestHeight)
    {
        if (!TextUtils.isEmpty(imagePath))
        {
            Log.i(TAG, "requestWidth: " + requestWidth);
            Log.i(TAG, "requestHeight: " + requestHeight);
            if (requestWidth <= 0 || requestHeight <= 0) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                return bitmap;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;//不加载图片到内存，仅获得图片宽高
            BitmapFactory.decodeFile(imagePath, options);
            Log.i(TAG, "original height: " + options.outHeight);
            Log.i(TAG, "original width: " + options.outWidth);
            if (options.outHeight == -1 || options.outWidth == -1)
            {
                try
                {
                    ExifInterface exifInterface = new ExifInterface(imagePath);
                    int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的高度
                    int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的宽度
                    Log.i(TAG, "exif height: " + height);
                    Log.i(TAG, "exif width: " + width);
                    options.outWidth = width;
                    options.outHeight = height;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            options.inSampleSize = calculateInSampleSize(options, requestWidth, requestHeight); //计算获取新的采样率
            Log.i(TAG, "inSampleSize: " + options.inSampleSize);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(imagePath, options);

        }
        else
        {
            return null;
        }
    }

    //public static boolean needRotate(Context coNtext, String fileName)
    //{
    //    try
    //    {
    //        ExifInterface exif = new ExifInterface(fileName);
    //        int orientation = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION,
    //                                                ExifInterface.ORIENTATION_UNDEFINED);
    //        Log.d(TAG, "orientation is " + orientation);
    //        switch (orientation)
    //        {
    //            case ExifInterface.ORIENTATION_ROTATE_90:
    //                return true;
    //            case ExifInterface.ORIENTATION_ROTATE_270:
    //                return true;
    //            case ExifInterface.ORIENTATION_ROTATE_180:
    //                return true;
    //        }
    //        return false;
    //    }
    //    catch (IOException e)
    //    {
    //        e.printStackTrace();
    //        return false;
    //    }
    //}

    //public static Bitmap rotateBitmap(Bitmap bitmap, int orientation)
    //{
    //    Matrix matrix = new Matrix();
    //    switch (orientation)
    //    {
    //        case ExifInterface.ORIENTATION_NORMAL:
    //            return bitmap;
    //        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
    //            matrix.setScale(-1, 1);
    //            break;
    //        case ExifInterface.ORIENTATION_ROTATE_180:
    //            matrix.setRotate(180);
    //            break;
    //        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
    //            matrix.setRotate(180);
    //            matrix.postScale(-1, 1);
    //            break;
    //        case ExifInterface.ORIENTATION_TRANSPOSE:
    //            matrix.setRotate(90);
    //            matrix.postScale(-1, 1);
    //            break;
    //        case ExifInterface.ORIENTATION_UNDEFINED:
    //        case ExifInterface.ORIENTATION_ROTATE_90:
    //            matrix.setRotate(90);
    //            break;
    //        case ExifInterface.ORIENTATION_TRANSVERSE:
    //            matrix.setRotate(-90);
    //            matrix.postScale(-1, 1);
    //            break;
    //        case ExifInterface.ORIENTATION_ROTATE_270:
    //            matrix.setRotate(-90);
    //            break;
    //        //case ExifInterface.ORIENTATION_UNDEFINED:
    //        //    matrix.setRotate(-90);
    //        //    matrix.postScale(-1, 1);
    //        //    break;
    //        default:
    //            return bitmap;
    //    }
    //    try
    //    {
    //        Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0,
    //                            bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    //        bitmap.recycle();
    //        return bmRotated;
    //    }
    //    catch (OutOfMemoryError e)
    //    {
    //        e.printStackTrace();
    //        return null;
    //    }
    //}

    //public static int getOrientation(Context context, Uri photoUri)
    //{
    //    Cursor cursor = context.getContentResolver().query(photoUri,
    //            new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);
    //
    //    Log.w(TAG, "getOrientation(), cursor.getCount(): " + cursor.getCount());
    //    //textView.setText("Count: " + cursor.getCount());
    //    if (cursor == null || cursor.getCount() != 1)
    //    {
    //        return 90;  //Assuming it was taken portrait
    //    }
    //
    //    cursor.moveToFirst();
    //    return cursor.getInt(0);
    //}

    //public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException
    //{
    //    Log.w(TAG, "modifyOrientation(), image_absolute_path: " + image_absolute_path);
    //    ExifInterface ei = new ExifInterface(image_absolute_path);
    //    //int orientation = ei.getAttributeInt(   ExifInterface.TAG_ORIENTATION,
    //    //                                        ExifInterface.ORIENTATION_NORMAL);
    //
    //    String orientString = ei.getAttribute(ExifInterface.TAG_ORIENTATION);
    //    int orientation = orientString != null ?
    //                      Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
    //
    //    Log.w(TAG, "modifyOrientation(), orientation: " + orientation);
    //    //textView.setText(textView.getText() + ", degree: " + orientation);
    //    switch (orientation)
    //    {
    //        case ExifInterface.ORIENTATION_ROTATE_90:
    //            return rotate(bitmap, 90);
    //
    //        case ExifInterface.ORIENTATION_ROTATE_180:
    //            return rotate(bitmap, 180);
    //
    //        case ExifInterface.ORIENTATION_ROTATE_270:
    //            return rotate(bitmap, 270);
    //
    //        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
    //            return flip(bitmap, true, false);
    //
    //        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
    //            return flip(bitmap, false, true);
    //
    //        case ExifInterface.ORIENTATION_UNDEFINED:
    //            return rotate(bitmap, -90);
    //
    //        default:
    //            return bitmap;
    //    }
    //}

    //public static Bitmap rotate(Bitmap bitmap, float degrees)
    //{
    //    Matrix matrix = new Matrix();
    //    matrix.postRotate(degrees);
    //
    //    Log.w(TAG, "rotate(), degree: " + degrees);
    //    return Bitmap.createBitmap(   bitmap, 0, 0,
    //                                  bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    //}

    //public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical)
    //{
    //    Matrix matrix = new Matrix();
    //    matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
    //    Log.w(TAG, "flip(),  matrix: " + matrix);
    //    return Bitmap.createBitmap(   bitmap, 0, 0,
    //                                  bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    //}

    ////---
    //public static byte[] rotateImageIfRequired(Context context, Uri uri, byte[] fileBytes)
    //{
    //    byte[] data = null;
    //    Bitmap bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.length);
    //    ByteArrayOutputStream outputStream = null;
    //
    //    try
    //    {
    //        //bitmap = ImageResizer.rotateImageIfRequired(bitmap, context, uri);
    //        bitmap = rotateImageIfRequired(bitmap, context, uri);
    //        outputStream = new ByteArrayOutputStream();
    //        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    //        data = outputStream.toByteArray();
    //    }
    //    catch (IOException e)
    //    {
    //        //Timber.e(e.getMessage());
    //        e.printStackTrace();
    //    }
    //    finally
    //    {
    //        try
    //        {
    //            if (outputStream != null)
    //            {
    //                outputStream.close();
    //            }
    //        }
    //        catch (IOException e)
    //        {
    //            // Intentionally blank
    //            e.printStackTrace();
    //        }
    //    }
    //
    //    return data;
    //}

}



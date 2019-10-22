package com.owson.shareimage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int SHARE_STORAGE_PERMS_REQUEST_CODE = 900;
    private final int SAVE_STORAGE_PERMS_REQUEST_CODE = 901;
    private final int RESULT_LOAD_IMG_REQUEST_CODE = 778;

    private final String[] perms = { android.Manifest.permission.WRITE_EXTERNAL_STORAGE,  android.Manifest.permission.READ_EXTERNAL_STORAGE};


    private static final String IMAGE_URL = "https://ichef.bbci.co.uk/images/ic/720x405/p0517py6.jpg";

    @BindView(R.id.imageView)
    SimpleDraweeView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        imageView.setImageURI(Uri.parse(IMAGE_URL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RESULT_LOAD_IMG_REQUEST_CODE  && resultCode == RESULT_OK) {
            List<Image> images =  ImagePicker.getImages(data);

            if(images.size() > 0) {
                imageView.setImageURI(Uri.fromFile(new File(images.get(0).getPath())));
            }
        }
    }

    @OnClick(R.id.shareButton)
    void onShareTouched() {
        boolean has_perms = EasyPermissions.hasPermissions(MainActivity.this, perms);
        if (has_perms)
            shareImageFromUrl(IMAGE_URL);
        else {
            EasyPermissions.requestPermissions(
                    MainActivity.this,
                    getString(R.string.rationale_storage),
                    SHARE_STORAGE_PERMS_REQUEST_CODE,
                    perms);
        }
    }

    @OnClick(R.id.saveButton)
    void onSaveTouched() {
        String[] perms = { android.Manifest.permission.WRITE_EXTERNAL_STORAGE,  android.Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean has_perms = EasyPermissions.hasPermissions(MainActivity.this, perms);
        if (has_perms)
            saveImage();
        else {
            EasyPermissions.requestPermissions(
                    MainActivity.this,
                    getString(R.string.rationale_storage),
                    SAVE_STORAGE_PERMS_REQUEST_CODE,
                    perms);
        }
    }

    @OnClick(R.id.loadImageButton)
    void onLoadImageButtonClick() {
        loadPhotoFromGallery();
    }

    @AfterPermissionGranted(SHARE_STORAGE_PERMS_REQUEST_CODE)
    private void shareImageFromUrl(String url) {
        Bitmap bmp = getBitmapFromUrl(url);
        if(bmp == null) {
            //Show no bitmap message
            return;
        }
        shareImageFromBitmap(bmp);
    }

    private void shareImageFromBitmap(Bitmap bmp) {
        Uri uri = getUriImageFromBitmap(bmp, MainActivity.this);
        if(uri == null) {
            //Show no URI message
            return;
        }

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);

        shareIntent.putExtra(Intent.EXTRA_TEXT, IMAGE_URL);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/png");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share image using"));
    }

    @AfterPermissionGranted(SAVE_STORAGE_PERMS_REQUEST_CODE)
    private void saveImage() {
        Bitmap bmp = getBitmapFromUrl(IMAGE_URL);
        if(bmp == null) {
            //Show no bitmap message
            return;
        }

        String folder_name = "Demo";
        String pathname = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getPath() + "/"+folder_name+"/";
        File storageDir = new File(pathname);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;

        File imageFile = new File(storageDir.getAbsolutePath() + "/" + imageFileName + ".png");
        Uri photoURI;

//        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N))
        photoURI = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", imageFile);
//        else
//            photoURI = Uri.fromFile(imageFile);
//

        if(photoURI == null) {
            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.image_could_not_be_created), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();

            //Refreshing image on gallery
            MediaScannerConnection.scanFile(MainActivity.this, new String[] { imageFile.getPath() }, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri)
                {

                }
            });

            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.image_saved, folder_name), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "SavePublicImageFromPost: ", e);
        } catch (IOException e) {
            Log.e(TAG, "SavePublicImageFromPost: ", e);
        }

    }

    private Bitmap getBitmapFromUrl(String url) {
        Uri uri = Uri.parse(url);
        ImageRequest downloadRequest = ImageRequest.fromUri(uri);

        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(downloadRequest, MainActivity.this);
        if (ImagePipelineFactory.getInstance().getMainFileCache().hasKey(cacheKey)) {
            BinaryResource resource = ImagePipelineFactory.getInstance().getMainFileCache().getResource(cacheKey);

            byte[] data = null;
            try {
                data = resource.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        return null;
    }

    private Uri getUriImageFromBitmap(Bitmap bmp, Context context) {
        if(bmp == null)
            return null;

        Uri bmpUri = null;

        try {

            File file =  new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            bmpUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
//            else
//                bmpUri = Uri.fromFile(file);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpUri;
    }

    private void loadPhotoFromGallery(){
        ImagePicker.create(this)
                .folderMode(false)
                .single()
                .showCamera(false)
                .enableLog(false)
                .start(RESULT_LOAD_IMG_REQUEST_CODE);
    }
}

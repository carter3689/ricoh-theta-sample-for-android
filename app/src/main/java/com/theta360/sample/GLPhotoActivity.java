package com.theta360.sample;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import com.theta360.lib.PtpipInitiator;
import com.theta360.lib.ptpip.entity.ObjectInfo;
import com.theta360.lib.ptpip.entity.PtpObject;
import com.theta360.lib.ptpip.eventlistener.DataReceivedListener;
import com.theta360.lib.rexif.ExifReader;
import com.theta360.lib.rexif.entity.OmniInfo;
import com.theta360.sample.glview.GLPhotoView;
import com.theta360.sample.model.Photo;
import com.theta360.sample.model.RotateInertia;
import com.theta360.sample.view.ConfigurationDialog;
import com.theta360.sample.view.LogView;

import java.io.*;


/**
 * Activity that displays photo object as a sphere
 */
public class GLPhotoActivity extends Activity implements ConfigurationDialog.DialogBtnListener {

    private static final String CAMERA_IP_ADDRESS = "CAMERA_IP_ADDRESS";
    private static final String OBJECT_HANDLE = "OBJECT_HANDLE";
    private static final int INVALID_OBJECT_HANDLE = 0x00000000;
    private static final String THUMBNAIL = "THUMBNAIL";

    private GLPhotoView mGLPhotoView;

    private Photo mTexture = null;

    private RotateInertia mRotateInertia = RotateInertia.INERTIA_0;


    /**
     * onCreate Method
     * @param savedInstanceState onCreate Status value
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glphoto);

        Intent intent = getIntent();
        String cameraIpAddress = intent.getStringExtra(CAMERA_IP_ADDRESS);
        int objectHandle = intent.getIntExtra(OBJECT_HANDLE, INVALID_OBJECT_HANDLE);
        byte[] byteThumbnail = intent.getByteArrayExtra(THUMBNAIL);

        ByteArrayInputStream inputStreamThumbnail = new ByteArrayInputStream(byteThumbnail);
        Drawable thumbnail = BitmapDrawable.createFromStream(inputStreamThumbnail, null);

        Photo _thumbnail = new Photo(((BitmapDrawable)thumbnail).getBitmap());

        mGLPhotoView = (GLPhotoView) findViewById(R.id.photo_image);
        mGLPhotoView.setTexture(_thumbnail);
        mGLPhotoView.setmRotateInertia(mRotateInertia);

        new LoadPhotoTask(cameraIpAddress, objectHandle, thumbnail).execute();

        return;
    }


    /**
     * onCreateOptionsMenu method
     * @param menu Menu initialization object
     * @return Menu display feasibility status value
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.configuration, menu);

        return true;
    }


    /**
     * onOptionsItemSelected Method
     * @param item Process menu
     * @return Menu process continuation feasibility value
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.configuration:
                FragmentManager mgr = getFragmentManager();
                ConfigurationDialog.show(mgr, mRotateInertia);
                break;
            default:
                break;
        }
        return true;
    }


    /**
     * onResume Method
     */
    @Override
    protected void onResume() {
        super.onResume();
        mGLPhotoView.onResume();

        if (null != mTexture) {
            String tmpFileForReadingExif = "tmpFileForReadingExif";
            InputStream is;
            try {
                is = openFileInput(tmpFileForReadingExif);

                if (null != is) {
                    Bitmap __bitmap = BitmapFactory.decodeStream(is);
                    Bitmap _bitmap = Bitmap.createBitmap(__bitmap, 0, 0, __bitmap.getWidth(), __bitmap.getHeight(), null, true);

                    mTexture.updatePhoto(_bitmap);
                    if (null != mGLPhotoView) {
                        mGLPhotoView.setTexture(mTexture);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return;
    }

    /**
     * onPause Method
     */
    @Override
    protected void onPause() {
        this.mGLPhotoView.onPause();
        super.onPause();

        return;
    }


    /**
     *
     * @param inertia
     */
    @Override
    public void onDialogCommitClick(RotateInertia inertia) {
        mRotateInertia = inertia;
        if (null != mGLPhotoView) {
            mGLPhotoView.setmRotateInertia(mRotateInertia);
        }
        return;
    }


    private class LoadPhotoTask extends AsyncTask<Void, Object, PtpObject> {

        private LogView logViewer;
        private ProgressBar progressBar;
        private String cameraIpAddress;
        private int objectHandle;

        public LoadPhotoTask(String cameraIpAddress, int objectHandle, Drawable thumbnail) {
            this.logViewer = (LogView) findViewById(R.id.photo_info);
            this.progressBar = (ProgressBar) findViewById(R.id.loading_photo_progress_bar);
            this.cameraIpAddress = cameraIpAddress;
            this.objectHandle = objectHandle;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            return;
        }

        @Override
        protected PtpObject doInBackground(Void... params) {
            try {

                PtpipInitiator camera = new PtpipInitiator(cameraIpAddress);

                ObjectInfo objectInfo = camera.getObjectInfo(objectHandle);
                publishProgress("<ObjectInfo: object_handle=" + objectHandle + ", protection_status=" + objectInfo.getProtectionStatus() + ", filename=" + objectInfo.getFilename() + ", capture_date=" + objectInfo.getCaptureDate()
                        + ", object_compressed_size=" + objectInfo.getObjectCompressedSize() + ", thumb_compresses_size=" + objectInfo.getThumbCompressedSize() + ", thumb_pix_width=" + objectInfo.getThumbPixWidth() + ", thumb_pix_height="
                        + objectInfo.getThumbPixHeight() + ", image_pix_width=" + objectInfo.getImagePixWidth() + ", image_pix_height=" + objectInfo.getImagePixHeight() + ", object_format=" + objectInfo.getObjectFormat() + ", parent_object="
                        + objectInfo.getParentObject() + ", association_type=" + objectInfo.getAssociationType() + ">");

                int resizedWidth = 2048;
                int resizedHeight = 1024;
                PtpObject resizedImageObject = camera.getResizedImageObject(objectHandle, resizedWidth, resizedHeight, new DataReceivedListener() {
                    @Override
                    public void onDataPacketReceived(int progress) {
                        publishProgress(progress);
                    }
                });
                return resizedImageObject;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            for (Object param : values) {
                if (param instanceof Integer) {
                    progressBar.setProgress((Integer) param);
                } else if (param instanceof String) {
                    logViewer.append((String) param);
                }
            }
            return;
        }

        @Override
        protected void onPostExecute(PtpObject resizedImageObject) {
            if (resizedImageObject != null) {

                byte[] dataObject = resizedImageObject.getDataObject();

                Bitmap __bitmap = BitmapFactory.decodeByteArray(dataObject, 0, dataObject.length);
                Bitmap _bitmap = Bitmap.createBitmap(__bitmap, 0, 0, __bitmap.getWidth(), __bitmap.getHeight(), null, true);

                progressBar.setVisibility(View.GONE);

                FileOutputStream output;
                try {
                    String tmpFileForReadingExif = "tmpFileForReadingExif";
                    output = openFileOutput(tmpFileForReadingExif, MODE_PRIVATE);
                    output.write(dataObject);
                    output.flush();
                    output.close();

                    File tmpFile = getFileStreamPath(tmpFileForReadingExif);
                    ExifReader exif;
                    exif = new ExifReader(tmpFile);

                    OmniInfo omniInfo = exif.getOmniInfo();
                    Double yaw = omniInfo.getOrientationAngle();
                    Double pitch = omniInfo.getElevationAngle();
                    Double roll = omniInfo.getHorizontalAngle();
                    logViewer.append("<Angle: yaw=" + yaw + ", pitch=" + pitch + ", roll=" + roll + ">");

                    mTexture = new Photo(_bitmap, yaw, pitch, roll);
                    if (null != mGLPhotoView) {
                        mGLPhotoView.setTexture(mTexture);
                    }

                } catch (Throwable throwable) {
                    String errorLog = Log.getStackTraceString(throwable);
                    logViewer.append(errorLog);
                }
            }
            return;
        }
    }

    /**
     * Activity call method
     * 
     * @param activity Call source activity
     * @param cameraIpAddress IP address for camera device
     * @param objectHandle Photo object identifier
     * @param thumbnail Thumbnail
     */
    public static void startActivity(Activity activity, String cameraIpAddress, int objectHandle, byte[] thumbnail) {

        Intent intent = new Intent(activity, GLPhotoActivity.class);
        intent.putExtra(CAMERA_IP_ADDRESS, cameraIpAddress);
        intent.putExtra(OBJECT_HANDLE, objectHandle);
        intent.putExtra(THUMBNAIL, thumbnail);
        activity.startActivity(intent);

        return;
    }
}
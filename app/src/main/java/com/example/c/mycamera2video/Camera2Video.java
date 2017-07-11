package com.example.c.mycamera2video;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by c on 2017/7/10.
 */

public class Camera2Video extends Activity {

    //相机设备
    private CameraDevice mCameraDevice;
    //
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder captureRequest;
private final static int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT=1;

    private File mVideoFolder;
    private String mVideoFileName;

    private Button RecordButton;
    private Button stopButton;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    private int mTotalRotation;

    private Size mPreviewSize;
    private Size mVideoSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mRecordCaptureSession;

    //拍照权限请求码
    private static final int REQUEST_PICTURE_PERMISSION = 1;
    //拍照权限
    private static final String[] PICTURE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            startPreview(width,height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏


        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        createVideoFolder();
        mPreview = (TextureView) findViewById(R.id.surface_view);
        RecordButton = (Button) findViewById(R.id.button_capture);
        RecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(Camera2Video.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {

                }
                else
                {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    {
                        Toast.makeText(Camera2Video.this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
                }

                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setOutputFile(mVideoFileName);
                mMediaRecorder.setVideoEncodingBitRate(1000000);
                mMediaRecorder.setVideoFrameRate(30);
                mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.setOrientationHint(mTotalRotation);
                try {
                    mMediaRecorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SurfaceTexture surfaceTexture = mPreview.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface previewSurface = new Surface(surfaceTexture);
                Surface recordSurface = mMediaRecorder.getSurface();
                try {
                    mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                } catch (CameraAccessException e1) {
                    e1.printStackTrace();
                }
                mCaptureRequestBuilder.addTarget(previewSurface);
                mCaptureRequestBuilder.addTarget(recordSurface);

                try {
                    mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession session) {
                                    mRecordCaptureSession = session;
                                    try {
                                        mRecordCaptureSession.setRepeatingRequest(
                                                mCaptureRequestBuilder.build(), null, null
                                        );
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(CameraCaptureSession session) {
                                    Log.d(TAG, "onConfigureFailed: startRecord");
                                }
                            }, null);
                } catch (CameraAccessException e1) {
                    e1.printStackTrace();
                }
                mMediaRecorder.start();

            }


            }
        );
       stopButton=(Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();

                Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
                sendBroadcast(mediaStoreUpdateIntent);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mPreview.isAvailable()) {
            //TextureView可用时，打开相机
            // openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            //TextureView不可用时，为TextureView设置监听器
            mPreview.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private void startPreview(int width,int height){
        CameraManager manager = (CameraManager) getSystemService(Camera2Video.CAMERA_SERVICE);//1、通过系统服务获取照相机设备管理对象，
        // 等会用该对象获取系统中存在的照相机硬件设备

        try {
            String[] cameraIds = manager.getCameraIdList();//2、通过 1 中的对象获取系统相机设备编号Ids，存在字符串数组中

            //3、由于sdk版本XX之后的特性，权限需要动态赋予，因此在打开摄像头之前向系统需要请求权限。
            //   这里的请求的权限有照相权限、、、
            if (ActivityCompat.checkSelfPermission(Camera2Video.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(Camera2Video.this, PICTURE_PERMISSIONS, REQUEST_PICTURE_PERMISSION);
                //Toast.makeText(this, "???????" , Toast.LENGTH_SHORT).show();

            }

            //4、通过设备号来打开系统对应的照相机设备，如果设备正常打开，会回调openCamera（）的第二个参数。这里第二个参数对应一个内部匿名类的对象，
            //   在这个对象中重写了onOpened （）这个方法，即如果设备处于正常打开，则回调这个方法。

            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraIds[0]);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
            mTotalRotation = (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)+ORIENTATIONS.get(deviceOrientation)+360)%360;
            boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
            int rotatedWidth = width;
            int rotatedHeight = height;
            if(swapRotation) {
                rotatedWidth = height;
                rotatedHeight = width;
            }
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
            mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);

            manager.openCamera(cameraIds[0],  new CameraDevice.StateCallback(){
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice=cameraDevice;
                    mMediaRecorder = new MediaRecorder();
                    //5、初始化将要显示的View子类对象（这里用的是 TextureView 的对象 mTextureview），拿到该View子类对象对应的Surface 。
                    // 但是mTextureview无法和相机设备直接交互，必须中间用一个Surface对象进行过度。
                    // Surface本身就是视图对象View和窗口Window中间的一个缓冲区。

                    SurfaceTexture surfaceTexture=mPreview.getSurfaceTexture();
                    Surface surface=new Surface(surfaceTexture);
                    try {
                        //6、前面5步准备工作做完了，开始建立一个会话，在这个会话中应用请求Camena采取相应动作（这里是预览），然后Camera返回数据完成功能。
                        //
                       captureRequest=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//app请求Camera的相关配置
                        captureRequest.addTarget(surface);//将缓冲区 Surface 对象 surface 加入到请求中
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    try {
                        //7、开始创建会话
                        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {

                                try {
                                    //?????重复会话请求
                                    session.setRepeatingRequest(captureRequest.build(), null,null);
                                    //session.capture(captureRequest.build(), null,null);

                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            }
                        },null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                public void  onDisconnected(CameraDevice camera){
                }
                public void onError(CameraDevice camera, int error){}

            },null );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder = new File(movieFile, "camera2VideoImage");
        if(!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
    }
    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
private static class CompareSizeByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
        return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                (long)(rhs.getWidth() * rhs.getHeight()));
    }
}
}

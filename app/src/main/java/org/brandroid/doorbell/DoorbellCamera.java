package org.brandroid.doorbell;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;

import java.util.Collections;

/**
 * Created by brandon on 12/16/16.
 */

public class DoorbellCamera {
    private static final int IMAGE_WIDTH = 1920;
    private static final int IMAGE_HEIGHT = 1080;
    private static final int MAX_IMAGES = 10;
    private static final String TAG = "DoorbellCam";

    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Surface mPreviewImage = null;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {

        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }
    };

    public static DoorbellCamera getInstance() {
        return new DoorbellCamera();
    }

    public void takePicture() {
        if(mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Not initialized.");
            return;
        }

        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback, null);
        } catch(CameraAccessException cae) {
            Log.d(TAG, "access exception while preparing pic", cae);
        }
    }

    public void setPreviewSurface(Surface surface) {
        mPreviewImage = surface;
    }

    private final CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    triggerImageCapture();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.w(TAG, "Failed to configure camera");
                }
            };

    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            if(mPreviewImage != null)
                captureBuilder.addTarget(mPreviewImage);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch(CameraAccessException cae) {
            Log.d(TAG, "camera capture exception", cae);
        }
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    if(session != null) {
                        session.close();
                        mCaptureSession = null;
                        Log.d(TAG, "CaptureSession closed");
                    }
                }
            };

    public void initializeCamera(Context context, Handler handler, ImageReader.OnImageAvailableListener listener)
    {
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch(CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs", e);
        }
        if(camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }
        String id = camIds[0];

        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES);

        try {
            manager.openCamera(id, mStateCallback, handler);
        } catch(CameraAccessException cae) {
            Log.d(TAG, "Camera access exception", cae);
        } catch(SecurityException sex) {
            Log.e(TAG, "Camera Permission rejected", sex);
        }
    }

    public void shutDown() {
        if(mCameraDevice != null)
            mCameraDevice.close();
    }
}

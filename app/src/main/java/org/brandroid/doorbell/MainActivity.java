package org.brandroid.doorbell;

import android.app.Activity;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends Activity implements Button.OnButtonEventListener, ImageReader.OnImageAvailableListener {

    private final static String TAG = "DoorbellActivity";

    private final static String BUTTON_PIN = "BCM11";

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private DoorbellCamera mCamera;
    private Button mButton;
    private ButtonInputDriver mButtonInputDriver;
    private SurfaceView mMainImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "Available GPIO: " + service.getGpioList());
        startBackgroundThread();
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mBackgroundHandler, this);
        mMainImage = (SurfaceView) findViewById(R.id.preview_surface);
        mCamera.setPreviewSurface(mMainImage.getHolder().getSurface());
    }

    private void initializeDoorbellButton() {
        try {
            mButton = new Button(BUTTON_PIN, Button.LogicState.PRESSED_WHEN_LOW);
            mButton.setOnButtonEventListener(this);
        } catch(IOException e) {
            Log.e(TAG, "Unable to setup doorbell", e);
        }
    }

    @Override
    public void onButtonEvent(Button button, boolean pressed) {
        if(pressed) {
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        ByteBuffer buf = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        image.close();

        onPictureTaken(bytes);
    }

    private void onPictureTaken(byte[] bytes) {
        if(bytes != null) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBackgroundThread != null)
            mBackgroundThread.quitSafely();
        if(mButton != null) {
            try {
                mButton.close();
            } catch(IOException e) {
                Log.e(TAG, "button driver error", e);
            }
        }
        mCamera.shutDown();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("InputThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
}

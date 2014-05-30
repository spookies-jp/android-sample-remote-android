package jp.co.spookies.android.a3.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.KeyguardManager.OnKeyguardExitResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class RemoteCamera extends Activity {
    Camera mCamera = null;
    Size mSize = null;
    Thread mThread = null;
    byte[] mData = null;
    byte[] mTempData = null;
    private Object lockObject = new Object();
    private int interval;
    private int quality;
    private Rect mRect;
    private static final String tagName = "RemoteCamera";
    KeyguardLock mKeyguardLock;
    WakeLock mWakeLock;
    Callback mCallback = new Callback() {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(tagName, "surfaceChanged");
            mCamera.startPreview();
            mCamera.setPreviewCallback(new PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Log.d(tagName, "onPreviewFrame");
//                        synchronized (lockObject) {
                            mData = data;
//                        }
                    Log.d(tagName, "onPreviewFrameEnd");
                }
            });
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(tagName, "surfaceCreated");
            mThread = new CaptureThread();
            mThread.start();
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
                RemoteCamera.this.finish();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(tagName, "surfaceDestroyed");
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
            synchronized (lockObject) {
                mCamera.release();
                mCamera = null;
                mSize = null;
            }
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
            RemoteCamera.this.finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(RemoteCamera.this, AndroidWebSocketServer.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            mCamera = Camera.open();
            Parameters parameters = mCamera.getParameters();
            List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Collections.sort(supportedPreviewSizes, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return o1.width - o2.width;
                }
            });
            // TODO: プレビューサイズのとり方
            mSize = supportedPreviewSizes.get(0);
            mRect = new Rect(0, 0, mSize.width, mSize.height);
            parameters.setPreviewSize(mSize.width, mSize.height);
            parameters.setPreviewFrameRate(5);
            mCamera.setParameters(parameters);
        } catch (RuntimeException e) {
            RemoteCamera.this.finish();
            return;
        }
        // TODO スクリーンロックの解除　挫折した
        // KeyguardManager manager = (KeyguardManager)
        // getSystemService(Context.KEYGUARD_SERVICE);
        // mKeyguardLock = manager.newKeyguardLock(tagName);
        // mKeyguardLock.disableKeyguard();
        // manager.exitKeyguardSecurely(new OnKeyguardExitResult() {
        // @Override
        // public void onKeyguardExitResult(boolean success) {
        // Log.i(tagName, "onKeyguardExitResult");
        // mWakeLock = ((PowerManager)
        // getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
        // tagName);
        // mWakeLock.acquire();
        // }
        // });
        SurfaceView view = new SurfaceView(this);
        setContentView(view);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        view.getHolder().addCallback(mCallback);
        interval = Controller.getCameraInterval(this);
        quality = Controller.getCameraQuality(this);
        Log.i(tagName, "onCreate");
    }

    public void onPause() {
        super.onPause();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.i(tagName, "release WakeLock");
            mWakeLock.release();
        }
        if (mKeyguardLock != null) {
            Log.i(tagName, "release KeyguardLock");
            mKeyguardLock.reenableKeyguard();
        }
        Log.i(tagName, "onPause");
    }

    class CaptureThread extends Thread {
        public void run() {
            while (mData == null) {
                try {
                    Thread.sleep(500);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            while (mCamera != null) {
//                    synchronized(lockObject){
                        (new CompressThread(mData.clone())).start();
//                    }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        class CompressThread extends Thread {
            byte[] yuv;
            public CompressThread(byte[] yuv){
                this.yuv = yuv;
            }
            @Override
            public void run(){
                Log.d(tagName, "save start");
                byte[] data;
                synchronized(lockObject){
                    data = getPreviewImage(yuv);
                }
                try{
                    mService.broadcast(data);
                }catch(RemoteException e){
                    e.printStackTrace();
                }
                Log.d(tagName, "save end");
            }
        }
    }

    /**
     * プレビュー画像の取得
     */
    private byte[] getPreviewImage(byte[] yuv) {
        if (yuv == null || mCamera == null || mSize == null) {
            return null;
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Base64OutputStream base64OutStream = new Base64OutputStream(outStream, Base64.DEFAULT);
        YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, mSize.width, mSize.height, null);
        yuvImage.compressToJpeg(mRect, quality, base64OutStream);
        return outStream.toByteArray();
    }

    IRemoteService mService = null;
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IRemoteService.Stub.asInterface(service);
            try {
                mService.registerCallback(callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    private final ICameraCallback.Stub callback = new ICameraCallback.Stub() {
        @Override
        public void stopCamera() throws RemoteException {
            RemoteCamera.this.finish();
        }
        
        @Override
        public byte[] takeCamera() throws RemoteException {
            Log.w("a","b");
            if(mData == null){
                return null;
            }
            synchronized(lockObject){
                return getPreviewImage(mData.clone());
            }
        }
    };
}

package kr.co.bluebite.police_bluebite;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.widget.Toast;

import kr.co.bluebite.police_bluebite.CameraTextureView;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IStatusCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainPreview extends AppCompatActivity implements CameraDialog.CameraDialogParent {

    // 쓰레드 풀
    private static final int CORE_POOL_SIZE = 1;		// 최소 쓰레드
    private static final int MAX_POOL_SIZE = 4;			// 최대 쓰레드
    private static final int KEEP_ALIVE_TIME = 10;		// 시간동안 유지되는 쓰레드
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private CameraTextureView mCameraTextureView;
    private Surface mPreviewSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_preview);
        mCameraTextureView = (CameraTextureView)findViewById(R.id.CameraTextureView1);
        mCameraTextureView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        if(mUVCCamera == null)
            CameraDialog.showDialog(MainPreview.this);
    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener ()
    {

        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainPreview.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainPreview.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device,final UsbControlBlock ctrlBlock,final boolean createNew) {
            if (mUVCCamera != null)
                mUVCCamera.destroy();
            mUVCCamera = new UVCCamera();
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    mUVCCamera.open(ctrlBlock);
                    mUVCCamera.setStatusCallback(new IStatusCallback() {
                        @Override
                        public void onStatus(final int statusClass,final int event,final int selector
                                            ,final int statusAttribute,final ByteBuffer data) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainPreview.this, "onStatus(statusClass=" + statusClass
                                            + "; " +
                                            "event=" + event + "; " +
                                            "selector=" + selector + "; " +
                                            "statusAttribute=" + statusAttribute + "; " +
                                            "data=...)", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    });
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device,final UsbControlBlock ctrlBlock) {
            if (mUVCCamera != null) {
                mUVCCamera.close();
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
            }
        }

        @Override
        public void onCancel() {

        }

    };

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }
}

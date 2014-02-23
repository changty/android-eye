package teaonly.droideye;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import  org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity 
    implements View.OnTouchListener, CameraView.CameraReadyCallback, OverlayView.UpdateDoneCallback{
    private static final String TAG = "TEAONLY";

	private int PORT =  55555;
	private String SERVER_IP;

    boolean inProcessing = false;
    final int maxVideoNumber = 3;
    VideoFrame[] videoFrames = new VideoFrame[maxVideoNumber];
    byte[] preFrame = new byte[1024*1024*8];
    
    TeaServer webServer = null;
    private CameraView cameraView_;
    private OverlayView overlayView_;
    private Button btnExit;
    private TextView tvMessage1;
    private TextView tvMessage2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    
        //win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 

        setContentView(R.layout.main);

        btnExit = (Button)findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(exitAction);
        tvMessage1 = (TextView)findViewById(R.id.tv_message1);
        tvMessage2 = (TextView)findViewById(R.id.tv_message2);
        
        for(int i = 0; i < maxVideoNumber; i++) {
            videoFrames[i] = new VideoFrame(1024*1024*2);        
        }    
        initCamera();
    }
    
    @Override
    public void onCameraReady() {
        if ( initWebServer() ) {
            int wid = cameraView_.Width();
            int hei = cameraView_.Height();
            cameraView_.StopPreview();
            cameraView_.setupCamera(wid, hei, previewCb_);
            cameraView_.StartPreview();
        }
    }
 
    @Override
    public void onUpdateDone() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }   

    @Override
    public void onStart(){
        super.onStart();
    }   

    @Override
    public void onResume(){
        super.onResume();
    }   
    
    @Override
    public void onPause(){  
        super.onPause();
        inProcessing = true;
        if ( webServer != null)
            webServer.stop();
        cameraView_.StopPreview(); 

    }  

//    @Override
//    public void onBackPressed() {
//       super.onBackPressed();
//    }

    @Override 
    public boolean onTouch(View v, MotionEvent evt) { 
        return false;
    }
    
    /* QR-Code stuff */
	public void readQRCode(View view) {

		Log.d("DEBUG", "Open BARCODE Intent");
		
		try {
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // Set zxing reader to QR-mode
			startActivityForResult(intent, 0); // requestCode 0
		}
		catch (Exception e) {
			Uri marketUri = Uri.parse("market://details?=id=com.google.zxing.client.android");
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
			startActivity(marketIntent);
		}
	}
	
	// Catch intent activity results (mainly QR-code reading)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d("QRCODE", requestCode + ", resultCode: " + resultCode);
		if(requestCode == 0) {
			if(resultCode == RESULT_OK) {
				String qrCode = data.getStringExtra("SCAN_RESULT");
				Log.d("OUTPUT", qrCode);
				
				TextView t = (TextView)findViewById(R.id.hello1);
				t.setText(qrCode);
				
				// save ip to class variable
				this.SERVER_IP = qrCode;
				//send a 'hello world' -message to server
				new Sender(SERVER_IP, PORT).execute("Hello world!");
			}
			
			if(resultCode == RESULT_CANCELED) {
				Log.d("DEBUG", "QR read cancelled");
			}
		}
	}

    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView_ = new CameraView(cameraSurface);        
        cameraView_.setCameraReadyCallback(this);

        overlayView_ = (OverlayView)findViewById(R.id.surface_overlay);
        overlayView_.setOnTouchListener(this);
        overlayView_.setUpdateDoneCallback(this);
    }
    
    public String getLocalIpAddress() {
    	//Get WLAN-IP (because we are only streamin to LAN)
		String wlanIP =  Utils.getIPAddress(true, "wlan0");

		// For now we'll just pass on the public IP if no LAN-ip is found.
		if(wlanIP.equals("")) {
	        try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()) ) {
	                        String ipAddr = inetAddress.getHostAddress();
	                        return ipAddr;
	                    }
	                }
	            }
	        } catch (SocketException ex) {
	            Log.d(TAG, ex.toString());
	        }
	        return null;
		}
		else {
			return wlanIP;
		}
    }   

    private boolean initWebServer() {
        String ipAddr = getLocalIpAddress();
        if ( ipAddr != null ) {
            try{
                webServer = new TeaServer(8080, this); 
                webServer.registerCGI("/cgi/query", doQuery);
                webServer.registerCGI("/cgi/setup", doSetup);
                webServer.registerCGI("/stream/live.jpg", doCapture);
            }catch (IOException e){
                webServer = null;
            }
        }
        if ( webServer != null) {
            tvMessage1.setText( getString(R.string.connect_to_pc));
            //+ " http://" + ipAddr  + ":8080" 
            return true;
        } else {
            tvMessage1.setText( getString(R.string.msg_error) );
            tvMessage2.setVisibility(View.GONE);
            return false;
        }
          
    }
   
    //Exit action?
    private OnClickListener exitAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onPause();
            finish();
        }   
    };
   
    private PreviewCallback previewCb_ = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            if ( !inProcessing ) {
                inProcessing = true;
           
                int picWidth = cameraView_.Width();
                int picHeight = cameraView_.Height(); 
                ByteBuffer bbuffer = ByteBuffer.wrap(frame); 
                bbuffer.get(preFrame, 0, picWidth*picHeight + picWidth*picHeight/2);

                inProcessing = false;
            }
        }
    };
    
    private TeaServer.CommonGatewayInterface doQuery = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            String ret = "";
            List<Camera.Size> supportSize =  cameraView_.getSupportedPreviewSize();                             
            ret = ret + "" + cameraView_.Width() + "x" + cameraView_.Height() + "|";
            for(int i = 0; i < supportSize.size() - 1; i++) {
                ret = ret + "" + supportSize.get(i).width + "x" + supportSize.get(i).height + "|";
            }
            int i = supportSize.size() - 1;
            ret = ret + "" + supportSize.get(i).width + "x" + supportSize.get(i).height ;
            return ret;
        }
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    }; 

    private TeaServer.CommonGatewayInterface doSetup = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            int wid = Integer.parseInt(parms.getProperty("wid")); 
            int hei = Integer.parseInt(parms.getProperty("hei"));
            Log.d("TEAONLY", ">>>>>>>run in doSetup wid = " + wid + " hei=" + hei);
            cameraView_.StopPreview();
            cameraView_.setupCamera(wid, hei, previewCb_);
            cameraView_.StartPreview();
            return "OK";
        }   
 
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    }; 

    private TeaServer.CommonGatewayInterface doCapture = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
           return null;
        }   
        
        @Override 
        public InputStream streaming(Properties parms) {
            VideoFrame targetFrame = null;
            for(int i = 0; i < maxVideoNumber; i++) {
                if ( videoFrames[i].acquire() ) {
                    targetFrame = videoFrames[i];
                    break;
                }
            }
            // return 503 internal error
            if ( targetFrame == null) {
                Log.d("TEAONLY", "No free videoFrame found!");
                return null;
            }

            // compress yuv to jpeg
            int picWidth = cameraView_.Width();
            int picHeight = cameraView_.Height(); 
            YuvImage newImage = new YuvImage(preFrame, ImageFormat.NV21, picWidth, picHeight, null);
            targetFrame.reset();
            boolean ret;
            inProcessing = true;
            try{
                ret = newImage.compressToJpeg( new Rect(0,0,picWidth,picHeight), 30, targetFrame);
            } catch (Exception ex) {
                ret = false;    
            } 
            inProcessing = false;

            // compress success, return ok
            if ( ret == true)  {
                parms.setProperty("mime", "image/jpeg");
                InputStream ins = targetFrame.getInputStream();
                return ins;
            }
            // send 503 error
            targetFrame.release();

            return null;
        }
    }; 

}    


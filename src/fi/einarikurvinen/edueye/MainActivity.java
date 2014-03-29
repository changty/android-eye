package fi.einarikurvinen.edueye;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import  org.apache.http.conn.util.InetAddressUtils;

import fi.einarikurvinen.edueye.helpers.ThreadManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity 
    implements View.OnTouchListener, CameraView.CameraReadyCallback, OverlayView.UpdateDoneCallback{
    private static final String TAG = "CHGT";

    private int WWWPORT = 44451;
	private int PORT =  55555;
	private String SERVER_IP;

    boolean inProcessing = false;
    final int maxVideoNumber = 3;
    VideoFrame[] videoFrames = new VideoFrame[maxVideoNumber];
    byte[] preFrame = new byte[1920*1920*8];
    
    EduEyeServer webServer = null;
    private CameraView cameraView_;
    private OverlayView overlayView_;
    private ImageView btn ;
    private TextView tvMessage1;
    private TextView tvMessage2;
    private TextView debug;
    private ImageView readQR;
    private Button closeInfo;
    private LinearLayout info;
    private FrameLayout defaultView; 
    
    boolean frontCamera; 
    boolean rearCamera;
    
    private ProgressBar busy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    

        setContentView(R.layout.main);

        
    	PackageManager pm = getPackageManager();
    
    	//Must have a targetSdk >= 9 defined in the AndroidManifest
    	frontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    	rearCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    	        
       // btnExit = (ImageView)findViewById(R.id.btn_exit);
       // btnExit.setOnClickListener(exitAction);
    	closeInfo  = (Button)findViewById(R.id.close_info);
    	info = (LinearLayout)findViewById(R.id.info);
    	defaultView = (FrameLayout)findViewById(R.id.default_view);
    	
    	tvMessage1 = (TextView)findViewById(R.id.tv_message1);
      
        //not in use atm.
        tvMessage2 = (TextView)findViewById(R.id.tv_message2);
        
        debug = (TextView)findViewById(R.id.hello1);
        debug.setText("");
        busy = (ProgressBar)findViewById(R.id.busy);

        readQR = (ImageView)findViewById(R.id.qrCode);
        readQR.setEnabled(false);
        
        for(int i = 0; i < maxVideoNumber; i++) {
            videoFrames[i] = new VideoFrame(1024*1024*2);        
        }    
        
        //Start camera anyway
        //This will also start the webserver.
        initCamera();

        
        //Check that WLAN is in use and connected
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        
        //Check that we have a wifi connection AND we have atleast one camera.
        if(mWifi.isConnected() && (frontCamera || rearCamera)) {
        	tvMessage1.setText(R.string.connect_to_pc);
            readQR.setEnabled(true); 
        }
        
        // If there is no connection, we'll establish one and wait it to connect.
        // After connection, we'll just move forward
        else if(frontCamera || rearCamera){
        	tvMessage1.setText(R.string.connecting_to_wlan);
        	toggleBusy();
        	
        	WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        	wifiManager.setWifiEnabled(true);
        	
            ThreadManager.runInBackgroundThenUi(new Runnable() {
                @Override
                public void run() {               
                	//Check that WLAN is in use and connected
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    
                    while(!mWifi.isConnected())  {
                    	Log.d("DEBUG", "Checking wifi connection");
                    	mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    	SystemClock.sleep(1000);
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                	toggleBusy();
                	tvMessage1.setText(R.string.connect_to_pc);
                    readQR.setEnabled(true);            }
            });
        }
        
        else {
        	tvMessage1.setText(R.string.no_camera);
        }
    }
    
    @Override
    public void onCameraReady() {
        if ( initWebServer() ) {
            int wid = cameraView_.Width();
            int hei = cameraView_.Height();
            cameraView_.StopPreview();
            cameraView_.setupCamera(wid, hei, previewCb_);
            
            //Turn camera the correct angle by default
            cameraView_.setCameraDisplayOrientation(this);
            cameraView_.StartPreview();
 
        }
    }
    
    //Catch screen orientation changes
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
    	if(cameraView_.isReady()) {
    		cameraView_.setCameraDisplayOrientation(this);
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
        cameraView_.Release();
    }  

    @Override
    public void onBackPressed() {
       //super.onBackPressed();
       
       if(defaultView.getVisibility() != View.VISIBLE) {
    	  info.setVisibility(View.GONE);
    	   defaultView.setVisibility(View.VISIBLE); 
    	  
       }
       else {
    	   super.onBackPressed();
       }
       
    }

    @Override 
    public boolean onTouch(View v, MotionEvent evt) { 
        return false;
    }
    

    public void zoom(int level) {
    	cameraView_.setZoom(level);
    	cameraView_.AutoFocus();
    }
    
    public void autoFocus() {
    	cameraView_.AutoFocus();
    }
    
    //Busy -sign for starting wlan
    private void toggleBusy() {

    	if(busy.isShown()) {
    		busy.setVisibility(View.INVISIBLE);
    		busy.setEnabled(false);
    	}
    	else {
    		busy.setEnabled(true);
    		busy.setVisibility(View.VISIBLE);
    	}
    }
    
    /*Close info view */
    public void closeInfo(View view) {
    	info.setVisibility(View.GONE);
    	defaultView.setVisibility(View.VISIBLE);
    }
    
    public void showInfo(View view) {
    	defaultView.setVisibility(View.GONE);
    	info.setVisibility(View.VISIBLE);
    }
    
    /* QR-Code stuff */
	public void readQRCode(View view) {		
		Log.d("DEBUG", "Open BARCODE Intent");
		//onPause();
		try {
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // Set zxing reader to QR-mode
			startActivityForResult(intent, 0); // requestCode 0
		}
		catch (Exception e) {			
			Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
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
				
				debug.setText("Connected to: " + qrCode);
				
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
    	Toast error = Toast.makeText(this, R.string.no_camera, Toast.LENGTH_LONG);

    	if(frontCamera || rearCamera) {
    	// detect camera here!
    	
        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView_ = new CameraView(cameraSurface);        
        cameraView_.setCameraReadyCallback(this);

        overlayView_ = (OverlayView)findViewById(R.id.surface_overlay);
        overlayView_.setOnTouchListener(this);
        overlayView_.setUpdateDoneCallback(this);
    	}
    	else {
        	error.show();
    	}
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
                webServer = new EduEyeServer(WWWPORT, this); 
                webServer.registerCGI("/cgi/query", doQuery);
                webServer.registerCGI("/cgi/setup", doSetup);
                webServer.registerCGI("/stream/live.jpg", doCapture);
                webServer.registerCGI("/cgi/zoom", doZoom);
                webServer.registerCGI("/cgi/focus", doFocus);
                webServer.registerCGI("/cgi/getMaxZoom", getMaxZoom);
            }catch (IOException e){
                webServer = null;
            }
        }
        if ( webServer != null) {
            //tvMessage1.setText( getString(R.string.connect_to_pc));
            //+ " http://" + ipAddr  + ":8080" 
            return true;
        } else {
            tvMessage1.setText( getString(R.string.msg_error) );
            tvMessage2.setVisibility(View.GONE);
            return false;
        }
          
    }
   
    //Exit action
//    private OnClickListener exitAction = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            onPause();
//            finish();
//        }   
//    };
   
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
    
    private EduEyeServer.CommonGatewayInterface doQuery = new EduEyeServer.CommonGatewayInterface () {
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

    private EduEyeServer.CommonGatewayInterface doSetup = new EduEyeServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            int wid = Integer.parseInt(parms.getProperty("wid")); 
            int hei = Integer.parseInt(parms.getProperty("hei"));
            //Log.d("CHGT", ">>>>>>>run in doSetup wid = " + wid + " hei=" + hei);
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

    private EduEyeServer.CommonGatewayInterface doCapture = new EduEyeServer.CommonGatewayInterface () {
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
            try{// 30 here is the image quality. Should it be raised later on?
                ret = newImage.compressToJpeg( new Rect(0,0,picWidth,picHeight), 60, targetFrame);
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
    
    private EduEyeServer.CommonGatewayInterface doZoom = new EduEyeServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            int zoom = Integer.parseInt(parms.getProperty("zoom")); 
            Log.d("CHGT", ">>>>>>>run in doZoom zoom = " + zoom);
            zoom(zoom);
            return "OK";
        }   
 
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    };  
    
    private EduEyeServer.CommonGatewayInterface doFocus = new EduEyeServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            autoFocus();
            return "OK";
        }   
 
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    };  
    
    private EduEyeServer.CommonGatewayInterface getMaxZoom = new EduEyeServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            return cameraView_.getMaxZoom();
        }   
 
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    };  


}    


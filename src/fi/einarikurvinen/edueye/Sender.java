package fi.einarikurvinen.edueye;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class Sender extends AsyncTask<String, Object, String> {
	
	private String SERVER_IP;
	private int PORT;
	
	public Sender(String SERVER_IP, int PORT) {
		this.SERVER_IP = SERVER_IP; 
		this.PORT = PORT;
	}
	
	@Override
	protected String doInBackground(String... d) {
		
		String data = d[0];
		
		Log.d("NETWORKING", "Trying to send data to server");
		if(!this.SERVER_IP.equals("") || this.SERVER_IP != null) {
			
			//Get server address
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName(SERVER_IP);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				Log.d("ERROR", "Failed to get InetAddress");
				e1.printStackTrace();
			}
			
			//Create socket
			Log.d("DEBUG", SERVER_IP + ", " + PORT);
			DatagramSocket socket = null;
			try {
				socket = new DatagramSocket();
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				Log.d("ERROR", "Failed to initiate Datagram socket");
				e1.printStackTrace();
			} 
			
			//Send the actual package
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, addr, PORT);
			try {
				socket.send(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d("NETWORKING", "Package sent");
			socket.close();
			return "SUCCESS";
		}
		return "FAILED";
	}

	protected void onPostExecute(Boolean flag) {
		
	}
}

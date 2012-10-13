package ch.bfh.adhocnetwork;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class AdhocNetworkService extends Service {

	private static final String TAG = AdhocNetworkService.class.getSimpleName();
	private InetAddress broadcast;
	private DatagramSocket s;

	public AdhocNetworkService() {
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
		new Thread(new UDPBroadcastReceiverThread(this)).start();
		Log.d(TAG, "onCreate()");
		Toast.makeText(this, "onCreate()", Toast.LENGTH_SHORT).show();

		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter("messageSend"));

		broadcast = getBroadcastAddress();
	}

	public void processMessage(Message msg) {
		Log.d(TAG, msg.getMessage());
		Intent intent = new Intent("messageReceived");
		// You can also include some extra data.
		intent.putExtra("message", msg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private class BroadcastMessageAsyncTask extends
			AsyncTask<Message, Integer, Integer> {
		protected Integer doInBackground(Message... msg) {
			Log.d(TAG, "Broadcasting message");

			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);
				out.writeObject(msg[0]);
				byte[] bytes = bos.toByteArray();
				s = new DatagramSocket();
				s.setBroadcast(true);
				s.setReuseAddress(true);
				DatagramPacket p = new DatagramPacket(bytes, bytes.length,
						broadcast, 12345);
				s.send(p);
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
		}
		
	}

	// Broadcasting a message
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			Message msg = (Message) intent.getSerializableExtra("message");
			new BroadcastMessageAsyncTask().execute(msg);
		}
	};
	
	public InetAddress getBroadcastAddress() {
		InetAddress found_bcast_address = null;
		System.setProperty("java.net.preferIPv4Stack", "true");
		try {
			Enumeration<NetworkInterface> niEnum = NetworkInterface
					.getNetworkInterfaces();
			while (niEnum.hasMoreElements()) {
				NetworkInterface ni = niEnum.nextElement();
				
				if (ni.getDisplayName().contains("p2p-wlan")){
					for (InterfaceAddress interfaceAddress : ni
							.getInterfaceAddresses()) {

						found_bcast_address = interfaceAddress.getBroadcast();
						
						// found_bcast_address =
						// found_bcast_address.substring(1);

					}
					if (found_bcast_address != null){
						Log.d(TAG, "found p2p Broadcast address: " + found_bcast_address.toString());
						break;
					}
				}
				
			}
			
			if (found_bcast_address == null){
				Log.d(TAG, "no p2p Broadcast addresses found, now trying to find network broadcast adresses");
				niEnum = NetworkInterface.getNetworkInterfaces();
				while (niEnum.hasMoreElements()) {
					NetworkInterface ni = niEnum.nextElement();
					Log.d(TAG, "looping...");
					if (!ni.isLoopback()) {
						for (InterfaceAddress interfaceAddress : ni
								.getInterfaceAddresses()) {
	
							found_bcast_address = interfaceAddress.getBroadcast();
							
							// found_bcast_address =
							// found_bcast_address.substring(1);
	
						}
						
						if (found_bcast_address != null){
							Log.d(TAG, "found Broadcast address: " + found_bcast_address.toString());
							break;
						}
						
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return found_bcast_address;
	}


}

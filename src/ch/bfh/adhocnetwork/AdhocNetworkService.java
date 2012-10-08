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

		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				if (networkInterface.isLoopback())
					continue; // Don't want to broadcast to the loopback
								// interface
				for (InterfaceAddress interfaceAddress : networkInterface
						.getInterfaceAddresses()) {
					broadcast = interfaceAddress.getBroadcast();

					if (broadcast == null) {
						continue;
					} else {
						Log.d(TAG, broadcast.getHostAddress());
					}
				}
			}

			if (broadcast == null) {
				try {
					broadcast = InetAddress.getByName("192.168.1.255");
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			try {
				broadcast = InetAddress.getByName("192.168.1.255");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "using " + broadcast.getHostAddress());
		} catch (SocketException e) {

			e.printStackTrace();
		}

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

}

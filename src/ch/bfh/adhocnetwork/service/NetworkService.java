package ch.bfh.adhocnetwork.service;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import ch.bfh.adhocnetwork.Message;
import ch.bfh.adhocnetwork.db.NetworkDbHelper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class NetworkService extends Service {

	private static final String TAG = NetworkService.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private InetAddress broadcast;
	private DatagramSocket s;

	private Set<String> availableNetworks = Collections
			.synchronizedSet(new HashSet<String>());

	private String networkUUID;

	private NetworkDbHelper dbHelper;

	public NetworkService() {
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
		if (networkUUID != null && dbHelper == null) {
			dbHelper = new NetworkDbHelper(this, networkUUID);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		new Thread(new UDPBroadcastReceiverThread(this)).start();
		Log.d(TAG, "Service started");
		Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();

		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter("messageSend"));

		broadcast = getBroadcastAddress();

		Log.d(TAG, "Intent Extra: " + intent.getStringExtra("action"));

		if (intent.getStringExtra("action").equals("createnetwork")) {
			createNetwork();
		} else if (intent.getStringExtra("action").equals("joinnetwork")) {
			networkUUID = null;
			new DiscoverNetworkTask().execute();
			
			int i = 0;
			while (i < 10){
				if (networkUUID != null){
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				i++;
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public void processMessage(Message msg) {
		Log.d(TAG, "Message received...");

		if (dbHelper != null) {
			dbHelper.insertMessage(msg);
		}

		Intent intent = new Intent("messagesChanged");
		// You can also include some extra data.
		// intent.putExtra("message", msg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		switch (msg.getMessageType()) {

		case Message.MSG_CONTENT:
			Log.d(TAG, "Content...");
			break;
		case Message.MSG_MSGJOIN:
			Log.d(TAG, "Join...");
			dbHelper.insertParticipants(msg.getMessage());
			break;
		case Message.MSG_MSGLEAVE:
			Log.d(TAG, "Leave...");
			break;
		case Message.MSG_MSGRESENDREQ:
			Log.d(TAG, "Resend Request...");
			break;
		case Message.MSG_MSGRESENDRES:
			Log.d(TAG, "Resend Response...");
			break;
		case Message.MSG_NETWORKAD:
			Log.d(TAG, "got network advertisement for " + msg.getMessage() + ", adding to network list...");
			availableNetworks.add(msg.getMessage());
			break;
		case Message.MSG_DISCOVERNETS:
			Log.d(TAG, "got discovernetwork, advertising...");
			advertiseNetwork();
			break;
		default:
			Log.d(TAG, "Default...");
			break;
		}

	}

	private class BroadcastMessageAsyncTask extends
			AsyncTask<Message, Integer, Integer> {
		protected Integer doInBackground(Message... msg) {

			if (broadcast == null) {
				broadcast = getBroadcastAddress();
			}

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

	private class DiscoverNetworkTask extends
			AsyncTask<Void, Void, Void> {
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (availableNetworks.isEmpty()) {
				Log.d(TAG, "no networks found");
			} else {
				networkUUID = availableNetworks.iterator().next();
				joinNetwork(networkUUID,
						getSharedPreferences("network_preferences", 0)
								.getString("identifier", "N/A"));

				Log.d(TAG, "connected to network " + networkUUID);
			}
			
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			discoverNetworks();
		}

		protected Void doInBackground(Void... args) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
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

				if (ni.getDisplayName().contains("p2p-wlan")) {
					for (InterfaceAddress interfaceAddress : ni
							.getInterfaceAddresses()) {

						found_bcast_address = interfaceAddress.getBroadcast();

						// found_bcast_address =
						// found_bcast_address.substring(1);

					}
					if (found_bcast_address != null) {
						Log.d(TAG, "found p2p Broadcast address: "
								+ found_bcast_address.toString());
						break;
					}
				}

			}

			if (found_bcast_address == null) {
				Log.d(TAG,
						"no p2p Broadcast addresses found, now trying to find network broadcast adresses");
				niEnum = NetworkInterface.getNetworkInterfaces();
				while (niEnum.hasMoreElements()) {
					NetworkInterface ni = niEnum.nextElement();
					Log.d(TAG, "looping...");
					if (!ni.isLoopback()) {
						for (InterfaceAddress interfaceAddress : ni
								.getInterfaceAddresses()) {

							found_bcast_address = interfaceAddress
									.getBroadcast();

							// found_bcast_address =
							// found_bcast_address.substring(1);

						}

						if (found_bcast_address != null) {
							Log.d(TAG, "found Broadcast address: "
									+ found_bcast_address.toString());
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

	private void createNetwork() {
		networkUUID = UUID.randomUUID().toString();
		joinNetwork(networkUUID, getSharedPreferences("network_preferences", 0)
				.getString("identification", "N/A"));
	}

	private void joinNetwork(String networkUUID, String identifier) {
		dbHelper = new NetworkDbHelper(this, networkUUID);
		Message joinMessage = new Message(identifier, 1, Message.MSG_MSGJOIN,
				networkUUID);
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("networkUUID", networkUUID);
		editor.commit();
		new BroadcastMessageAsyncTask().execute(joinMessage);
	}

	private void discoverNetworks() {
		Message discoverMessage = new Message(getSharedPreferences(
				"network_preferences", 0).getString("identifier", "N/A"), 1,
				Message.MSG_DISCOVERNETS);
		new BroadcastMessageAsyncTask().execute(discoverMessage);
	}

	private void advertiseNetwork() {
		Message adMessage = new Message(networkUUID, 1, Message.MSG_NETWORKAD,
				networkUUID);
		new BroadcastMessageAsyncTask().execute(adMessage);
	}

	public String getNetworkUUID() {
		return networkUUID;
	}

	public Set<String> getAvilableNetworks() {
		return availableNetworks;
	}
}

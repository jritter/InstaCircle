package ch.bfh.instacircle.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import ch.bfh.instacircle.MainActivity;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.NetworkActiveActivity;
import ch.bfh.instacircle.R;
import ch.bfh.instacircle.db.NetworkDbHelper;
import ch.bfh.instacircle.wifi.AdhocWifiManager;
import ch.bfh.instacircle.wifi.WifiAPManager;

public class NetworkService extends Service {

	private static final String TAG = NetworkService.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private static final Integer[] messagesToSave = { Message.MSG_CONTENT,
			Message.MSG_MSGJOIN, Message.MSG_MSGLEAVE };

	private InetAddress broadcast;

	private Set<String> availableNetworks = Collections
			.synchronizedSet(new HashSet<String>());

	private volatile boolean advertisementArrived = false;

	private NetworkDbHelper dbHelper;

	private UDPBroadcastReceiverThread udpBroadcastReceiverThread;
	private TCPUnicastReceiverThread tcpUnicastReceiverThread;

	private String cipherKey;

	public NetworkService() {

	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				messageSendReceiver);
		dbHelper.close();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		dbHelper = new NetworkDbHelper(this);

		Intent myIntent = new Intent(this, NetworkActiveActivity.class);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, myIntent,
				myIntent.getFlags());

		cipherKey = getSharedPreferences(PREFS_NAME, 0).getString("password",
				"N/A");

		Log.d(TAG, "Using the cipher key " + cipherKey);

		Notification.Builder notificationBuilder = new Notification.Builder(
				this);
		notificationBuilder.setContentTitle("Adhoc Network Chat");
		notificationBuilder
				.setContentText("An Adhoc Network Chat session is running. Tap to bring in front.");
		notificationBuilder
				.setSmallIcon(R.drawable.glyphicons_244_conversation);
		notificationBuilder.setContentIntent(pIntent);
		notificationBuilder.setOngoing(true);
		Notification notification = notificationBuilder.getNotification();

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(TAG, 1, notification);

		udpBroadcastReceiverThread = new UDPBroadcastReceiverThread(this,
				cipherKey);
		udpBroadcastReceiverThread.start();

		tcpUnicastReceiverThread = new TCPUnicastReceiverThread(this, cipherKey);
		tcpUnicastReceiverThread.start();

		Log.d(TAG, "Service started");

		LocalBroadcastManager.getInstance(this).registerReceiver(
				messageSendReceiver, new IntentFilter("messageSend"));

		do {
			broadcast = getBroadcastAddress();
			if (broadcast != null) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (broadcast == null);

		Log.d(TAG, "Broadcast Address: " + broadcast.getHostAddress());

		dbHelper.openConversation(getSharedPreferences(PREFS_NAME, 0)
				.getString("password", "N/A"));

		joinNetwork(getSharedPreferences(PREFS_NAME, 0).getString(
				"identification", "N/A"));

		intent = new Intent(this, NetworkActiveActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		startActivity(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	public void processBroadcastMessage(Message msg) {
		Log.d(TAG, "Broadcast Message received...");
		String identification = getSharedPreferences(PREFS_NAME, 0).getString(
				"identification", "N/A");
		Intent intent;
		if (!checkMessageConsistency()) {
			return;
		}

		switch (msg.getMessageType()) {

		case Message.MSG_CONTENT:
			Log.d(TAG, "Content...");
			break;
		case Message.MSG_MSGJOIN:
			Log.d(TAG, "Join...");
			dbHelper.insertParticipant(msg.getMessage(),
					msg.getSenderIPAddress());
			intent = new Intent("participantJoined");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
			break;
		case Message.MSG_MSGLEAVE:
			Log.d(TAG, "Leave...");
			dbHelper.updateParticipantState(msg.getSender(), 0);

			// Don't broadcast if I'm the one who left...
			if (!msg.getSender().equals(identification)) {
				intent = new Intent("participantChangedState");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
			}
			break;
		case Message.MSG_MSGRESENDREQ:
			// should be handled as unicast
			break;
		case Message.MSG_MSGRESENDRES:
			// should be handled as unicast
			break;
		case Message.MSG_WHOISTHERE:
			Message response = new Message(
					(dbHelper.getNextSequenceNumber() - 1) + "",
					Message.MSG_IAMHERE, identification, -1);
			new UnicastMessageAsyncTask(msg.getSenderIPAddress())
					.execute(response);
			break;
		case Message.MSG_IAMHERE:
			// should be handled as unicast
			break;
		default:
			Log.d(TAG, "Default...");
			break;
		}

		if (Arrays.asList(messagesToSave).contains(msg.getMessageType())) {

			// Check whether participant already exists in the database

			Log.d(TAG,
					"Got message with sequence number "
							+ msg.getSequenceNumber());
			Log.d(TAG,
					"Expecting sequence Number "
							+ (dbHelper.getCurrentParticipantSequenceNumber(msg
									.getSender()) + 1));

			if (msg.getSequenceNumber() != -1
					&& msg.getSequenceNumber() > dbHelper
							.getCurrentParticipantSequenceNumber(msg
									.getSender()) + 1) {
				// Request missing messages
				Log.d(TAG, "Messagelist from " + msg.getSender()
						+ " incomplete, requesting messages...");
				Message resendRequestMessage = new Message("",
						Message.MSG_MSGRESENDREQ, getSharedPreferences(
								PREFS_NAME, 0).getString("identification",
								"N/A"), -1);
				new UnicastMessageAsyncTask(msg.getSenderIPAddress())
						.execute(resendRequestMessage);
			} else {
				if (dbHelper != null) {
					dbHelper.insertMessage(msg);
				}
			}
		}

		// Stop everything as soon as the own leave message has been processed
		if (msg.getMessageType() == Message.MSG_MSGLEAVE
				&& msg.getSender().equals(identification)) {
			tcpUnicastReceiverThread.interrupt();
			udpBroadcastReceiverThread.interrupt();
			try {
				tcpUnicastReceiverThread.serverSocket.close();
				udpBroadcastReceiverThread.socket.close();
			} catch (IOException e) {

			}

			dbHelper.closeConversation();
			WifiManager wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			new AdhocWifiManager(wifiman)
					.restoreWifiConfiguration(getBaseContext());
			new WifiAPManager().disableHotspot(wifiman, getBaseContext());
			stopSelf();
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancelAll();
			intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		} else {

			intent = new Intent("messageArrived");
			intent.putExtra("message", msg);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}

	}

	public void processUnicastMessage(Message msg) {
		Log.d(TAG, "Broadcast Message received...");

		if (!checkMessageConsistency()) {
			return;
		}

		switch (msg.getMessageType()) {

		case Message.MSG_CONTENT:
			Log.d(TAG, "Content...");
			break;

		case Message.MSG_MSGRESENDREQ:
			Log.d(TAG, "Got resendrequest from " + msg.getSender());
			Cursor myMessages = dbHelper.queryMyMessages();
			ArrayList<Message> messages = new ArrayList<Message>();
			// iterate over cursor
			for (boolean hasItem = myMessages.moveToFirst(); hasItem; hasItem = myMessages
					.moveToNext()) {

				// Assemble new messages from database
				messages.add(new Message(myMessages.getString(myMessages
						.getColumnIndex("message")), myMessages
						.getInt(myMessages.getColumnIndex("message_type")),
						myMessages.getString(myMessages
								.getColumnIndex("identification")), myMessages
								.getInt(myMessages
										.getColumnIndex("sequence_number"))));
			}

			// serializing the list to a Base64 String
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(messages);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String serializedMessages = Base64.encodeToString(
					baos.toByteArray(), Base64.DEFAULT);

			String identification = getSharedPreferences("network_preferences",
					0).getString("identification", "N/A");

			Message resendMessage = new Message(serializedMessages,
					Message.MSG_MSGRESENDRES, identification, -1);

			new UnicastMessageAsyncTask(msg.getSenderIPAddress())
					.execute(resendMessage);
			break;

		case Message.MSG_MSGRESENDRES:
			Log.d(TAG, "Got all messages from " + msg.getSender());

			try {
				ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream(Base64.decode(
								msg.getMessage(), Base64.DEFAULT)));
				ArrayList<Message> deserializedMessages = (ArrayList<Message>) ois
						.readObject();
				for (Message message : deserializedMessages) {
					Log.d(TAG, "Reprocessing message...");
					message.setSenderIPAddress(msg.getSenderIPAddress());
					processBroadcastMessage(message);
				}
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			break;

		case Message.MSG_IAMHERE:
			Log.d(TAG,
					"Got I am here message from " + msg.getSender()
							+ " with Sequence number "
							+ Integer.parseInt(msg.getMessage()));

			// Check if the received sequence number is bigger than the one we
			// have in our db, request messages if true
			if (Integer.parseInt(msg.getMessage()) > dbHelper
					.getCurrentParticipantSequenceNumber(msg.getSender())) {
				// Request missing messages
				Log.d(TAG, "Messagelist from " + msg.getSender()
						+ " incomplete, requesting messages...");
				Message resendRequestMessage = new Message("",
						Message.MSG_MSGRESENDREQ, getSharedPreferences(
								PREFS_NAME, 0).getString("identification",
								"N/A"), -1);
				new UnicastMessageAsyncTask(msg.getSenderIPAddress())
						.execute(resendRequestMessage);
			}

			break;
		}

		// non-Broadcast messages don't get a valid sequence number
		if (Arrays.asList(messagesToSave).contains(msg.getMessageType())) {
			msg.setSequenceNumber(-1);
			dbHelper.insertMessage(msg);
		}

		Intent intent = new Intent("messageArrived");
		intent.putExtra("message", msg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private boolean checkMessageConsistency() {
		return true;
	}

	private class BroadcastMessageAsyncTask extends
			AsyncTask<Message, Integer, Integer> {

		private DatagramSocket s;

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
				byte[] encryptedBytes = encrypt(cipherKey.getBytes(), bytes);

				ByteBuffer b = ByteBuffer.allocate(4);
				b.putInt(encryptedBytes.length);
				byte[] length = b.array();

				byte[] bytesToSend = new byte[length.length
						+ encryptedBytes.length];

				System.arraycopy(length, 0, bytesToSend, 0, length.length);
				System.arraycopy(encryptedBytes, 0, bytesToSend, length.length,
						encryptedBytes.length);

				s = new DatagramSocket();
				s.setBroadcast(true);
				s.setReuseAddress(true);
				DatagramPacket p = new DatagramPacket(bytesToSend,
						bytesToSend.length, broadcast, 12345);
				s.send(p);
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return 0;
		}
	}

	private class UnicastMessageAsyncTask extends
			AsyncTask<Message, Integer, Integer> {

		private String destinationAddr;
		private Socket s;

		public UnicastMessageAsyncTask(String destinationAddr) {
			this.destinationAddr = destinationAddr;
		}

		protected Integer doInBackground(Message... msg) {

			if (broadcast == null) {
				broadcast = getBroadcastAddress();
			}

			Log.d(TAG, "Sending message to " + destinationAddr);

			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);
				out.writeObject(msg[0]);

				byte[] bytes = bos.toByteArray();
				byte[] encryptedBytes = encrypt(cipherKey.getBytes(), bytes);
				s = new Socket(destinationAddr, 12345);
				out.close();

				ByteBuffer b = ByteBuffer.allocate(4);
				b.putInt(encryptedBytes.length);
				byte[] length = b.array();

				byte[] bytesToSend = new byte[length.length
						+ encryptedBytes.length];

				System.arraycopy(length, 0, bytesToSend, 0, length.length);
				System.arraycopy(encryptedBytes, 0, bytesToSend, length.length,
						encryptedBytes.length);

				DataOutputStream dOut = new DataOutputStream(
						s.getOutputStream());
				dOut.write(bytesToSend);
				dOut.flush();
				dOut.close();
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return 0;
		}
	}

	private class DiscoverNetworkTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (availableNetworks.isEmpty()) {
				Log.d(TAG, "no networks found");
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
				e.printStackTrace();
			}
			return null;
		}

	}

	private BroadcastReceiver messageSendReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			boolean broadcast = intent.getBooleanExtra("broadcast", true);

			Log.d(TAG, "Localbroadcastreceiver got called...");
			// Get extra data included in the Intent
			Message msg = (Message) intent.getSerializableExtra("message");

			if (broadcast) {
				Log.d(TAG, "sending Broadcast message");
				new BroadcastMessageAsyncTask().execute(msg);
			} else {
				Log.d(TAG, "sending Unicast message");
				new UnicastMessageAsyncTask(intent.getStringExtra("ipAddress"))
						.execute(msg);
			}
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
					if (!ni.isLoopback()) {
						for (InterfaceAddress interfaceAddress : ni
								.getInterfaceAddresses()) {

							found_bcast_address = interfaceAddress
									.getBroadcast();
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

	private void joinNetwork(String identification) {

		Message joinMessage = new Message(identification, Message.MSG_MSGJOIN,
				identification, dbHelper.getNextSequenceNumber());
		new BroadcastMessageAsyncTask().execute(joinMessage);

		Message whoisthereMessage = new Message(identification,
				Message.MSG_WHOISTHERE, identification, -1);
		new BroadcastMessageAsyncTask().execute(whoisthereMessage);
	}

	private void discoverNetworks() {
		Message discoverMessage = new Message(getSharedPreferences(
				"network_preferences", 0).getString("identifier", "N/A"),
				Message.MSG_DISCOVERNETS, getSharedPreferences(
						"network_preferences", 0)
						.getString("identifier", "N/A"));
		new BroadcastMessageAsyncTask().execute(discoverMessage);
	}

	public Set<String> getAvilableNetworks() {
		return availableNetworks;
	}

	private byte[] encrypt(byte[] rawSeed, byte[] clear) {
		Cipher cipher;
		MessageDigest digest;
		byte[] encrypted = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			SecretKeySpec skeySpec = new SecretKeySpec(digest.digest(rawSeed),
					"AES");
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			encrypted = cipher.doFinal(clear);
			Log.d(TAG, "sent Length: " + encrypted.length);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			return null;
		}

		return encrypted;
	}
}

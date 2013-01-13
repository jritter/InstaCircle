/*
 *  UniCrypt Cryptographic Library
 *  Copyright (c) 2013 Berner Fachhochschule, Biel, Switzerland.
 *  All rights reserved.
 *
 *  Distributable under GPL license.
 *  See terms of license at gnu.org.
 *  
 */

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
import java.util.Enumeration;

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

/**
 * This class implements an Android service which runs in the background. It
 * handles the incoming messages and reacts accordingly.
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class NetworkService extends Service {

	private static final String TAG = NetworkService.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private static final Integer[] messagesToSave = { Message.MSG_CONTENT,
			Message.MSG_MSGJOIN, Message.MSG_MSGLEAVE };

	private InetAddress broadcast;

	private NetworkDbHelper dbHelper;

	private UDPBroadcastReceiverThread udpBroadcastReceiverThread;
	private TCPUnicastReceiverThread tcpUnicastReceiverThread;

	private String cipherKey;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		// Unregister the receiver which listens for messages to be sent
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				messageSendReceiver);

		// close the DB connection
		dbHelper.close();
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Initializing the dbHelper in order to get access to the database
		dbHelper = new NetworkDbHelper(this);

		// Create a pending intent which will be invoked after tapping on the
		// Android notification
		Intent notificationIntent = new Intent(this,
				NetworkActiveActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		PendingIntent pendingNotificationIntent = PendingIntent.getActivity(
				this, 0, notificationIntent, notificationIntent.getFlags());

		// Setting up the notification which is being displayed
		Notification.Builder notificationBuilder = new Notification.Builder(
				this);
		notificationBuilder.setContentTitle(getResources().getString(
				R.string.app_name));
		notificationBuilder
				.setContentText("An InstaCircle Chat session is running. Tap to bring in front.");
		notificationBuilder
				.setSmallIcon(R.drawable.glyphicons_244_conversation);
		notificationBuilder.setContentIntent(pendingNotificationIntent);
		notificationBuilder.setOngoing(true);
		Notification notification = notificationBuilder.getNotification();

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(TAG, 1, notification);

		// Reading the cipher key from the preferences file
		cipherKey = getSharedPreferences(PREFS_NAME, 0).getString("password",
				"N/A");

		// Initializing the UDP and the TCP threads and start them
		udpBroadcastReceiverThread = new UDPBroadcastReceiverThread(this,
				cipherKey);
		udpBroadcastReceiverThread.start();

		tcpUnicastReceiverThread = new TCPUnicastReceiverThread(this, cipherKey);
		tcpUnicastReceiverThread.start();

		// Register a broadcastreceiver in order to get notification from the UI
		// when a message should be sent
		LocalBroadcastManager.getInstance(this).registerReceiver(
				messageSendReceiver, new IntentFilter("messageSend"));

		// Unfortunately the broadcast address is not available immediately
		// after the network connection is acutally indicated as ready...
		do {
			broadcast = getBroadcastAddress();
			if (broadcast != null) {
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (broadcast == null);

		// Opening a conversation
		dbHelper.openConversation(getSharedPreferences(PREFS_NAME, 0)
				.getString("password", "N/A"));

		// joining the conversation using the identification in the preferences
		// file
		joinNetwork(getSharedPreferences(PREFS_NAME, 0).getString(
				"identification", "N/A"));

		// start the NetworkActiveActivity
		intent = new Intent(this, NetworkActiveActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		startActivity(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * This method is called from the UDPBroadcastReceiver thread and is
	 * responsible for the actual processing of the incoming broadcast message.
	 * 
	 * @param msg
	 *            the message which should be processed
	 */
	public void processBroadcastMessage(Message msg) {

		String identification = getSharedPreferences(PREFS_NAME, 0).getString(
				"identification", "N/A");

		Intent intent;

		// Use the messagetype to determine what to do with the message
		switch (msg.getMessageType()) {

		case Message.MSG_CONTENT:
			// no special action required, just saving later on...
			break;
		case Message.MSG_MSGJOIN:
			// add the new participant to the participants list
			dbHelper.insertParticipant(msg.getMessage(),
					msg.getSenderIPAddress());

			// notify the UI
			intent = new Intent("participantJoined");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
			break;
		case Message.MSG_MSGLEAVE:
			// decativate the participant
			dbHelper.updateParticipantState(msg.getSender(), 0);

			// Notify the UI, but only if it's not myself who left
			if (!msg.getSender().equals(identification)) {
				intent = new Intent("participantChangedState");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
			}
			break;
		case Message.MSG_RESENDREQ:
			// should be handled as unicast
			break;
		case Message.MSG_RESENDRES:
			// should be handled as unicast
			break;
		case Message.MSG_WHOISTHERE:
			// immediately send a unicast response with my own identification
			// back
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
			// shouldn't happen
			break;
		}

		// handling the conversation relevant messages
		if (Arrays.asList(messagesToSave).contains(msg.getMessageType())) {

			// checking whether the sequence number is the one which we expect
			// from the participant, otherwise request the missing messages
			if (msg.getSequenceNumber() != -1
					&& msg.getSequenceNumber() > dbHelper
							.getCurrentParticipantSequenceNumber(msg
									.getSender()) + 1) {
				// Request missing messages
				Message resendRequestMessage = new Message("",
						Message.MSG_RESENDREQ, getSharedPreferences(PREFS_NAME,
								0).getString("identification", "N/A"), -1);
				new UnicastMessageAsyncTask(msg.getSenderIPAddress())
						.execute(resendRequestMessage);
			} else {
				if (dbHelper != null) {
					// insert the message into the database
					dbHelper.insertMessage(msg);
				}
			}
		}

		if (msg.getMessageType() == Message.MSG_MSGLEAVE
				&& msg.getSender().equals(identification)) {
			// Stop everything if the leave message is coming from myself
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
			// otherwise notify the UI that a new message has been arrived
			intent = new Intent("messageArrived");
			intent.putExtra("message", msg);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}

	}

	/**
	 * This method is called from the TCPUnicastReceiver thread and is
	 * responsible for the actual processing of the incoming unicast message.
	 * 
	 * @param msg
	 *            the message which should be processed
	 */
	public void processUnicastMessage(Message msg) {

		// Use the messagetype to determine what to do with the message
		switch (msg.getMessageType()) {

		case Message.MSG_CONTENT:
			// no special action required, just saving later on...
			break;

		case Message.MSG_RESENDREQ:
			// as soon as a resend request arrives we query the messages, stuff
			// them into an array and send the result back
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
					Message.MSG_RESENDRES, identification, -1);

			new UnicastMessageAsyncTask(msg.getSenderIPAddress())
					.execute(resendMessage);
			break;

		case Message.MSG_RESENDRES:

			// handling the resend response
			try {
				// deserializing the content into an ArrayList containing
				// messages
				ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream(Base64.decode(
								msg.getMessage(), Base64.DEFAULT)));
				ArrayList<Message> deserializedMessages = (ArrayList<Message>) ois
						.readObject();
				// iterate over the list and hanling them as if they had just
				// arrived as broadcasts
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

			// Check if the received sequence number is bigger than the one we
			// have in our db, request messages if true
			if (Integer.parseInt(msg.getMessage()) > dbHelper
					.getCurrentParticipantSequenceNumber(msg.getSender())) {
				// Request missing messages
				Log.d(TAG, "Messagelist from " + msg.getSender()
						+ " incomplete, requesting messages...");
				Message resendRequestMessage = new Message("",
						Message.MSG_RESENDREQ, getSharedPreferences(PREFS_NAME,
								0).getString("identification", "N/A"), -1);
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

		// notify the UI that new message has been arrived
		Intent intent = new Intent("messageArrived");
		intent.putExtra("message", msg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	/**
	 * AsyncTask which encapsulates the sending a broadcast message over the UDP
	 * channel into a separate thread
	 * 
	 * @author Juerg Ritter (rittj1@bfh.ch)
	 */
	private class BroadcastMessageAsyncTask extends
			AsyncTask<Message, Integer, Integer> {

		private DatagramSocket s;

		protected Integer doInBackground(Message... msg) {

			if (broadcast == null) {
				broadcast = getBroadcastAddress();
			}

			try {
				// serializing the message
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);
				out.writeObject(msg[0]);
				byte[] bytes = bos.toByteArray();

				// encrypt the message
				byte[] encryptedBytes = encrypt(cipherKey.getBytes(), bytes);

				// creating a byte array of 4 bytes to put the lenght of the
				// payload
				ByteBuffer b = ByteBuffer.allocate(4);
				b.putInt(encryptedBytes.length);
				byte[] length = b.array();

				byte[] bytesToSend = new byte[length.length
						+ encryptedBytes.length];

				// contatenate the length and the payload
				System.arraycopy(length, 0, bytesToSend, 0, length.length);
				System.arraycopy(encryptedBytes, 0, bytesToSend, length.length,
						encryptedBytes.length);

				// setting up the datagram and sending it
				s = new DatagramSocket();
				s.setBroadcast(true);
				s.setReuseAddress(true);
				DatagramPacket p = new DatagramPacket(bytesToSend,
						bytesToSend.length, broadcast, 12345);
				s.send(p);
				s.close();
			} catch (IOException e) {
				return null;
			}

			return 0;
		}
	}

	/**
	 * AsyncTask which encapsulates the sending a unicast message over the TCP
	 * channel into a separate thread
	 * 
	 * @author Juerg Ritter (rittj1@bfh.ch)
	 */
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

			try {
				// serializing the message
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);
				out.writeObject(msg[0]);

				byte[] bytes = bos.toByteArray();

				// encrypt the message
				byte[] encryptedBytes = encrypt(cipherKey.getBytes(), bytes);

				s = new Socket(destinationAddr, 12345);
				out.close();

				// creating a byte array of 4 bytes to put the lenght of the
				// payload
				ByteBuffer b = ByteBuffer.allocate(4);
				b.putInt(encryptedBytes.length);
				byte[] length = b.array();

				byte[] bytesToSend = new byte[length.length
						+ encryptedBytes.length];

				// contatenate the length and the payload
				System.arraycopy(length, 0, bytesToSend, 0, length.length);
				System.arraycopy(encryptedBytes, 0, bytesToSend, length.length,
						encryptedBytes.length);

				// send the data
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

	/**
	 * Implementation of a BroadcastReceiver in order to receive the
	 * notification that the UI wants to send a message
	 */
	private BroadcastReceiver messageSendReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// extract the broadcast flag from the intent
			boolean broadcast = intent.getBooleanExtra("broadcast", true);

			// extract the message from the intent
			Message msg = (Message) intent.getSerializableExtra("message");

			if (broadcast) {
				// sending as broadcast
				new BroadcastMessageAsyncTask().execute(msg);
			} else {
				// sending as unicast
				new UnicastMessageAsyncTask(intent.getStringExtra("ipAddress"))
						.execute(msg);
			}
		}
	};

	/**
	 * Method to extract the broadcastaddress of the current network
	 * configuration
	 * 
	 * @return The broadcast address
	 */
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

	/**
	 * Helper method which assembles and send a join message and a whoisthere
	 * message right afterwards
	 * 
	 * @param identification
	 */
	private void joinNetwork(String identification) {

		Message joinMessage = new Message(identification, Message.MSG_MSGJOIN,
				identification, dbHelper.getNextSequenceNumber());
		new BroadcastMessageAsyncTask().execute(joinMessage);

		Message whoisthereMessage = new Message(identification,
				Message.MSG_WHOISTHERE, identification, -1);
		new BroadcastMessageAsyncTask().execute(whoisthereMessage);
	}

	/**
	 * Method which de data using a key
	 * 
	 * @param rawSeed
	 *            The symetric key as byte array
	 * @param clear
	 *            The data which should be encrypted
	 * @return The encrypted bytes
	 */
	private byte[] encrypt(byte[] rawSeed, byte[] clear) {
		Cipher cipher;
		MessageDigest digest;
		byte[] encrypted = null;
		try {
			// we need a 256 bit key, let's use a SHA-256 hash of the rawSeed
			// for that
			digest = MessageDigest.getInstance("SHA-256");
			digest.reset();

			SecretKeySpec skeySpec = new SecretKeySpec(digest.digest(rawSeed),
					"AES");
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			encrypted = cipher.doFinal(clear);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (NoSuchPaddingException e) {
			return null;
		} catch (InvalidKeyException e) {
			return null;
		} catch (IllegalBlockSizeException e) {
			return null;
		} catch (BadPaddingException e) {
			return null;
		}

		return encrypted;
	}
}

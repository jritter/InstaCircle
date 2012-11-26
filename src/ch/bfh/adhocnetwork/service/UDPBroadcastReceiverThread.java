package ch.bfh.adhocnetwork.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;
import ch.bfh.adhocnetwork.Message;

public class UDPBroadcastReceiverThread implements Runnable { 
	
	private static final String TAG = UDPBroadcastReceiverThread.class.getSimpleName();
	
	NetworkService service;
	
	public UDPBroadcastReceiverThread(NetworkService service) {
		this.service = service;
	}

	public void run() {
		DatagramSocket socket;
		Message msg;
		try {
			socket = new DatagramSocket(12345);
			socket.setBroadcast(true);
			while (true && !Thread.currentThread().isInterrupted()) {

				DatagramPacket packet = new DatagramPacket(new byte[1040], 1040);
				socket.receive(packet);


//				InetAddress address = packet.getAddress();
//				int port = packet.getPort();
//				int len = packet.getLength();
				byte[] encryptedData = packet.getData();
				Log.d(TAG, "LENGTH: " + encryptedData.length);
				
				byte[] data = decrypt("1234".getBytes(), encryptedData);
				
				
				ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInput oin = null;
				try {
				  oin = new ObjectInputStream(bis);
				  msg = (Message) oin.readObject(); 

				} finally {
				  bis.close();
				  oin.close();
				}
				
				
				msg.setSenderIPAddress((packet.getAddress()).getHostAddress());
				service.processBroadcastMessage(msg);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		}
	}
	
	private byte[] decrypt(byte[] rawSeed, byte[] encrypted) {
		Cipher cipher;
		MessageDigest digest;
		byte[] decrypted = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			SecretKeySpec skeySpec = new SecretKeySpec(digest.digest(rawSeed), "AES");
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			decrypted = cipher.doFinal(encrypted);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return decrypted;
	}
}

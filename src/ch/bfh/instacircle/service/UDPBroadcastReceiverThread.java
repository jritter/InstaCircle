package ch.bfh.instacircle.service;

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
import ch.bfh.instacircle.Message;

public class UDPBroadcastReceiverThread extends Thread { 
	
	private static final String TAG = UDPBroadcastReceiverThread.class.getSimpleName();
	public DatagramSocket socket;
	
	NetworkService service;
	
	private String cipherKey;
	
	public UDPBroadcastReceiverThread(NetworkService service, String cipherKey) {
		this.setName(TAG);
		this.service = service;
		this.cipherKey = cipherKey;
	}

	public void run() {
		Message msg;
		try {
			socket = new DatagramSocket(12345);
			socket.setBroadcast(true);
			while (!Thread.currentThread().isInterrupted()) {

				try {
					DatagramPacket packet = new DatagramPacket(new byte[1040], 1040);
					socket.receive(packet);
	
	
	//				InetAddress address = packet.getAddress();
	//				int port = packet.getPort();
	//				int len = packet.getLength();
					byte[] encryptedData = packet.getData();
					Log.d(TAG, "LENGTH: " + encryptedData.length);
					
					byte[] data = decrypt(cipherKey.getBytes(), encryptedData);
					
					if (data != null){
					
						ByteArrayInputStream bis = new ByteArrayInputStream(data);
						ObjectInput oin = null;
						try {
						  oin = new ObjectInputStream(bis);
						  msg = (Message) oin.readObject(); 
		
						} finally {
						  bis.close();
						  oin.close();
						}
						
						if (!Thread.currentThread().isInterrupted()){
							msg.setSenderIPAddress((packet.getAddress()).getHostAddress());
							service.processBroadcastMessage(msg);
						}
					}
					
				}
				catch (IOException e){
					Log.d(TAG, "Terminating...");
					socket.close();
					Thread.currentThread().interrupt();
				}
				
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
			return null;
		}
		return decrypted;
	}
}

package ch.bfh.instacircle.service;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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

public class TCPUnicastReceiverThread extends Thread {

	private static final String TAG = TCPUnicastReceiverThread.class
			.getSimpleName();
	public ServerSocket serverSocket;

	NetworkService service;

	private String cipherKey;

	public TCPUnicastReceiverThread(NetworkService service, String cipherKey) {
		this.setName(TAG);
		this.service = service;
		this.cipherKey = cipherKey;
	}

	public void run() {
		Socket clientSocket;
		Message msg;
		InputStream in = null;
		try {
			serverSocket = new ServerSocket(12345);
			while (!Thread.currentThread().isInterrupted()) {

				try {
					clientSocket = serverSocket.accept();
					in = clientSocket.getInputStream();
					DataInputStream dis = new DataInputStream(in);

					// read the first 4 bytes to determine the length of the
					// inputstream
					byte[] lenght = new byte[4];
					dis.read(lenght);

					// initialise and read an array with the previously
					// determined length
					byte[] encryptedData = new byte[ByteBuffer.wrap(lenght)
							.getInt()];
					dis.readFully(encryptedData);

					Log.d(TAG, "LENGTH: " + encryptedData.length);

					byte[] data = decrypt(cipherKey.getBytes(), encryptedData);
					
					if (data != null) {

						ByteArrayInputStream bis = new ByteArrayInputStream(data);
						ObjectInput oin = null;
						try {
							oin = new ObjectInputStream(bis);
							msg = (Message) oin.readObject();
	
						} finally {
							bis.close();
							oin.close();
						}
	
						if (!Thread.currentThread().isInterrupted()) {
							msg.setSenderIPAddress((clientSocket.getInetAddress())
									.getHostAddress());
		
							service.processUnicastMessage(msg);
						}
					
					}
				} catch (IOException e) {
					Log.d(TAG, "Terminating...");
					serverSocket.close();
					Thread.currentThread().interrupt();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private byte[] decrypt(byte[] rawSeed, byte[] encrypted) {
		Cipher cipher;
		MessageDigest digest;
		byte[] decrypted = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			SecretKeySpec skeySpec = new SecretKeySpec(digest.digest(rawSeed),
					"AES");
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			decrypted = cipher.doFinal(encrypted);
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
		return decrypted;
	}
}

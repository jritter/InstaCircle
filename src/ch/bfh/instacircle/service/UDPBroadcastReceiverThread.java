package ch.bfh.instacircle.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

public class UDPBroadcastReceiverThread extends Thread {

	private static final String TAG = UDPBroadcastReceiverThread.class
			.getSimpleName();
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

					DatagramPacket datagram = new DatagramPacket(
							new byte[socket.getReceiveBufferSize()],
							socket.getReceiveBufferSize());
					socket.receive(datagram);
					Log.d(TAG, "Message received...");
					byte[] data = datagram.getData();

					byte[] length = new byte[4];
					System.arraycopy(data, 0, length, 0, length.length);

					byte[] encryptedData = new byte[ByteBuffer.wrap(length)
							.getInt()];

					System.arraycopy(data, length.length, encryptedData, 0,
							encryptedData.length);

					byte[] cleardata = decrypt(cipherKey.getBytes(),
							encryptedData);

					if (cleardata != null) {

						ByteArrayInputStream bis = new ByteArrayInputStream(
								cleardata);
						ObjectInput oin = null;
						try {
							oin = new ObjectInputStream(bis);
							msg = (Message) oin.readObject();

						} finally {
							bis.close();
							oin.close();
						}

						if (!Thread.currentThread().isInterrupted()) {
							msg.setSenderIPAddress((datagram.getAddress())
									.getHostAddress());
							service.processBroadcastMessage(msg);
						}
					}

				} catch (IOException e) {
					Log.d(TAG, "Terminating...");
					socket.close();
					Thread.currentThread().interrupt();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
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

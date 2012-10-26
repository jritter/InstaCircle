package ch.bfh.adhocnetwork.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import ch.bfh.adhocnetwork.Message;

public class UDPBroadcastReceiverThread implements Runnable { 
	
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
			while (true) {

				DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
				socket.receive(packet);


//				InetAddress address = packet.getAddress();
//				int port = packet.getPort();
//				int len = packet.getLength();
				byte[] data = packet.getData();
				
				
				ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInput in = null;
				try {
				  in = new ObjectInputStream(bis);
				  msg = (Message) in.readObject(); 

				} finally {
				  bis.close();
				  in.close();
				}
				
				service.processMessage(msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		}
	}
}

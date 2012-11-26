package ch.bfh.adhocnetwork;

import java.io.Serializable;

public class Message implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int MSG_CONTENT 		= 1;
	public static final int MSG_MSGJOIN 		= 2;
	public static final int MSG_MSGLEAVE 		= 3;
	public static final int MSG_MSGRESENDREQ 	= 4;
	public static final int MSG_MSGRESENDRES 	= 5;
	public static final int MSG_DISCOVERNETS	= 6;
	public static final int MSG_NETWORKAD		= 7;
	
	
	private String message;
	private int sequenceNumber = -1;
	private String sender;
	private int messageType;
	private String networkUUID;
	private String senderIPAddress;

	public Message(String message, int messageType, String sender){
		this.message = message;
		this.messageType = messageType;
		this.sender = sender;
		
	}
	
	public Message(String message, int messageType, String sender, int sequenceNumber, String networkUUID){
		this.message = message;
		this.messageType = messageType;
		this.sender = sender;
		this.networkUUID = networkUUID;
		this.sequenceNumber = sequenceNumber;
	}

	public String getMessage() {
		return message;
	}
	
	public void setSequenceNumber(int sequenceNumber){
		this.sequenceNumber = sequenceNumber;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public String getSender() {
		return sender;
	}

	public int getMessageType() {
		return messageType;
	}
	
	public String getNetworkUUID() {
		return networkUUID;
	}
	
	public String getSenderIPAddress() {
		return senderIPAddress;
	}
	
	public void setSenderIPAddress(String senderIPAddress){
		this.senderIPAddress = senderIPAddress;
	}
	

	@Override
	public String toString() {
		return "BroadcastMessage [message=" + message + ", sequenceNumber="
				+ sequenceNumber + ", sender=" + sender + ", messageType="
				+ messageType + ", networkUUID=" + networkUUID
				+ ", senderIPAddress=" + senderIPAddress + "]";
	}

}

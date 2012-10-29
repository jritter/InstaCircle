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
	private int sequenceNumber;
	private String sender;
	private int messageType;
	private String networkUUID;

	public Message(String message, int sequenceNumber, int messageType){
		this.message = message;
		this.sequenceNumber = sequenceNumber;
		this.messageType = messageType;
	}
	
	public Message(String message, int sequenceNumber, int messageType, String networkUUID){
		this.message = message;
		this.sequenceNumber = sequenceNumber;
		this.messageType = messageType;
		this.networkUUID = networkUUID;
	}

	public String getMessage() {
		return message;
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

	@Override
	public String toString() {
		return "Message [message=" + message + ", sequenceNumber="
				+ sequenceNumber + ", sender=" + sender + ", messageType="
				+ messageType + "]";
	}

}

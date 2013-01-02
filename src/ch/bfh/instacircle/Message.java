package ch.bfh.instacircle;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int MSG_CONTENT = 1;
	public static final int MSG_MSGJOIN = 2;
	public static final int MSG_MSGLEAVE = 3;
	public static final int MSG_MSGRESENDREQ = 4;
	public static final int MSG_MSGRESENDRES = 5;
	public static final int MSG_DISCOVERNETS = 6;
	public static final int MSG_NETWORKAD = 7;
	public static final int MSG_WHOISTHERE = 8;
	public static final int MSG_IAMHERE = 9;

	private String message;
	private int sequenceNumber = -1;
	private String sender;
	private int messageType;
	private String senderIPAddress;
	private long timestamp;

	public Message(String message, int messageType, String sender) {
		this.message = message;
		this.messageType = messageType;
		this.sender = sender;
	}

	public Message(String message, int messageType, String sender,
			int sequenceNumber) {
		this.message = message;
		this.messageType = messageType;
		this.sender = sender;
		this.sequenceNumber = sequenceNumber;
		this.timestamp = System.currentTimeMillis();
	}

	public String getMessage() {
		return message;
	}

	public void setSequenceNumber(int sequenceNumber) {
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

	public String getSenderIPAddress() {
		return senderIPAddress;
	}

	public void setSenderIPAddress(String senderIPAddress) {
		this.senderIPAddress = senderIPAddress;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "BroadcastMessage [message=" + message + ", sequenceNumber="
				+ sequenceNumber + ", sender=" + sender + ", messageType="
				+ messageType + ", senderIPAddress=" + senderIPAddress + "]";
	}

}

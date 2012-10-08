package ch.bfh.adhocnetwork;

import java.io.Serializable;

public class Message implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int MSG_CONTENT 		= 0;
	public static final int MSG_MSGJOIN 		= 1;
	public static final int MSG_MSGLEAVE 		= 2;
	public static final int MSG_MSGRESENDREQ 	= 3;
	public static final int MSG_MSGRESENDRES 	= 4;
	
	private String message;
	private int sequenceNumber;
	private String sender;
	private int messageType;

	public Message(String message, int sequenceNumber, int messageType){
		this.message = message;
		this.sequenceNumber = sequenceNumber;
		this.messageType = messageType;
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

	@Override
	public String toString() {
		return "Message [message=" + message + ", sequenceNumber="
				+ sequenceNumber + ", sender=" + sender + ", messageType="
				+ messageType + "]";
	}

}

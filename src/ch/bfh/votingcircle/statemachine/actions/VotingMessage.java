package ch.bfh.votingcircle.statemachine.actions;

import ch.bfh.instacircle.Message;

/**
 * Messages types of messages exchange over the network
 * @author Philémon von Bergen
 *
 */
public class VotingMessage extends Message {
	
	private static final long serialVersionUID = 1L;
	
	public static final int MSG_CONTENT_START = 11;
	public static final int MSG_CONTENT_INIT = 12;
	public static final int MSG_CONTENT_SETUP = 13;
	public static final int MSG_CONTENT_COMMIT = 14;
	public static final int MSG_CONTENT_VOTE = 15;
	public static final int MSG_CONTENT_RECOVER = 16;
	public static final int MSG_CONTENT_TALLY = 17;
	
	public static final int RESEND_REQUEST = 2;
	
	

	public VotingMessage(String message, int messageType, String sender) {
		super(message, messageType, sender);
	}

}

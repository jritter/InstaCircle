package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from state recovery to state tally
 * @author Philémon von Bergen
 *
 */
public class AllRecoveringMessagesReceivedEvent extends Event implements Serializable{

	private static final long serialVersionUID = 1L;

	public AllRecoveringMessagesReceivedEvent(Object data) {
		super(data);
	}

}

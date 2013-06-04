package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from state init to state setup
 * @author Philémon von Bergen
 *
 */
public class AllInitMessagesReceivedEvent extends Event implements Serializable {

	private static final long serialVersionUID = 1L;

	public AllInitMessagesReceivedEvent(Object data) {
		super(data);
	}
}


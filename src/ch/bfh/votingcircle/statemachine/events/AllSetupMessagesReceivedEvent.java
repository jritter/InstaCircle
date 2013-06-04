package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from setup init to state commit
 * @author Philémon von Bergen
 *
 */
public class AllSetupMessagesReceivedEvent extends Event implements Serializable{

	private static final long serialVersionUID = 1L;

	public AllSetupMessagesReceivedEvent(Object data) {
		super(data);
	}

}

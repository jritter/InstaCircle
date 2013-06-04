package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from state tally to state recovery
 * @author Philémon von Bergen
 *
 */
public class NotAllMessageReceivedEvent extends Event implements Serializable{

	private static final long serialVersionUID = 1L;

	public NotAllMessageReceivedEvent(Object data) {
		super(data);
	}
}

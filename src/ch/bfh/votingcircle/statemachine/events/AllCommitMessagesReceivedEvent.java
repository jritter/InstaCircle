package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from state commit to state vote
 * @author Philémon von Bergen
 *
 */
public class AllCommitMessagesReceivedEvent extends Event implements Serializable {

	private static final long serialVersionUID = 1L;

	public AllCommitMessagesReceivedEvent(Object data) {
		super(data);
	}
}


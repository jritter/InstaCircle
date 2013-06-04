package ch.bfh.votingcircle.statemachine.actions;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.math.element.interfaces.Element;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllVotingMessagesReceivedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;

import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Vote step
 * This action compute and exchange some values in relation with the vote
 * @author Philémon von Bergen
 * 
 */
public class VotingRoundAction extends AbstractAction {

	public VotingRoundAction(int messageTypeToListenTo, DataManager dm){
		super(messageTypeToListenTo,dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;
		
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		activity.setActivityTitle(R.string.voting_round);
		activity.changeImage(R.id.flow, R.drawable.flow_5);

		logger.debug("VotingRoundAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeaved, intentFilter);

		//compute the values of the protocol
		logger.debug(this.classname +" Computing and sending protocol values");

		//Send b_i
		String biString = this.dm.getSerializationUtil().serialize(this.dm.getMe().getBi());
		this.sendMessage(biString, VotingMessage.MSG_CONTENT_VOTE, null);

		//Notify GUI of participant state change
		this.dm.getMe().setState(1);
		Intent intent = new Intent();
		intent.setAction("participantMessageReceived");
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);

		this.actionRan = true;

		//Wait for receiving values from other participants
		if(this.readyToGoToNextState()){
			this.goToNextState();
		} else {
			startTimer(DataManager.TIME_BEFORE_RESEND_REQUESTS, DataManager.NUMBER_OF_RESEND_REQUESTS);
			logger.debug(this.classname +" Waiting for other participant. Actually received:"
					+ this.messagesReceived.size() +" of "+ this.dm.getProtocolParticipants().size());
		}

	}

	@Override
	protected void processMessage(Message message){
		Participant p = dm.getProtocolParticipants().get(message.getSenderIPAddress());
		
		//p can be null if participant that sent message has been previously excluded
		if(p==null){
			return;
		}
		
		if(!p.equals(this.dm.getMe())){

			//store the value in the corresponding participant 
			p.setBi((Element)this.dm.getSerializationUtil().deserialize(message.getMessage()));
			p.setState(1);

			//Notify GUI of participant state change
			Intent intent = new Intent();
			intent.setAction("participantMessageReceived");
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
		}

		if(this.readyToGoToNextState()){
			this.goToNextState();
		}
	}

	@Override
	protected void goToNextState(){
		
		this.actionTerminated = true;
		this.reset();
		this.resetParticipantsState();
		
		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new AllVotingMessagesReceivedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}


}



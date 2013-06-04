package ch.bfh.votingcircle.statemachine.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllInitMessagesReceivedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Initialization step
 * This action get the candidates, protocol participant and other initialization values
 * @author Philémon von Bergen
 * 
 */
public class InitAction extends AbstractAction {

	/**
	 * Create the object
	 * @param messageTypeToListenTo the type of message to listen to at this step
	 */
	public InitAction(int messageTypeToListenTo, DataManager dm) {
		super(messageTypeToListenTo,dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;
		
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		
		activity.setActivityTitle(R.string.init_action);
		activity.changeImage(R.id.flow, R.drawable.flow_2);

		logger.debug("InitAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeaved, intentFilter);

		//If the initiator of the voting process is me
		logger.error("initiator is "+this.dm.getInitiator());
		logger.error("datamanger is "+dm);
		//TODO if this error doesn't appear anymore, do the .equals on myIpAddress
		if (this.dm.getInitiator().equals(this.dm.getMyIpAddress())) {
			logger.debug(this.classname +" Generating and sending generator");

			//Determine the generator and send it to others
			AtomicElement g = this.dm.getG_q().createRandomGenerator();
			this.dm.setGenerator(g);

			String gString = dm.getSerializationUtil().serialize(g);
			//Serialize the participants map
			String participantsString = dm.getSerializationUtil().serialize(this.dm.getProtocolParticipants());
			//Serialize the candidate list
			String candidatesString = dm.getSerializationUtil().serialize(this.dm.getCandidates());
			//Serialize the question
			String questionString = dm.getSerializationUtil().serialize(this.dm.getQuestion());
			this.sendMessage(gString+"|"+participantsString+"|"+candidatesString+"|"+questionString,VotingMessage.MSG_CONTENT_INIT, null);
			
			activity.setTextInView(R.id.ev_connected, R.string.ev_participants);

		}

		this.actionRan = true;

		//Wait for receiving values from other participants
		if(this.readyToGoToNextState()){
			this.goToNextState();
		} else {
			logger.debug(this.classname +" Waiting for init values");
			startTimer(DataManager.TIME_BEFORE_RESEND_REQUESTS, Integer.MAX_VALUE);
		}
	}

	@Override
	protected void processMessage(Message message){
		if(this.dm.getInitiator()==null || !this.dm.getInitiator().equals(this.dm.getMyIpAddress())){
			logger.debug(this.classname+"Processing entering message");
			//message received from initiator
			Message m = this.messagesReceived.get(this.dm.getInitiator());
			if(m==null){
				logger.debug(classname+" Message received but was not from initiator!");
				return;
			}
			StringTokenizer tokenizer = new java.util.StringTokenizer(m.getMessage(), "|");

			//if message is malformed
			if(tokenizer.countTokens()!=4){
				this.messagesReceived.remove(message.getSenderIPAddress());
				logger.warn(this.classname+" Init message was not composed of four parts");
				//start timer and return;
				startTimer(5000, Integer.MAX_VALUE);
				return;
			}
			else{
				String gString = tokenizer.nextToken();
				String participantsString = tokenizer.nextToken();
				String candidatesString = tokenizer.nextToken();
				String questionString = tokenizer.nextToken();

				AtomicElement g = (AtomicElement)this.dm.getSerializationUtil().deserialize(gString);
				this.dm.setGenerator(g);

				@SuppressWarnings("unchecked")
				HashMap<String,Participant> map = (HashMap<String,Participant>)this.dm.getSerializationUtil().deserialize(participantsString);
				this.dm.setProtocolParticipants(map);

				@SuppressWarnings("unchecked")
				List<Candidate> candidates = (ArrayList<Candidate>) this.dm.getSerializationUtil().deserialize(candidatesString);
				this.dm.setCandidates(candidates);

				String question = (String) this.dm.getSerializationUtil().deserialize(questionString);
				this.dm.setQuestion(question);

				//Notify GUI of participant state change
				Intent intent = new Intent();
				intent.setAction("participantMessageReceived");
				LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);

				this.dm.getEvotingMainActivity().setTextInView(R.id.ev_connected, R.string.ev_participants);
				
				// if I was not in the received participants, reset my state
				if(this.dm.getMe()==null){
					this.dm.getEvotingMainActivity().dismissProgressDialog();
					AlertDialog.Builder builder1 = new AlertDialog.Builder(this.dm.getEvotingMainActivity());
					builder1.setTitle("Error");
					builder1.setMessage("Initiator didn't include you in the vote. Please leave the discussion");
					builder1.setCancelable(true);
					builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert1 = builder1.create();
					alert1.show();
					this.actionRan = false;
					this.dm.setProtocolStarted(false);
					this.stopTimer();
					this.actionTerminated = true;
					// Unregister
					LocalBroadcastManager.getInstance(this.dm.getContext()).unregisterReceiver(this.mMessageReceiver);
					LocalBroadcastManager.getInstance(this.dm.getContext()).unregisterReceiver(this.participantsLeaved);
				}
			}
		}

		
		if(this.readyToGoToNextState()){
			this.goToNextState();
		}
	}

	@Override
	protected boolean readyToGoToNextState(){
		if(!this.actionRan) return false;
		if(this.messagesReceived.containsKey(this.dm.getInitiator())){
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void goToNextState(){
		
		this.reset();
		
		this.actionTerminated = true;
		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new AllInitMessagesReceivedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}

}

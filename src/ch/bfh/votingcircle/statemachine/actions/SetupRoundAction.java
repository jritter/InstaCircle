package ch.bfh.votingcircle.statemachine.actions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.element.interfaces.Element;
import ch.bfh.unicrypt.math.element.interfaces.TupleElement;
import ch.bfh.unicrypt.math.function.classes.CompositeFunctionClass;
import ch.bfh.unicrypt.math.function.classes.MultiIdentityFunctionClass;
import ch.bfh.unicrypt.math.function.classes.PartiallyAppliedFunctionClass;
import ch.bfh.unicrypt.math.function.classes.SelfApplyFunctionClass;
import ch.bfh.unicrypt.math.function.interfaces.Function;
import ch.bfh.unicrypt.math.group.classes.ProductGroupClass;
import ch.bfh.unicrypt.math.group.classes.ZPlusClass;
import ch.bfh.unicrypt.nizkp.classes.SigmaProofGeneratorClass;
import ch.bfh.unicrypt.nizkp.interfaces.SigmaProofGenerator;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllSetupMessagesReceivedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;

import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Setup step
 * This action compute setup value for the protocol
 * @author Philémon von Bergen
 * 
 */
public class SetupRoundAction extends AbstractAction {

	//Stores message that can be treated immediately
	private List<Message> savedMessages = new ArrayList<Message>();

	public SetupRoundAction(int messageTypeToListenTo, DataManager dm) {
		super(messageTypeToListenTo,dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;
		
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();

		activity.setActivityTitle(R.string.setup_round);
		activity.changeImage(R.id.flow, R.drawable.flow_3);

		logger.debug("SetupRoundAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeaved, intentFilter);

		//compute the values of the protocol
		logger.debug(this.classname +" Computing and sending protocol values");

		//compute the secret x_i and a_i=g^(x_i)

		AtomicElement xi = this.dm.getZ_q().createRandomElement(this.dm.getRandom());
		AtomicElement ai = this.dm.getGenerator().selfApply(xi);

		//Computation of the proof

		//Function g^r
		Function f = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
				new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), this.dm.getGenerator(), 0));
		SigmaProofGenerator spg = new SigmaProofGeneratorClass(f);

		//Generator and index of the participant has also to be hashed in the proof
		Element i = ZPlusClass.getInstance().createElement(BigInteger.valueOf(
				this.dm.getMe().getProtocolParticipantIndex()));
		TupleElement otherInput = ProductGroupClass.createTupleElement(this.dm.getGenerator(), i);

		Element ts = spg.generate(xi, ai, otherInput, this.dm.getRandom());

		//store the value in the participant corresponding to me
		Participant me = this.dm.getProtocolParticipants().get(dm.getMyIpAddress());
		me.setXi(xi);
		me.setAi(ai);
		me.setProofForXi(ts);

		//Send a_i and the proof to other participants
		String aiString = this.dm.getSerializationUtil().serialize(ai);
		String tsString = this.dm.getSerializationUtil().serialize(ts);

		//Notify GUI of participant state change
		me.setState(1);
		Intent intent = new Intent();
		intent.setAction("participantMessageReceived");
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);

		this.sendMessage(aiString+"|"+tsString,VotingMessage.MSG_CONTENT_SETUP, null);

		//Process stored message that couldn't be processed before
		if(!this.savedMessages.isEmpty()){
			for(Message m: this.savedMessages){
				this.processMessage(m);
			}
		}

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

		//if a message comes in before init action was terminated, participant are unknown, so the
		//message cannot be processed at this time. So we store it and we process it in the doAction method
		if(dm.getProtocolParticipants()==null){
			this.savedMessages.add(message);
			return;
		}

		Participant p = dm.getProtocolParticipants().get(message.getSenderIPAddress());

		if(!p.equals(this.dm.getMe())){

			//Store value received in the corresponding participant object
			StringTokenizer tokenizer = new java.util.StringTokenizer(message.getMessage(), "|");

			//if message is malformed
			if(tokenizer.countTokens()!=2){
				this.messagesReceived.remove(message.getSenderIPAddress());
				logger.warn(this.classname+" Setup message was not composed of two parts");
				//start timer and return;
				startTimer(DataManager.TIME_BEFORE_RESEND_REQUESTS, DataManager.NUMBER_OF_RESEND_REQUESTS);
				return;
			}
			else{
				String aiString = tokenizer.nextToken();
				String tsString = tokenizer.nextToken();

				Element ai = (Element)this.dm.getSerializationUtil().deserialize(aiString);
				Element ts = (Element)this.dm.getSerializationUtil().deserialize(tsString);

				//store the value in the corresponding participant
				p.setAi(ai);
				p.setProofForXi(ts);
				p.setState(1);

				//Notify GUI of participant state change
				Intent intent = new Intent();
				intent.setAction("participantMessageReceived");
				LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
			}
		}

		if(this.readyToGoToNextState()){
			this.goToNextState();
		}
	}

	@Override
	protected void goToNextState(){
		this.reset();
		this.resetParticipantsState();

		this.actionTerminated = true;

		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new AllSetupMessagesReceivedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}
}

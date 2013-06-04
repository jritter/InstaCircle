package ch.bfh.votingcircle.statemachine.actions;


import java.math.BigInteger;
import java.util.StringTokenizer;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.element.interfaces.Element;
import ch.bfh.unicrypt.math.function.classes.CompositeFunctionClass;
import ch.bfh.unicrypt.math.function.classes.MultiIdentityFunctionClass;
import ch.bfh.unicrypt.math.function.classes.PartiallyAppliedFunctionClass;
import ch.bfh.unicrypt.math.function.classes.SelfApplyFunctionClass;
import ch.bfh.unicrypt.math.function.interfaces.Function;
import ch.bfh.unicrypt.math.group.classes.ProductGroupClass;
import ch.bfh.unicrypt.nizkp.classes.SigmaEqualityProofGeneratorClass;
import ch.bfh.unicrypt.nizkp.interfaces.SigmaEqualityProofGenerator;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllRecoveringMessagesReceivedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;


/**
 * Action executed in Recovery step
 * This action exchange messages to recover values for excluded participants
 * @author Philémon von Bergen
 * 
 */
public class RecoveryRoundAction extends AbstractAction {

	public RecoveryRoundAction(int messageTypeToListenTo, DataManager dm){
		super(messageTypeToListenTo,dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;
		
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		activity.setActivityTitle(R.string.recovery_round);
		activity.changeImage(R.id.flow, R.drawable.flow_6);

		logger.debug("RecoveryRoundAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeft, intentFilter);

		//compute the values of the protocol
		logger.debug(this.classname +" Computing and sending protocol values");

		Element productNumerator = this.dm.getG_q().createElement(BigInteger.valueOf(1));
		Element productDenominator = this.dm.getG_q().createElement(BigInteger.valueOf(1));
		for(Participant p: this.dm.getExcludedParticipants()){
			if(p.getAi()==null){
				//participant was excluded before start of crypto protocol
				//so we do not need to compute a recovery value for him
				continue;
			}
			if(p.getProtocolParticipantIndex()>this.dm.getMe().getProtocolParticipantIndex()){
				productNumerator = productNumerator.apply(p.getAi());
			} else if (p.getProtocolParticipantIndex()<this.dm.getMe().getProtocolParticipantIndex()){
				productDenominator = productDenominator.apply(p.getAi());
			}
		}

		Element hiHat = productNumerator.apply(productDenominator.invert());
		this.dm.getMe().setHiHat(hiHat);

		Element hiHatPowXi = hiHat.selfApply(((AtomicElement)this.dm.getMe().getXi()).getBigInteger());
		this.dm.getMe().setHiHatPowXi(hiHatPowXi);

		//Computation of the proof 

		//Function g^r
		Function f1 = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
				new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), this.dm.getGenerator(), 0));

		//Function h_hat^r
		Function f2 = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
				new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), this.dm.getMe().getHiHat(), 0));

		SigmaEqualityProofGenerator sepg = new SigmaEqualityProofGeneratorClass(f1,f2);
		Element publicInput = ProductGroupClass.createTupleElement(this.dm.getMe().getAi(), this.dm.getMe().getHiHatPowXi());

		Element proof = sepg.generate(this.dm.getMe().getXi(), publicInput, null, this.dm.getRandom());
		this.dm.getMe().setProofForHiHat(proof);

		//Send the values to other participants
		String hiHatString = this.dm.getSerializationUtil().serialize(hiHat);
		String hiHatPowXiString = this.dm.getSerializationUtil().serialize(hiHatPowXi);
		String proofString = this.dm.getSerializationUtil().serialize(proof);
		this.sendMessage(hiHatString+"|"+hiHatPowXiString+"|"+proofString, VotingMessage.MSG_CONTENT_RECOVER, null);

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
			//Store value received in the corresponding participant object
			StringTokenizer tokenizer = new java.util.StringTokenizer(message.getMessage(), "|");

			//if message is malformed
			if(tokenizer.countTokens()!=3){
				this.messagesReceived.remove(message.getSenderIPAddress());
				logger.warn(this.classname+" Recovery message was not composed of two parts");
				//start timer and return;
				startTimer(5000, DataManager.NUMBER_OF_RESEND_REQUESTS);
				return;
			}
			else{
				String hiHatString = tokenizer.nextToken();
				String hiHatPowXiString = tokenizer.nextToken();
				String proofString = tokenizer.nextToken();
				Element hiHat = (Element)this.dm.getSerializationUtil().deserialize(hiHatString);
				Element hiHatPowXi = (Element)this.dm.getSerializationUtil().deserialize(hiHatPowXiString);
				Element proof = (Element)this.dm.getSerializationUtil().deserialize(proofString);
				//store the value in the corresponding participant
				p.setHiHat(hiHat);
				p.setHiHatPowXi(hiHatPowXi);
				p.setProofForHiHat(proof);
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
		this.actionTerminated=true;
		this.reset();
		this.resetParticipantsState();

		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new AllRecoveringMessagesReceivedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}
}



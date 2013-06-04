package ch.bfh.votingcircle.statemachine.actions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.encryption.classes.ElGamalEncryptionClass;
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
import ch.bfh.unicrypt.nizkp.classes.SigmaEqualityProofGeneratorClass;
import ch.bfh.unicrypt.nizkp.classes.SigmaProofGeneratorClass;
import ch.bfh.unicrypt.nizkp.interfaces.SigmaEqualityProofGenerator;
import ch.bfh.unicrypt.nizkp.interfaces.SigmaProofGenerator;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllRecoveringMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.NotAllMessageReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.ResultComputedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;

import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;


/**
 * Action executed in Tally step
 * This action is responsible for verifying the proofs and computing the results
 * @author Philémon von Bergen
 * 
 */
public class TallyingAction extends AbstractAction {

	private boolean recoveryRoundNeeded = false;
	private int[] voteResult = null;

	public TallyingAction(int messageTypeToListenTo, DataManager dm){
		super(messageTypeToListenTo, dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;

		EvotingMainActivity activity = this.dm.getEvotingMainActivity();

		activity.setActivityTitle(R.string.tally);
		activity.changeImage(R.id.flow, R.drawable.flow_7);

		logger.debug("TallyingAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeft, intentFilter);

		//when not apply transition to recovery round
		if(!this.dm.getExcludedParticipants().isEmpty() && !(message instanceof AllRecoveringMessagesReceivedEvent)){
			this.stopTimer();
			this.recoveryRoundNeeded  = true;
			this.actionTerminated = false;
			Intent transitionIntent = new Intent("applyTransition");
			transitionIntent.putExtra("event", new NotAllMessageReceivedEvent(null));
			LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
			return;
		}

		//verify proofs
		for(Participant p:this.dm.getProtocolParticipants().values()){

			logger.debug("Verifying proofs for: "+p.getIdentification());

			//Proof of knowledge of xi

			Function f = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
					new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), this.dm.getGenerator(), 0));
			SigmaProofGenerator spg = new SigmaProofGeneratorClass(f);

			Element i = ZPlusClass.getInstance().createElement(BigInteger.valueOf(
					p.getProtocolParticipantIndex()));
			TupleElement otherInput = ProductGroupClass.createTupleElement(this.dm.getGenerator(), i);

			if(!spg.verify((TupleElement) p.getProofForXi(), p.getAi(), otherInput)){
				AlertDialog.Builder builder1 = new AlertDialog.Builder(this.dm.getEvotingMainActivity());
				builder1.setTitle("Error");
				builder1.setMessage("Knowledge of xi false for participant " + p.getIdentification());
				builder1.setCancelable(true);
				builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert1 = builder1.create();
				alert1.show();
			}
			logger.debug("Proof for xi was correct for participant " + p.getIdentification());


			//verify validity proof

			//possiblePlainTexts are the possible vi
			AtomicElement[] possiblePlainTexts = new AtomicElement[this.dm.getCandidates().size()];
			for(int j=0;j<this.dm.getCandidates().size();j++){
				possiblePlainTexts[j]=(AtomicElement)this.dm.getCandidates().get(j).getRepresentation();
			}

			//possibleVotes are the g^vi
			AtomicElement[] possibleVotes = new AtomicElement[this.dm.getCandidates().size()];
			for(int j=0;j<this.dm.getCandidates().size();j++){
				possibleVotes[j]=this.dm.getGenerator().selfApply(possiblePlainTexts[j]);
			}

			//compute validity proof
			ElGamalEncryptionClass eec = new ElGamalEncryptionClass(this.dm.getG_q(), this.dm.getGenerator());
			//cipher text

			Element ciphertext = ProductGroupClass.createTupleElement(p.getAi(),p.getBi());

			if(!eec.verifyValidityProof((TupleElement)p.getProofValidVote(), ciphertext, p.getHi(), possibleVotes)){
				AlertDialog.Builder builder1 = new AlertDialog.Builder(this.dm.getEvotingMainActivity());
				builder1.setTitle("Error");
				builder1.setMessage("Validity proof is false for participant " + p.getIdentification());
				builder1.setCancelable(true);
				builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert1 = builder1.create();
				alert1.show();
			}
			logger.debug("Validity Proof was correct for participant " + p.getIdentification());
			
			//TODO when a participant has been excluded, result is not found even if combinations are corrects: why?
			//maybe problem of the index if it is  = 2 and participant = 1 has been excluded ?

			//Proof of equality between discrete logs
			if(this.recoveryRoundNeeded){
				//Function g^r
				Function f1 = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
						new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), this.dm.getGenerator(), 0));

				//Function h_hat^r
				Function f2 = new CompositeFunctionClass(new MultiIdentityFunctionClass(this.dm.getZ_q(), 1),
						new PartiallyAppliedFunctionClass(new SelfApplyFunctionClass(this.dm.getG_q(), this.dm.getZ_q()), p.getHiHat(), 0));

				SigmaEqualityProofGenerator sepg = new SigmaEqualityProofGeneratorClass(f1,f2);

				Element publicInput = ProductGroupClass.createTupleElement(p.getAi(), p.getHiHatPowXi());

				if(!sepg.verify((TupleElement) p.getProofForHiHat(), publicInput, null)){
					AlertDialog.Builder builder1 = new AlertDialog.Builder(this.dm.getEvotingMainActivity());
					builder1.setTitle("Error");
					builder1.setMessage("Equality between logs proof is false for participant " + p.getIdentification());
					builder1.setCancelable(true);
					builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert1 = builder1.create();
					alert1.show();
				}
				logger.debug("Proof of Equality between logs was correct for participant " + p.getIdentification());
			}
		}


		//compute result
		Element product = this.dm.getG_q().createElement(BigInteger.valueOf(1));
		for(Participant p: this.dm.getProtocolParticipants().values()){
			if(this.recoveryRoundNeeded){
				product=product.apply(p.getBi()).apply(p.getHiHatPowXi());
			} else {
				product=product.apply(p.getBi());
			}
		}
		
		//try to find combination corresponding to the computed result

		//initialize array containing possible combination
		int[] voteForCandidates = new int[this.dm.getCandidates().size()];
		for(int i=0; i<voteForCandidates.length;i++){
			voteForCandidates[i]=0;
		}		
		
		computePossibleResults(voteForCandidates, this.dm.getProtocolParticipants().size(), this.dm.getProtocolParticipants().size(), 0, product);

		if(voteResult==null){
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this.dm.getEvotingMainActivity());
			builder1.setTitle("Error");
			builder1.setMessage("Result could not be computed");
			builder1.setCancelable(true);
			builder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert1 = builder1.create();
			alert1.show();
		}

		//Set the text
		activity.changeImage(R.id.flow, R.drawable.flow_8);
		this.dm.setResult(createResult(voteResult,this.dm.getCandidates()));


		for(Participant p:this.dm.getProtocolParticipants().values()){
			p.setState(1);
		}
		//Notify GUI of participant state change
		Intent intent = new Intent();
		intent.setAction("participantMessageReceived");
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);

		this.actionRan = true;

		this.goToNextState();


	}

	@Override
	protected void processMessage(Message message){}

	@Override
	protected boolean readyToGoToNextState(){
		return true;
	}


	@Override
	protected void goToNextState(){
		this.actionTerminated = true;
		this.reset();
		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new ResultComputedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}

	/**
	 * Compute all combination of divide up MAX votes between Array.length candidate 
	 * @param array Array containing the possible combination
	 * @param resting Resting number of vote to divide up
	 * @param max Number of vote to divide up in total
	 * @param idx Index of the column (start with 0)
	 * @param result Obtained result of votes to find
	 */	
	private void computePossibleResults(int[] array, int resting, int max, int idx, Element result) {

		//stop condition for the recursion
		//we have reached the last column
		if (idx == array.length) {
			//if the number of votes attributed < max => not interesting for us
			if(arraySum(array)<max)return;
			logger.debug("Possible combination "+Arrays.toString(array));
			//compare combination and result of tally
			AtomicElement tempResult = this.dm.getZ_q().createElement(BigInteger.ZERO);
			for(int j=0;j<array.length;j++){
				tempResult = tempResult.apply(this.dm.getCandidates().get(j).getRepresentation().selfApply(this.dm.getZ_q().createElement(BigInteger.valueOf(array[j]))));
			}
			if(this.dm.getG_q().areEqualElements(result, this.dm.getGenerator().selfApply(tempResult))){
				this.voteResult=array;
			}
			return;
		}
		//else put a value at the index and call recursion for the other columns
		for (int i = 0; i <= resting; i++) { 
			array[idx] = i;
			computePossibleResults(array, resting-i, max, idx+1, result);
			//if result was already found, we don't try other combinations
			if(this.voteResult!=null) return;
		}
	}

	/**
	 * Make the sum of each element of the array
	 * Warning: this method is sensible to integer overflow if the number a great enough
	 * @param array the array to sum up
	 * @return the sum
	 */
	private int arraySum(int[] array){
		int sum=0;
		for(int i=0;i<array.length;i++){
			sum+=array[i];
		}
		return sum;
	}

	/**
	 * Create a list with the result in the same order as the list of candidates
	 * @param array
	 * @param list
	 * @return
	 */
	private List<Candidate> createResult(int[] array, List<Candidate> list){
		if(array==null){
			return new ArrayList<Candidate>();
		}
		for(int i=0; i<array.length;i++){
			list.get(i).setVotes(array[i]);
		}

		Collections.sort(list, Collections.reverseOrder());

		return list;
	}

}




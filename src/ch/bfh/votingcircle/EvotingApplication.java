package ch.bfh.votingcircle;

import java.math.BigInteger;
import java.util.List;

import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.InstaCircleInterface;
import ch.bfh.votingcircle.entities.ResendManager;
import ch.bfh.votingcircle.entities.StateMachineManager;
import ch.bfh.votingcircle.statemachine.actions.VotingMessage;
import ch.bfh.votingcircle.statemachine.events.StartCycleEvent;
import ch.bfh.votingcircle.util.JavaSerialization;
import ch.bfh.votingcircle.util.SerializationUtil;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Class representing the Evoting application
 * This class initialize some entities used in the application
 * @author Philémon von Bergen
 *
 */
public class EvotingApplication extends Application {


	private DataManager dm;
	@SuppressWarnings("unused")
	private InstaCircleInterface icInterface;
	private StateMachineManager smm;
	private ResendManager rsm;

	@Override
	public void onCreate() {
		super.onCreate();

		new Thread(new EntitiesCreator()).start();
	}


	/**
	 * Returns the data manager
	 * @return
	 */
	public DataManager getDataManager() {
		return dm;
	}

	/**
	 * Method called when starting the protocol phase
	 * It stores the participants and send the the start command
	 */
	public void startProtocol(){

		dm.setProtocolParticipants(dm.getTempParticipants());

		//compute Baudron et al
		AtomicElement two = this.dm.getZ_q().createElement(BigInteger.valueOf(2));
		int m = -1;
		for(int i=0; i<Integer.MAX_VALUE;i++){
			BigInteger pow2i = two.getBigInteger().pow(i);
			//if 2^i > n
			if(pow2i.compareTo(BigInteger.valueOf(this.dm.getProtocolParticipants().size()))==1){
				m=i;
				break;
			}
		}
		//create a "generator" for each candidate
		List<Candidate> candidatesList = this.dm.getCandidates();
		for(int i=0; i<candidatesList.size();i++){
			Candidate c = candidatesList.get(i);
			//compute 2^(i*m)
			c.setRepresentation(this.dm.getZ_q().createElement(two.getBigInteger().pow(i*m)));
		}
		dm.setProtocolStarted(true);

		//send message to start protocol
		Intent intentMessage = new Intent("sendMessage");
		intentMessage.putExtra("type", VotingMessage.MSG_CONTENT_START);
		//intentMessage.putExtra("messageContent", "Start protocol message from "+dm.getMyIdentification());
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentMessage);
	}

	/**
	 * Method to reset the application after a poll
	 */
	public void reset(){
		dm.setActive(false);
		rsm.close();
		smm.reset();
		//Reset the interface
		Intent intentMessage = new Intent("resetInterface");
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentMessage);
		this.onCreate();
	}

	/**
	 * Configure Log4J to also log in LogCat
	 */
	private void initialiseLogging() {
		final LogConfigurator logConfigurator = new LogConfigurator();

		logConfigurator.setFileName(Environment.getExternalStorageDirectory()
				+ "/evotingcircle.log");
		logConfigurator.setRootLevel(DataManager.LEVEL);
		// max 3 rotated log files
		logConfigurator.setMaxBackupSize(3);
		// Max 500ko per file
		logConfigurator.setMaxFileSize(500000);
		logConfigurator.configure();
	}

	private class EntitiesCreator implements Runnable{
		
		@Override
		public void run() {
			dm = new DataManager();
			dm.setSerializationUtil(new SerializationUtil(new JavaSerialization()));
			dm.setContext(EvotingApplication.this.getApplicationContext());
			
			EvotingApplication.this.initialiseLogging();

			icInterface = new InstaCircleInterface(dm);

			smm = new StateMachineManager(dm);
			smm.run();

			//Create the manager that we send the message if a participant has not received it the first time it was sent
			rsm = new ResendManager(dm);

			Intent transitionIntent = new Intent("applyTransition");
			transitionIntent.putExtra("event", new StartCycleEvent(null));
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(transitionIntent);
		}
		
	}

}


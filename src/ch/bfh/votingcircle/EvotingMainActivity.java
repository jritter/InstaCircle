package ch.bfh.votingcircle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ch.bfh.instacircle.Message;
import ch.bfh.votingcircle.ParticipantListAdapter;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.actions.AbstractAction;
import ch.bfh.instacircle.R;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Main Activity display the flow of the protocol
 * @author Philémon von Bergen
 *
 */
public class EvotingMainActivity extends ListActivity {

	private static final int ELECTION_INFO_REQUEST = 1;

	private EvotingMainActivity activity;
	private DataManager dm;

	private Button btnConfig;
	private Button btnStart;
	private ParticipantListAdapter pca;
	private ProgressDialog pd;

	/************************ Android methods ************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_evoting_main);

		activity = this;

		dm = ((EvotingApplication)this.getApplication()).getDataManager();
		//if an exception occured during the creation of the entities, they could be null
		//so recreate them
		if(dm==null){
			((EvotingApplication)this.getApplication()).onCreate();
		}
		//wait until recreation is done
		while(dm==null){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Log.d("EvotingMainActivity","DataManager is null, waiting to recreate a new one.");
			dm = ((EvotingApplication)this.getApplication()).getDataManager();
		};
		dm.setEvotingMainActivity(this);



		btnConfig = (Button) this.findViewById(R.id.config_button);
		btnConfig.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(activity,ElectionInfoActivity.class);
				List<Candidate> candidates = dm.getCandidates();
				if(candidates.size()==0){
					candidates.add(new Candidate("No",null));
					candidates.add(new Candidate("Yes",null));
				}
				intent.putExtra("candidates", (Serializable)candidates);
				intent.putExtra("question", dm.getQuestion());
				startActivityForResult(intent,ELECTION_INFO_REQUEST);
			}

		});

		btnStart = (Button) this.findViewById(R.id.start_button);
		btnStart.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				DataManager.LOGGER.debug("Start called by pressing the start button");
				((EvotingApplication)getApplication()).startProtocol();
			}

		});
		if(dm.getQuestion()==null || dm.getQuestion().equals("") || dm.getCandidates().size()<2) btnStart.setEnabled(false);

		LocalBroadcastManager.getInstance(this).registerReceiver(participantsUpdater, new IntentFilter("participantMessageReceived"));


		// initializing the adapter
		Intent intent = new Intent("countParticipants");
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
		List<Participant> list = new ArrayList<Participant>();
		pca = new ParticipantListAdapter(this.getApplicationContext(),R.layout.list_item_participant,list);
		setListAdapter(pca);

		IntentFilter intentFilter = new IntentFilter();
		//intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

		//LocalBroadcastManager.getInstance(this).registerReceiver(wifiEventReceiver, intentFilter);
		//this.registerReceiver(wifiEventReceiver, intentFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.evoting_main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
		((EvotingApplication)this.getApplication()).reset();

		LocalBroadcastManager.getInstance(dm.getEvotingMainActivity()).unregisterReceiver(participantsUpdater);
		//this.unregisterReceiver(wifiEventReceiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.leave_network:

			// Display a confirm dialog asking whether really to leave
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.leave);
			builder.setMessage(R.string.really_leave);
			builder.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Intent intentToSend = new Intent("sendMessage");
					intentToSend.putExtra("messageContent", Message.DELETE_DB);
					intentToSend.putExtra("type", Message.MSG_MSGLEAVE);					
					LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
					LocalBroadcastManager.getInstance(dm.getEvotingMainActivity()).unregisterReceiver(participantsUpdater);
					((EvotingApplication)activity.getApplication()).reset();
					activity.finish();
				}
			});
			builder.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();

			return true;

		case R.id.display_qrcode:
			// displaying the QR code

			SharedPreferences preferences = getSharedPreferences("network_preferences", 0);
			String config = preferences.getString("SSID", "N/A") + "||"
					+ preferences.getString("password", "N/A");
			Intent intent;
			try {
				intent = new Intent("com.google.zxing.client.android.ENCODE");
				intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
				intent.putExtra("ENCODE_DATA", config);
				intent.putExtra("ENCODE_FORMAT", "QR_CODE");
				intent.putExtra("ENCODE_SHOW_CONTENTS", false);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {

				// if the "Barcode Scanner" application is not installed ask the
				// user if he wants to install it
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("InstaCircle - Barcode Scanner Required");
				alertDialog.setMessage("In order to use this feature, the Application \"Barcode Scanner\" must be installed. Install now?");
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.dismiss();
						// redirect to Google Play
						try {
							startActivity(new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("market://details?id=com.google.zxing.client.android")));
						} catch (Exception e) {
							DataManager.LOGGER.debug("Unable to find market. User will have to install ZXing himself");
						}
					}
				});
				alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.dismiss();
					}
				});
				alertDialog.show();
			}
			return true;

		}

		return false;
	}

	/************************ Broadcast receivers ************************/


	/**
	 * if a participant has sent a message, update the list
	 */
	private BroadcastReceiver participantsUpdater = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!dm.isActive()){
				return;
			}
			List<Participant> list = new ArrayList<Participant>();
			if(dm.getProtocolParticipants()==null){
				list.addAll(dm.getTempParticipants().values());
			}
			else{
				list.addAll(dm.getProtocolParticipants().values());
				list.addAll(dm.getExcludedParticipants());
			}
			pca.clear();
			pca.addAll(list);
			pca.notifyDataSetChanged();
		}
	};


	/**
	 * If network is disconnected, a dialog inform him
	 */
	/*private BroadcastReceiver wifiEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String PREFS_NAME = "network_preferences";

			ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiAPManager wifiapmanager = new WifiAPManager();
			
			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			final String configuredSsid = preferences.getString("SSID", "N/A");
			//final String password = preferences.getString("password", "N/A");

			NetworkInfo nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			String ssid = wifi.getConnectionInfo().getSSID();
			// Only check the state if this device is not an access point
			if (!wifiapmanager.isWifiAPEnabled(wifi)) {
				if (!(nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED
						&& nInfo.getState() == NetworkInfo.State.CONNECTED && ssid
							.equals(configuredSsid))) {
					
					// Display a dialog to inform about connection loss
					AlertDialog.Builder builder = new AlertDialog.Builder(EvotingMainActivity.this);
					builder.setTitle(R.string.network_loss);
					builder.setMessage(R.string.network_loss_text);
					builder.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent intentToSend = new Intent("sendMessage");
							intentToSend.putExtra("messageContent", Message.DELETE_DB);
							intentToSend.putExtra("type", Message.MSG_MSGLEAVE);					
							LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
							LocalBroadcastManager.getInstance(dm.getEvotingMainActivity()).unregisterReceiver(participantsUpdater);
							((EvotingApplication)activity.getApplication()).reset();
							activity.finish();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
				
			
		}
	};*/


	/************************ UI Manipulation methods ************************/ 

	/**
	 * Set a text in a view
	 * @param viewId view where to set the text
	 * @param stringId text to set
	 */
	public void setTextInView(int viewId, int stringId){
		TextView tv = (TextView)findViewById(viewId);
		tv.setText(stringId);
	}

	/**
	 * Set the title of the activity
	 * @param stringId text to set
	 */
	public void setActivityTitle(int stringId){
		setTitle(stringId);
	}

	/**
	 * Change image in a view
	 * @param viewId view where to change the image
	 * @param resId image to set
	 */
	public void changeImage(int viewId, int resId){
		ImageView iv = (ImageView)findViewById(viewId);
		iv.setImageResource(resId);
	}

	/**
	 * Show a progress dialog
	 * @param title title to set
	 * @param text text to set
	 */
	public void showProgressDialog(String title, String text){
		pd = new ProgressDialog(this);
		pd.setTitle(title);
		pd.setMessage(text);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.show();
	}

	/**
	 * Close progress dialog
	 */
	public void dismissProgressDialog(){
		if(pd!=null && pd.isShowing())pd.dismiss();
	}


	/************************ Helper methods to launch another activity ************************/


	private AbstractAction callbackAction = null;
	private static final int COMMITMENT_REQUEST = 0;

	/**
	 * Launch an activity an wait for result
	 * This method is used in the commitment round to launch the vote activity
	 * @param c activity to launch
	 * @param action Action on which callback has to be called
	 * @param objects Objects to pass to the activity
	 */
	public void showActivity(@SuppressWarnings("rawtypes") Class c, AbstractAction action){
		this.callbackAction = action;
		Intent intent = new Intent(this,c);
		startActivityForResult(intent,COMMITMENT_REQUEST);
	}

	/**
	 * Method called on result of another activity
	 */
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data){
		if(requestCode==COMMITMENT_REQUEST){
			if(resultCode==Activity.RESULT_OK){
				callbackAction.executeCallback(data);
			}
		}
		else if(requestCode==ELECTION_INFO_REQUEST){
			if(resultCode==Activity.RESULT_OK){

				String question = data.getStringExtra("question");

				//create a "generator" for each candidate
				@SuppressWarnings("unchecked")
				List<Candidate> candidatesList = (ArrayList<Candidate>)data.getSerializableExtra("candidates");

				dm.setCandidates(candidatesList);
				dm.setQuestion(question);
				btnStart.setEnabled(true);
			}
		}
	}

}

/*
 *  UniCrypt Cryptographic Library
 *  Copyright (c) 2013 Berner Fachhochschule, Biel, Switzerland.
 *  All rights reserved.
 *
 *  Distributable under GPL license.
 *  See terms of license at gnu.org.
 *  
 */

package ch.bfh.instacircle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import ch.bfh.instacircle.db.NetworkDbHelper;

/**
 * Fragment which is embedded in the ParticipantDetailActivity. It displays
 * information of a participant.
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class ParticipantDetailFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {

	private GridLayout layout;
	private TextView participantIdentification;
	private TextView participantIpAddress;
	private TextView participantStatus;
	private TextView participantSequenceNumber;

	private int participantId;
	private NetworkDbHelper dbHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Subscibing to the participantJoined and participantChangedState
		// events to update immediately
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("messageArrived");
		intentFilter.addAction("participantChangedState");
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, intentFilter);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// applying the layout
		View rootView = inflater.inflate(R.layout.fragment_participant_detail,
				container, false);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// getting access to the Database
		dbHelper = new NetworkDbHelper(getActivity());

		participantId = getActivity().getIntent().getIntExtra("participant_id",
				-1);

		layout = (GridLayout) getView().findViewById(
				R.id.participant_detail_layout);

		participantIdentification = (TextView) layout
				.findViewById(R.id.participant_identification);
		participantIpAddress = (TextView) layout
				.findViewById(R.id.participant_ip_address);
		participantStatus = (TextView) layout
				.findViewById(R.id.participant_status);
		participantSequenceNumber = (TextView) layout
				.findViewById(R.id.participant_sequence_number);

		updateView();
	}

	/**
	 * Query the database and updates the labels in the view accordingly
	 */
	private void updateView() {

		Cursor participant = dbHelper.queryParticipant(participantId);
		participant.moveToFirst();

		participantIdentification.setText(participant.getString(participant
				.getColumnIndex("identification")));
		participantIpAddress.setText(participant.getString(participant
				.getColumnIndex("ip_address")));

		// use some pretty colours to display the state
		switch (participant.getInt(participant.getColumnIndex("state"))) {
		case 0:
			participantStatus.setTextColor(getActivity().getResources()
					.getColor(android.R.color.holo_orange_light));
			participantStatus.setText("inactive");
			break;
		case 1:
			participantStatus.setTextColor(getActivity().getResources()
					.getColor(android.R.color.holo_green_light));
			participantStatus.setText("active");
			break;
		default:
			participantStatus.setTextColor(getActivity().getResources()
					.getColor(android.R.color.holo_red_light));
			participantStatus.setText("unknown");
			break;
		}

		// get the sequencenumber and displaying it
		participantSequenceNumber
				.setText(""
						+ dbHelper
								.getCurrentParticipantSequenceNumber(participant.getString(participant
										.getColumnIndex("identification"))));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onDestroy()
	 */
	@Override
	public void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(
				mMessageReceiver);
		super.onDestroy();
	}

	/**
	 * if there is the new participant or the state of a participant has been
	 * changed we need to update the view
	 */
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateView();
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.bfh.instacircle.ParticipantsListFragment.Callbacks#onItemSelected(
	 * java.lang.String)
	 */
	public void onItemSelected(String id) {

	}
}

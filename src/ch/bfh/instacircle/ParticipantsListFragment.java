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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import ch.bfh.instacircle.db.NetworkDbHelper;

/**
 * This class implements the fragment which lists all the participants of the
 * conversation (middle tab of the NetworkActiveActivity)
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class ParticipantsListFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private int mActivatedPosition = ListView.INVALID_POSITION;

	private ParticipantCursorAdapter pca;

	private Cursor cursor;
	private NetworkDbHelper helper;

	public interface Callbacks {
		public void onItemSelected(String id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Subscribing to the participantJoined and participantChangedState
		// events to update immediately
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("participantJoined");
		intentFilter.addAction("participantChangedState");
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, intentFilter);

		// getting access to the database and query it
		helper = new NetworkDbHelper(getActivity());
		cursor = helper.queryParticipants();

		// initializing the adapter and assign it to myself
		pca = new ParticipantCursorAdapter(getActivity(), cursor);
		setListAdapter(pca);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View,
	 * android.os.Bundle)
	 */
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}

		this.getListView().setTranscriptMode(2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onDetach()
	 */
	@Override
	public void onDetach() {
		super.onDetach();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView
	 * , android.view.View, int, long)
	 */
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// launch the ParticipantDetailActivity when clicking on a participant
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		Intent intent = new Intent(getActivity(),
				ParticipantDetailActivity.class);
		intent.putExtra("participant_id",
				cursor.getInt(cursor.getColumnIndex("_id")));
		getActivity().startActivity(intent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * @param activateOnItemClick
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	/**
	 * @param position
	 */
	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
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
			pca.changeCursor(helper.queryParticipants());
		}
	};
}

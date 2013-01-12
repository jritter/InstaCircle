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
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import ch.bfh.instacircle.db.NetworkDbHelper;

/**
 * This class implements the fragment which lists all the messages of the
 * conversation (left tab of the NetworkActiveActivity)
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class MessageListFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private int mActivatedPosition = ListView.INVALID_POSITION;

	private MessageCursorAdapter mca;

	private Cursor cursor;
	private NetworkDbHelper helper;

	public interface Callbacks {
		public void onItemSelected(String id);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Subscribing to the messageArrived events to update immediately
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, new IntentFilter("messageArrived"));
		
		// getting access to the database and query it
		helper = new NetworkDbHelper(getActivity());
		cursor = helper.queryMessages();

		// initializing the adapter and assign it to myself
		mca = new MessageCursorAdapter(getActivity(), cursor);
		setListAdapter(mca);

	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View, android.os.Bundle)
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

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
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

	/* (non-Javadoc)
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
	 * we need to update the view as soon as a new message arrives
	 */
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mca.changeCursor(helper.queryMessages());
		}
	};
}

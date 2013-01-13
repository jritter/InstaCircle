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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import ch.bfh.instacircle.db.NetworkDbHelper;

/**
 * This class implements the fragment which displays the input box and send
 * button for composing new messages
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class MessageSendFragment extends Fragment implements OnClickListener {

	private static final String TAG = MessageSendFragment.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	private NetworkDbHelper dbHelper;
	private boolean broadcast = true;
	private String ipAddress = null;

	private Button btnSend;
	private EditText et;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// apply the layout
		View rootView = inflater.inflate(R.layout.fragment_message_send,
				container, false);

		// extract the button and the textfield
		btnSend = (Button) rootView.findViewById(R.id.send_button);
		et = (EditText) getActivity().findViewById(R.id.message_text);
		btnSend.setOnClickListener(this);
		dbHelper = new NetworkDbHelper(getActivity());

		// determining if we are dealing with broadcast or unicast messages
		if (getActivity() instanceof ParticipantDetailActivity) {
			ipAddress = ((ParticipantDetailActivity) getActivity())
					.getIpAddress();
			broadcast = false;
		}
		return rootView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View view) {

		if (view == btnSend) {
			et = (EditText) getActivity().findViewById(R.id.message_text);
			String identification = getActivity().getSharedPreferences(
					PREFS_NAME, 0).getString("identification", "N/A");

			// create new message instance
			Message message = new Message(et.getText().toString(),
					Message.MSG_CONTENT, identification,
					dbHelper.getNextSequenceNumber());
			Intent intent = new Intent("messageSend");
			intent.putExtra("message", message);
			if (broadcast == false && ipAddress == null) {
				Log.w(TAG, "IP Address not set, cannot send unicast message");
			} else if (broadcast == false) {

				// pass message to service using the local broacast manager
				intent.putExtra("ipAddress", ipAddress);
				intent.putExtra("broadcast", false);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						intent);
			} else {

				// pass message to service using the local broacast manager
				intent.putExtra("broadcast", true);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						intent);
			}
			// clear the textfield
			et.setText("");

		}
	}
}

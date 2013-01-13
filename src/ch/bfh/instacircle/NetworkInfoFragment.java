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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import ch.bfh.instacircle.db.NetworkDbHelper;
import ch.bfh.instacircle.wifi.AdhocWifiManager;

/**
 * This class implements the fragment which displays the technical infos about
 * the conversation (right tab of the NetworkActiveActivity)
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class NetworkInfoFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {

	private static final String PREFS_NAME = "network_preferences";

	private GridLayout layout;
	private TextView networkSSID;
	private TextView networkPassword;
	private TextView participantIdentification;
	private TextView ipAddress;
	private TextView broadcastAddress;
	private SharedPreferences preferences;
	private NetworkDbHelper dbHelper;

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

		// applying the view
		View rootView = inflater.inflate(R.layout.fragment_network_info,
				container, false);
		return rootView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.bfh.instacircle.ParticipantsListFragment.Callbacks#onItemSelected(
	 * java.lang.String)
	 */
	public void onItemSelected(String id) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onViewCreated(android.view.View,
	 * android.os.Bundle)
	 */
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		// get the network managers
		WifiManager wifi = (WifiManager) getActivity().getSystemService(
				Context.WIFI_SERVICE);
		AdhocWifiManager adhoc = new AdhocWifiManager(wifi);

		super.onViewCreated(view, savedInstanceState);
		dbHelper = new NetworkDbHelper(getActivity());
		preferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

		// set the labels accordingly
		layout = (GridLayout) getView().findViewById(R.id.network_info_layout);
		networkSSID = (TextView) layout.findViewById(R.id.network_ssid);
		networkPassword = (TextView) layout.findViewById(R.id.network_password);
		participantIdentification = (TextView) layout
				.findViewById(R.id.participant_identification);
		ipAddress = (TextView) layout.findViewById(R.id.ip_address);
		broadcastAddress = (TextView) layout
				.findViewById(R.id.broadcast_address);

		networkSSID.setText(preferences.getString("SSID", "N/A"));
		networkPassword.setText(dbHelper.getCipherKey());
		participantIdentification.setText(preferences.getString(
				"identification", "N/A"));
		ipAddress.setText(adhoc.getIpAddress().getHostAddress());
		broadcastAddress.setText(adhoc.getBroadcastAddress().getHostAddress());
	}
}

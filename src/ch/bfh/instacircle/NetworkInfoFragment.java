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

public class NetworkInfoFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {

	private static final String TAG = NetworkInfoFragment.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private GridLayout layout;
	private TextView networkSSID;
	private TextView networkPassword;
	private TextView participantIdentification;
	private TextView ipAddress;
	private TextView broadcastAddress;
	private SharedPreferences preferences;
	private NetworkDbHelper dbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_network_info,
				container, false);
		return rootView;
	}

	public void onItemSelected(String id) {

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		WifiManager wifi = (WifiManager) getActivity().getSystemService(
				Context.WIFI_SERVICE);
		AdhocWifiManager adhoc = new AdhocWifiManager(wifi);

		super.onViewCreated(view, savedInstanceState);
		dbHelper = new NetworkDbHelper(getActivity());
		preferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

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

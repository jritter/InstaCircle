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

public class MessageSendFragment extends Fragment implements OnClickListener {

	private static final String TAG = MessageSendFragment.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	private NetworkDbHelper dbHelper;
	private boolean broadcast = true;
	private String ipAddress = null;

	private Button btnSend;
	private EditText et;

	public MessageSendFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_message_send,
				container, false);
		btnSend = (Button) rootView.findViewById(R.id.send_button);
		et = (EditText) getActivity().findViewById(R.id.message_text);
		btnSend.setOnClickListener(this);
		dbHelper = new NetworkDbHelper(getActivity());

		if (getActivity() instanceof ParticipantDetailActivity) {
			ipAddress = ((ParticipantDetailActivity) getActivity())
					.getIpAddress();
			broadcast = false;
		}
		return rootView;
	}

	public void onClick(View view) {

		if (view == btnSend) {
			et = (EditText) getActivity().findViewById(R.id.message_text);
			String identification = getActivity().getSharedPreferences(
					PREFS_NAME, 0).getString("identification", "N/A");
			Message message = new Message(et.getText().toString(),
					Message.MSG_CONTENT, identification,
					dbHelper.getNextSequenceNumber());
			Intent intent = new Intent("messageSend");
			intent.putExtra("message", message);
			if (broadcast == false && ipAddress == null) {
				Log.w(TAG, "IP Address not set, cannot send unicast message");
			} else if (broadcast == false) {
				intent.putExtra("ipAddress", ipAddress);
				intent.putExtra("broadcast", false);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						intent);
			} else {
				intent.putExtra("broadcast", true);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						intent);
			}
			et.setText("");

		}
	}

	public boolean isBroadcast() {
		return broadcast;
	}

	public String getIpAddress() {
		return ipAddress;
	}
	
	public void enableControls() {
		et.setEnabled(true);
		btnSend.setEnabled(true);
	}
	
	public void disableControls() {
		et.setEnabled(false);
		btnSend.setEnabled(false);
	}
}

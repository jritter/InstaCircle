

package ch.bfh.instacircle;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class MessageFragment extends Fragment implements
		MessageListFragment.Callbacks {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void onSendButtonClicked(View view) {

		// assembling the message and sending it
		EditText et = (EditText) getActivity().findViewById(R.id.message_text);

		Message message = new Message(et.getText().toString(),
				Message.MSG_CONTENT, getActivity().getSharedPreferences(
						"network_preferences", 0)
						.getString("identifier", "N/A"));
		Intent intent = new Intent("messageSend");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
		et.setText("");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_message, container,
				false);
		return rootView;
	}

	public void onItemSelected(String id) {
		
	}

}

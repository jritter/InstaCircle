package ch.bfh.adhocnetwork;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import ch.bfh.adhocnetwork.db.NetworkDbHelper;

public class MessageSendFragment extends Fragment implements OnClickListener {
	
	private static final String PREFS_NAME = "network_preferences";
	private NetworkDbHelper dbHelper;

	Button btnSend;
	
    public MessageSendFragment() {
    	
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_send, container, false);
        btnSend = (Button) rootView.findViewById(R.id.send_button);
        btnSend.setOnClickListener(this);
        dbHelper = new NetworkDbHelper(getActivity());
        return rootView;
    }

	public void onClick(View view) {
		
		if (view == btnSend) {
			EditText et = (EditText)getActivity().findViewById(R.id.message_text);
			
			String identification = getActivity().getSharedPreferences(PREFS_NAME, 0).getString("identification", "N/A");
			String networkUUID = getActivity().getSharedPreferences(PREFS_NAME, 0).getString("networkUUID", "N/A");
			Message message = new Message(et.getText().toString(), Message.MSG_CONTENT, identification, dbHelper.getNextSequenceNumber(), networkUUID);
			Intent intent = new Intent("messageSend");
			intent.putExtra("message", message);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
			et.setText("");
		}
	}
}

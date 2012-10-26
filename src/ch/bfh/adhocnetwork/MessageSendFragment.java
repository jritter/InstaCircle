package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.R;


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
import android.widget.Toast;

public class MessageSendFragment extends Fragment implements OnClickListener {

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
        return rootView;
    }

	public void onClick(View view) {
		
		if (view == btnSend) {
			EditText et = (EditText)getActivity().findViewById(R.id.message_text);
			Toast.makeText(getActivity(), et.getText().toString(), Toast.LENGTH_SHORT).show();
			Message message = new Message(et.getText().toString(), 1, 1);
			Intent intent = new Intent("messageSend");
			intent.putExtra("message", message);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
			et.setText("");
		}
	}
}

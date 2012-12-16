package ch.bfh.instacircle;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import ch.bfh.instacircle.R;
import ch.bfh.instacircle.wifi.WifiAPManager;

public class MessageFragment extends Fragment implements
		MessageListFragment.Callbacks {
	
	private WifiAPManager wifiapman;
	private WifiManager wifiman; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		wifiapman = new WifiAPManager();
		wifiman = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);

	}
	
	public void onSendButtonClicked(View view) {
		
		EditText et = (EditText)getActivity().findViewById(R.id.message_text);
		
		Message message = new Message(et.getText().toString(), Message.MSG_CONTENT, getActivity().getSharedPreferences(
				"network_preferences", 0).getString("identifier", "N/A"));
		Intent intent = new Intent("messageSend");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
		et.setText("");
	}

	public void onItemSelected(String id) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message, container, false);
        return rootView;
    }
	
}

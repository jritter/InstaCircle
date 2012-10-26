package ch.bfh.adhocnetwork;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ch.bfh.adhocnetwork.wifi.WifiAPManager;

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
		
		Toast.makeText(getActivity(), et.getText().toString(), Toast.LENGTH_SHORT).show();
		Message message = new Message(et.getText().toString(), 1, 1);
		Intent intent = new Intent("messageSend");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
		et.setText("");
	}
	
	/*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_hotspot:
            	Toast.makeText(getActivity(), "Toggle WiFi AP", Toast.LENGTH_SHORT).show();
            	
            	wifiapman.toggleWiFiAP(wifiman, getActivity());
                
            default:
                return super.onOptionsItemSelected(item);
        }
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

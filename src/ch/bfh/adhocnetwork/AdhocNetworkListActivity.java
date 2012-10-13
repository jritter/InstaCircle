package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.R;
import ch.bfh.adhocnetwork.wifi.WifiAPManager;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AdhocNetworkListActivity extends FragmentActivity implements
		AdhocNetworkListFragment.Callbacks {

	private boolean mTwoPane;
	
	private WifiAPManager wifiapman;
	private WifiManager wifiman; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_adhocnetwork_list);

		if (findViewById(R.id.adhocnetwork_detail_container) != null) {
			mTwoPane = true;
			((AdhocNetworkListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.adhocnetwork_list))
					.setActivateOnItemClick(true);
		}

		

		Intent intent = new Intent(this, AdhocNetworkService.class);
		startService(intent);
		
		wifiapman = new WifiAPManager();
		wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);

	}

	public void onItemSelected(String id) {
		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(AdhocNetworkDetailFragment.ARG_ITEM_ID, id);
			AdhocNetworkDetailFragment fragment = new AdhocNetworkDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.adhocnetwork_detail_container, fragment)
					.commit();

		} else {
			Intent detailIntent = new Intent(this,
					AdhocNetworkDetailActivity.class);
			detailIntent.putExtra(AdhocNetworkDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}
	
	public void onSendButtonClicked(View view) {
		
		EditText et = (EditText)findViewById(R.id.message_text);
		
		Toast.makeText(this, et.getText().toString(), Toast.LENGTH_SHORT).show();
		Message message = new Message(et.getText().toString(), 1, 1);
		Intent intent = new Intent("messageSend");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		et.setText("");
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }
	
	/*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_hotspot:
            	Toast.makeText(this, "Toggle WiFi AP", Toast.LENGTH_SHORT).show();
            	
            	wifiapman.toggleWiFiAP(wifiman, this);
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

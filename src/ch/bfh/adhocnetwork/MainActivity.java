package ch.bfh.adhocnetwork;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import ch.bfh.adhocnetwork.wifi.AdhocNetworkConfiguration;
import ch.bfh.adhocnetwork.wifi.AdhocWiFiManager;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener, ConnectNetworkDialogFragment.NoticeDialogListener, TextWatcher{

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private SimpleAdapter adapter;
	private AdhocWiFiManager adhoc;
	
	private ArrayList<HashMap<String, Object>> arraylist = new ArrayList<HashMap<String, Object>>();
	private Button btnCreateNetwork;
	private String ITEM_DESCRIPTION = "description";

	private String ITEM_OBJECT = "object";
	private ListView lv;
	
	private SharedPreferences preferences;
	private List<ScanResult> results;

	private ScanResult selectedResult;

	private int size = 0;
	
	private EditText txtIdentification;
	
	private WifiManager wifi;
	
	private BroadcastReceiver wifibroadcastreceiver;

	public void afterTextChanged(Editable s) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("identification", txtIdentification.getText().toString());
		editor.commit();
		Log.d(TAG, "Preferences saved");
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

		
	}
	
	public void onClick(View v) {
		if (v == btnCreateNetwork){
			Intent intent = new Intent(this, CreateNetworkActivity.class);
			intent.putExtra("action", "joinnetwork");
			startActivity(intent);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (isServiceRunning()){
			startActivity(new Intent(this, NetworkActiveActivity.class));
		}
		
		setContentView(R.layout.activity_main);
		
		
		preferences = getSharedPreferences(PREFS_NAME, 0);
		String identification = preferences.getString("identification", readOwnerName());

		lv = (ListView) findViewById(R.id.network_listview);
		btnCreateNetwork = (Button) findViewById(R.id.create_network_button);
		btnCreateNetwork.setOnClickListener(this);
		
		txtIdentification = (EditText) findViewById(R.id.identification_edittext);
		txtIdentification.setText(identification);
		
		txtIdentification.addTextChangedListener(this);

		// Handling the WiFi...

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWiFiManager(wifi);
		
		
//		if (wifi.isWifiEnabled() == false) {
//			Toast.makeText(getApplicationContext(),
//					"WiFi is disabled. Enabling it...", Toast.LENGTH_LONG)
//					.show();
//			wifi.setWifiEnabled(true);
//		}

		adapter = new SimpleAdapter(this, arraylist, R.layout.list_item_network,
				new String[] { ITEM_DESCRIPTION }, new int[] { R.id.network_name });
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);

		
		wifibroadcastreceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context c, Intent intent) {
				results = wifi.getScanResults();
				size = results.size();
				
				try {
					size = size - 1;
					while (size >= 0) {
						HashMap<String, Object> item = new HashMap<String, Object>();
						item.put(ITEM_DESCRIPTION,
								results.get(size).SSID + "  "
										+ results.get(size).capabilities);
						
						item.put(ITEM_OBJECT, results.get(size));

						arraylist.add(item);
						Log.d(TAG, (String) item.get(ITEM_DESCRIPTION));
						size--;
						adapter.notifyDataSetChanged();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
		
		registerReceiver(wifibroadcastreceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_action_items, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.capture_qrcode:
			wifi.startScan();
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.setPackage("com.google.zxing.client.android");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	            String serializedAPConfig = intent.getStringExtra("SCAN_RESULT");
	    		
	    		try {
	    			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode(serializedAPConfig, Base64.DEFAULT)));
	    			AdhocNetworkConfiguration networkConfig = (AdhocNetworkConfiguration) ois.readObject();
	    			Log.d(TAG, networkConfig.toString());
	    			AdhocWiFiManager adhoc = new AdhocWiFiManager(wifi);
	    			adhoc.connectToNetwork(networkConfig.getSsid(), networkConfig.getPassword(), this);
	    		} catch (StreamCorruptedException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} catch (ClassNotFoundException e){
	    			e.printStackTrace();
	    		}
	            // Handle successful scan
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}

	@Override
	protected void onDestroy()
	{
	    super.onDestroy();
	    unregisterReceiver(wifibroadcastreceiver);
	}

	public void onDialogNegativeClick(DialogFragment dialog) {
		dialog.dismiss();
	}

	public void onDialogPositiveClick(DialogFragment dialog) {
		adhoc.connectToNetwork(selectedResult, ((ConnectNetworkDialogFragment) dialog).getPassword(), this);
		dialog.dismiss();
	}

	public void onItemClick(AdapterView<?> listview, View view, int arg2, long arg3) {
		
		HashMap<String, Object> hash = (HashMap<String, Object>) listview.getAdapter().getItem(arg2);
		
		selectedResult = (ScanResult) hash.get(ITEM_OBJECT);
		
		DialogFragment dialog = new ConnectNetworkDialogFragment();
		dialog.show(getFragmentManager(), TAG);
		
		//
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		scan();
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	public String readOwnerName () {
		
		Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		if (c.getCount() == 0){
			return "";
		}
		c.moveToFirst();
		String displayName = c.getString(c.getColumnIndex("display_name"));
		c.close();
		
		return displayName;
		
	}

	public void scan() {
		arraylist.clear();
		wifi.startScan();
	}
	
	public boolean isServiceRunning(){
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("ch.bfh.adhocnetwork.service.NetworkService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
     }
}

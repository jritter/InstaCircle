package ch.bfh.adhocnetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.bfh.adhocnetwork.wifi.AdhocWiFiManager;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener {

	

	private static final String TAG = MainActivity.class.getSimpleName();

	private WifiManager wifi;
	private AdhocWiFiManager adhoc;
	
	private ListView lv;
	private int size = 0;
	private List<ScanResult> results;

	private String ITEM_DESCRIPTION = "description";
	private String ITEM_OBJECT = "object";
	
	private ArrayList<HashMap<String, Object>> arraylist = new ArrayList<HashMap<String, Object>>();
	private SimpleAdapter adapter;

	private BroadcastReceiver wifibroadcastreceiver;

	private Button btnCreateNetwork;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		lv = (ListView) findViewById(R.id.network_listview);
		btnCreateNetwork = (Button) findViewById(R.id.create_network_button);
		
		btnCreateNetwork.setOnClickListener(this);

		// Handling the WiFi...

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWiFiManager(wifi);
		
		
		if (wifi.isWifiEnabled() == false) {
			Toast.makeText(getApplicationContext(),
					"WiFi is disabled. Enabling it...", Toast.LENGTH_LONG)
					.show();
			wifi.setWifiEnabled(true);
		}

		adapter = new SimpleAdapter(this, arraylist, R.layout.list_item_network,
				new String[] { ITEM_DESCRIPTION }, new int[] { R.id.network_name });
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);

		
		wifibroadcastreceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context c, Intent intent) {
				results = wifi.getScanResults();
				size = results.size();
				
				Toast.makeText(MainActivity.this, "Scanning....", Toast.LENGTH_SHORT).show();
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
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		scan();
	}
	
	@Override
	protected void onDestroy()
	{
	    super.onDestroy();
	    unregisterReceiver(wifibroadcastreceiver);
	}

	public void onClick(View v) {
		if (v == btnCreateNetwork){
			Intent intent = new Intent(this, CreateNetworkActivity.class);
			startActivity(intent);
		}

	}

	public void scan() {
		arraylist.clear();
		wifi.startScan();
	}

	public void onItemClick(AdapterView<?> listview, View view, int arg2, long arg3) {
		
		HashMap<String, Object> hash = (HashMap<String, Object>) listview.getAdapter().getItem(arg2);
		
		ScanResult result = (ScanResult) hash.get(ITEM_OBJECT);
		
		adhoc.connectToNetwork(result, this);
		
	}
	
}

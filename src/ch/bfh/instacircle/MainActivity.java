package ch.bfh.instacircle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Toast;
import ch.bfh.instacircle.wifi.AdhocWifiManager;

public class MainActivity extends Activity implements OnClickListener,
		OnItemClickListener, ConnectNetworkDialogFragment.NoticeDialogListener,
		TextWatcher {

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	private NetworkArrayAdapter adapter;
	private AdhocWifiManager adhoc;

	private ArrayList<HashMap<String, Object>> arraylist = new ArrayList<HashMap<String, Object>>();
	private HashMap<String, Object> lastItem = new HashMap<String, Object>();
	private Button btnCreateNetwork;

	private ListView lv;

	private SharedPreferences preferences;
	private List<ScanResult> results;
	private List<WifiConfiguration> configuredNetworks;

	private ScanResult selectedResult;

	private EditText txtIdentification;

	private WifiManager wifi;

	private BroadcastReceiver wifibroadcastreceiver;
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter nfcIntentFilter;
	private IntentFilter[] intentFiltersArray;
	private boolean nfcAvailable;
	private Parcelable[] rawMsgs;
	private int selectedNetId;

	public void afterTextChanged(Editable s) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("identification", txtIdentification.getText()
				.toString());
		editor.commit();
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	public void onClick(View v) {
		if (v == btnCreateNetwork) {
			Intent intent = new Intent(this, CreateNetworkActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isServiceRunning()) {
			Log.d(TAG, "going straight to the network Active Activity...");
			Intent intent = new Intent(this, NetworkActiveActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
		}

		setContentView(R.layout.activity_main);

		preferences = getSharedPreferences(PREFS_NAME, 0);
		String identification = preferences.getString("identification",
				readOwnerName());

		lv = (ListView) findViewById(R.id.network_listview);
//		btnCreateNetwork = (Button) findViewById(R.id.create_network_button);
//		btnCreateNetwork.setOnClickListener(this);

		txtIdentification = (EditText) findViewById(R.id.identification_edittext);
		txtIdentification.setText(identification);

		txtIdentification.addTextChangedListener(this);

		// Handling the WiFi...

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWifiManager(wifi);
		
		lastItem.put("SSID", "Create new network...");
		
		
		adapter = new NetworkArrayAdapter(this, R.layout.list_item_network, arraylist);
		adapter.add(lastItem);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);

		wifibroadcastreceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context c, Intent intent) {
				results = wifi.getScanResults();
				configuredNetworks = wifi.getConfiguredNetworks();
				arraylist.clear();

				for (ScanResult result : results) {
					HashMap<String, Object> item = new HashMap<String, Object>();

					item.put("known", false);

					// check whether the network is already known, i.e. the
					// password is already stored in the device
					for (WifiConfiguration configuredNetwork : configuredNetworks) {
						if (configuredNetwork.SSID.equals("\"".concat(
								result.SSID).concat("\""))) {
							item.put("known", true);
							item.put("netid", configuredNetwork.networkId);
							break;
						}
					}

					if (result.capabilities.contains("WPA")
							|| result.capabilities.contains("WEP")) {
						item.put("secure", true);
					} else {
						item.put("secure", false);
					}
					item.put("SSID", result.SSID);
					item.put("capabilities", result.capabilities);
					item.put("object", result);
					arraylist.add(item);
				}
				arraylist.add(lastItem);
				adapter.notifyDataSetChanged();

			}
		};

		registerReceiver(wifibroadcastreceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		// NFC stuff

		nfcAvailable = this.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_NFC);

		if (nfcAvailable) {
			nfcAdapter = NfcAdapter.getDefaultAdapter(this);

			rawMsgs = null;
			rawMsgs = getIntent().getParcelableArrayExtra(
					NfcAdapter.EXTRA_NDEF_MESSAGES);
			Log.d(TAG, "NFC is available");
			if (rawMsgs != null && !isServiceRunning()) {
				processNfcTag();
			}

			if (nfcAdapter.isEnabled()) {

				Intent intent = new Intent(this, getClass());
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

				nfcIntentFilter = new IntentFilter(
						NfcAdapter.ACTION_NDEF_DISCOVERED);
				try {
					nfcIntentFilter
							.addDataType("application/ch.bfh.instacircle");
				} catch (MalformedMimeTypeException e) {
					throw new RuntimeException("fail", e);
				}
				intentFiltersArray = new IntentFilter[] { nfcIntentFilter };
			} else {
				nfcAvailable = false;
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.capture_qrcode:

			wifi.startScan();
			try {
				Intent intent = new Intent(
						"com.google.zxing.client.android.SCAN");
				intent.setPackage("com.google.zxing.client.android");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, 0);
			} catch (ActivityNotFoundException e) {
				AlertDialog alertDialog = new AlertDialog.Builder(this)
						.create();
				alertDialog.setTitle("InstaCircle - Barcode Scanner Required");
				alertDialog
						.setMessage("In order to use this feature, the Application \"Barcode Scanner\" must be installed. Install now?");
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								try {
									startActivity(new Intent(
											Intent.ACTION_VIEW,
											Uri.parse("market://details?id=com.google.zxing.client.android")));
								} catch (Exception e) {
									Log.d(TAG,
											"Unable to find market. User will have to install ZXing himself");
								}
							}
						});
				alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				alertDialog.show();
			}
			return true;

		case R.id.rescan_wifi:
			wifi.startScan();
			Toast.makeText(this, "Rescan initiated", Toast.LENGTH_SHORT).show();

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == RESULT_OK) {
			String[] config = intent.getStringExtra("SCAN_RESULT").split(
					"\\|\\|");
			Log.d(TAG, "Extracted SSID from QR Code: " + config[0]);
			Log.d(TAG, "Extracted Password from QR Code: " + config[1]);

			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("SSID", config[0]);
			editor.putString("password", config[1]);
			editor.commit();

			connect(config);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(wifibroadcastreceiver);
	}

	public void onDialogNegativeClick(DialogFragment dialog) {
		dialog.dismiss();
	}

	public void onDialogPositiveClick(DialogFragment dialog) {

		if (selectedNetId != -1) {
			adhoc.connectToNetwork(selectedNetId, this);
		} else {
			adhoc.connectToNetwork(selectedResult.SSID,
					((ConnectNetworkDialogFragment) dialog).getNetworkKey(),
					this);
		}

		Log.d(TAG, "now saving the password "
				+ ((ConnectNetworkDialogFragment) dialog).getPassword());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("SSID", selectedResult.SSID);
		editor.putString("password",
				((ConnectNetworkDialogFragment) dialog).getPassword());
		editor.commit();

		dialog.dismiss();
	}

	public void onItemClick(AdapterView<?> listview, View view, int position,
			long id) {
		
		if (listview.getAdapter().getCount() - 1 == position){
			Intent intent = new Intent(this, CreateNetworkActivity.class);
			startActivity(intent);
		}
		else {

			HashMap<String, Object> hash = (HashMap<String, Object>) listview
					.getAdapter().getItem(position);
	
			selectedResult = (ScanResult) hash.get("object");
			selectedNetId = -1;
	
			if ((Boolean) hash.get("secure") && !((Boolean) hash.get("known"))) {
				DialogFragment dialog = new ConnectNetworkDialogFragment(true);
				dialog.show(getFragmentManager(), TAG);
			} else if ((Boolean) hash.get("known")) {
				selectedNetId = (Integer) hash.get("netid");
				DialogFragment dialog = new ConnectNetworkDialogFragment(false);
				dialog.show(getFragmentManager(), TAG);
			} else {
				DialogFragment dialog = new ConnectNetworkDialogFragment(false);
				dialog.show(getFragmentManager(), TAG);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		scan();
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	public String readOwnerName() {

		Cursor c = getContentResolver().query(
				ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		if (c.getCount() == 0) {
			return "";
		}
		c.moveToFirst();
		String displayName = c.getString(c.getColumnIndex("display_name"));
		c.close();

		return displayName;

	}

	public void scan() {
		wifi.startScan();
	}

	public boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("ch.bfh.instacircle.service.NetworkService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	// Handle the NFC part...
	@Override
	public void onNewIntent(Intent intent) {
		rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		processNfcTag();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAvailable = true;

		}

		if (nfcAvailable) {
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					intentFiltersArray, null);
		}

	}

	private void processNfcTag() {
		NdefMessage msg = (NdefMessage) rawMsgs[0];

		String[] config = new String(msg.getRecords()[0].getPayload())
				.split("\\|\\|");
		Log.d(TAG, "Extracted SSID from NFC Tag: " + config[0]);
		Log.d(TAG, "Extracted Password from NFC Tag: " + config[1]);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("SSID", config[0]);
		editor.putString("password", config[1]);
		editor.commit();

		connect(config);
	}

	private void connect(String[] config) {
		boolean connectedSuccessful = false;
		// check whether the network is already known, i.e. the password is
		// already stored in the device
		for (WifiConfiguration configuredNetwork : wifi.getConfiguredNetworks()) {
			if (configuredNetwork.SSID.equals("\"".concat(config[0]).concat(
					"\""))) {
				Log.d(TAG, "Found known network, connecting...");
				connectedSuccessful = true;
				adhoc.connectToNetwork(configuredNetwork.networkId, this);
				break;
			}
		}
		if (!connectedSuccessful) {
			for (ScanResult result : wifi.getScanResults()) {
				if (result.SSID.equals(config[0])) {
					connectedSuccessful = true;
					Log.d(TAG, "Found unknown network, connecting...");
					adhoc.connectToNetwork(config[0], config[1], this);
					break;
				}
			}
		}

		if (!connectedSuccessful) {
			Log.d(TAG, "network not found");
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("InstaCircle - Network not found");
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alertDialog.setMessage("The network \"" + config[0]
					+ "\" is not available, cannot connect.");
			alertDialog.show();
		}
	}
	
	public void addCreateNetworkItem () {
		
		
	}
}

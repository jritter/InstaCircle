package ch.bfh.instacircle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import ch.bfh.instacircle.db.NetworkDbHelper;
import ch.bfh.instacircle.service.NetworkService;
import ch.bfh.instacircle.wifi.AdhocWifiManager;
import ch.bfh.instacircle.wifi.WifiAPManager;

public class NetworkActiveActivity extends FragmentActivity implements
		ActionBar.TabListener {

	private static final String TAG = NetworkActiveActivity.class
			.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private WifiManager wifi;
	private AdhocWifiManager adhoc;
	private WifiAPManager wifiapmanager;
	
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter nfcIntentFilter;
	private IntentFilter[] intentFiltersArray;

	private ProgressDialog writeNfcTagDialog;

	private boolean nfcAvailable;
	private boolean writeNfcEnabled;
	
	private boolean repairInitiated;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_network_active);
		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab.
		// We can also use ActionBar.Tab#select() to do this if we have a
		// reference to the
		// Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});
		
		this.registerReceiver(wifiReceiver,new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		// Handle the change of the Wifi configuration
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWifiManager(wifi);
		wifiapmanager = new WifiAPManager();

		// NFC stuff

		nfcAvailable = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
		
		if (nfcAvailable){
			nfcAdapter = NfcAdapter.getDefaultAdapter(this);
			if (nfcAdapter.isEnabled()){
				pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
						getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		
				nfcIntentFilter = new IntentFilter();
				nfcIntentFilter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
				nfcIntentFilter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
				intentFiltersArray = new IntentFilter[] { nfcIntentFilter };
			}
			else {
				nfcAvailable = false;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_items, menu);
		if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)){
			menu.removeItem(R.id.write_nfc_tag);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.display_qrcode:

			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			String config = preferences.getString("SSID", "N/A")
					+ "||" + preferences.getString("password", "N/A");
			try {
				intent = new Intent("com.google.zxing.client.android.ENCODE");
				intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
				intent.putExtra("ENCODE_DATA", config);
				intent.putExtra("ENCODE_FORMAT", "QR_CODE");
				intent.putExtra("ENCODE_SHOW_CONTENTS", false);
				startActivity(intent);
			} catch (ActivityNotFoundException e){
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
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

		case R.id.leave_network:

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Leave Network?");
			builder.setMessage("Do you really want to leave this conversation?");
			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							
							if (isServiceRunning()){
								String identification = getSharedPreferences(
										PREFS_NAME, 0).getString("identification",
										"N/A");
								String networkUUID = getSharedPreferences(
										PREFS_NAME, 0).getString("networkUUID",
										"N/A");
								Message message = new Message(identification,
										Message.MSG_MSGLEAVE, identification,
										new NetworkDbHelper(
												NetworkActiveActivity.this)
												.getNextSequenceNumber(),
										networkUUID);
								Intent intent = new Intent("messageSend");
								intent.putExtra("message", message);
								LocalBroadcastManager.getInstance(
										NetworkActiveActivity.this).sendBroadcast(
										intent);
							}
							else {
								new NetworkDbHelper(NetworkActiveActivity.this).closeConversation();
								NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								notificationManager.cancelAll();
								Intent intent = new Intent(NetworkActiveActivity.this, MainActivity.class);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
								startActivity(intent);
							}
						}
					});
			builder.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();

			return true;

		case R.id.write_nfc_tag:
			
			if (!nfcAdapter.isEnabled()){
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("InstaCircle - NFC needs to be enabled");
				alertDialog.setMessage("In order to use this feature, NFC must be enabled. Enable now?");
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int which) {
				    	  dialog.dismiss();
				    	  startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
				       } 
				});
				alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int which) {
				    	  dialog.dismiss();
				       } 
				});
				alertDialog.show();
			}
			else {
				writeNfcEnabled = true;
				writeNfcTagDialog = new ProgressDialog(this);
				writeNfcTagDialog.setTitle("InstaCircle - Share Networkconfiguration with NFC Tag");
				writeNfcTagDialog
						.setMessage("Please tap a writeable NFC Tag on the back of your device...");
				writeNfcTagDialog.setCancelable(false);
				writeNfcTagDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						"Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								writeNfcEnabled = false;
								dialog.dismiss();
							}
						});
	
				writeNfcTagDialog.show();
			}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		private ArrayList<Fragment> fragments; 
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			
			fragments = new ArrayList<Fragment>();
			fragments.add(new MessageFragment());
			fragments.add(new ParticipantsFragment());
			fragments.add(new NetworkInfoFragment());
		}

		@Override
		public Fragment getItem(int i) {
			return fragments.get(i);
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.tab_title_messages).toUpperCase();
			case 1:
				return getString(R.string.tab_title_participants).toUpperCase();
			case 2:
				return getString(R.string.tab_title_network_info).toUpperCase();
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		public DummySectionFragment() {
		}

		public static final String ARG_SECTION_NUMBER = "section_number";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			TextView textView = new TextView(getActivity());
			textView.setGravity(Gravity.CENTER);
			Bundle args = getArguments();
			textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
			return textView;
		}
	}

	// Handle the NFC part...
	@Override
	public void onNewIntent(Intent intent) {
		if (writeNfcEnabled) {

			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			String text = preferences.getString("SSID", "N/A")
					+ "||" + preferences.getString("password", "N/A");
//			String lang = "en";
//			byte[] textBytes = text.getBytes();
//			byte[] langBytes = null;
//			try {
//				langBytes = lang.getBytes("US-ASCII");
//			} catch (UnsupportedEncodingException e1) {
//				e1.printStackTrace();
//			}
//			int langLength = langBytes.length;
//			int textLength = textBytes.length;
//			byte[] payload = new byte[1 + langLength + textLength];
//
//			// set status byte (see NDEF spec for actual bits)
//			payload[0] = (byte) langLength;
//
//			// copy langbytes and textbytes into payload
//			System.arraycopy(langBytes, 0, payload, 1, langLength);
//			System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
//
//			NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
//					NdefRecord.RTD_TEXT, new byte[0], payload);
			
			NdefRecord record = createMimeRecord("application/ch.bfh.instacircle", text.getBytes());
			
			NdefRecord aar = NdefRecord.createApplicationRecord(getPackageName());

			NdefMessage message = new NdefMessage(new NdefRecord[] { record, aar });

			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			writeTag(tag, message);

			writeNfcEnabled = false;
			writeNfcTagDialog.dismiss();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAvailable){
			nfcAdapter.disableForegroundDispatch(this);
		}
	}
	
	
	@Override
	protected void onDestroy()
	{
	    super.onDestroy();
	    unregisterReceiver(wifiReceiver);
	}

	@Override
	protected void onResume() {
		
		super.onResume();
		
		if (nfcAdapter != null && nfcAdapter.isEnabled()){
			nfcAvailable = true;
		}
		
		if (nfcAvailable){
			Log.d(TAG, "Enabling foreground Dispatching...");
			nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
		}
		
		checkNetworkState();

	}
	
	public boolean writeTag (Tag tag, NdefMessage message){
		
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("InstaCircle - write NFC Tag failed");
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int which) {
		    	  dialog.dismiss();
		       } 
		});
		try {
			// see if tag is already NDEF formatted
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Log.d(TAG, "This tag is read only.");
					alertDialog.setMessage("This tag is read only.");
					alertDialog.show();
					return false;
				}

				// work out how much space we need for the data
				int size = message.toByteArray().length;
				if (ndef.getMaxSize() < size) {
					Log.d(TAG, "Tag doesn't have enough free space.");
					alertDialog.setMessage("Tag doesn't have enough free space.");
					alertDialog.show();
					return false;
				}

				ndef.writeNdefMessage(message);
				Log.d(TAG, "Tag written successfully.");
				
			} else {
				// attempt to format tag
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						Log.d(TAG, "Tag written successfully!");
					} catch (IOException e) {
						alertDialog.setMessage("Unable to format tag to NDEF.");
						alertDialog.show();
						Log.d(TAG, "Unable to format tag to NDEF.");
						return false;

					}
				} else {
					Log.d(TAG, "Tag doesn't appear to support NDEF format.");
					alertDialog.setMessage("Tag doesn't appear to support NDEF format.");
					alertDialog.show();
					return false;
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "Failed to write tag");
			return false;
		}
		alertDialog.setTitle("InstaCircle");
		alertDialog.setMessage("NFC Tag written successfully.");
		alertDialog.show();
		return true;
	}
	
	
	private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
	
		@Override
	    public void onReceive(Context context, Intent intent) {
	        checkNetworkState();
	        Log.d(TAG, "State: " +  intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
	    }
	};
	
	public void checkNetworkState() {
		ConnectivityManager conn = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		final String configuredSsid = preferences.getString("SSID", "N/A");
		final String password = preferences.getString("password", "N/A");
		
		NetworkInfo nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		String ssid = wifi.getConnectionInfo().getSSID();
		Log.d(TAG, "Currently active SSID: " + ssid);
		// Only check the state if this device is not an access point
		if (!wifiapmanager.isWifiAPEnabled(wifi)){
			if (!(nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && nInfo.getState() == NetworkInfo.State.CONNECTED && ssid.equals(configuredSsid))){
				if (repairInitiated == false){
					repairInitiated = true;
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("InstaCircle - Network Connection Lost");
					alertDialog
							.setMessage("The connection to the network " + configuredSsid + " has been lost. Try to repair?");
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Repair",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									adhoc.connectToNetwork(configuredSsid, password, NetworkActiveActivity.this, false);
								}
							});
					alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Leave",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									WifiManager wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
									new AdhocWifiManager(wifiman).restoreWifiConfiguration(getBaseContext());
									new WifiAPManager().disableHotspot(wifiman, getBaseContext());
									NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
									notificationManager.cancelAll();
									Intent stopIntent = new Intent(NetworkActiveActivity.this, NetworkService.class);
									stopService(stopIntent);
									Intent intent = new Intent(NetworkActiveActivity.this, MainActivity.class);
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
									startActivity(intent);
								}
							});
					alertDialog.show();
				}
			}
			else {
				Log.d(TAG, "All good! :-)");
				repairInitiated = false;
			}
		}
		
	}
	
	/**
     * Creates a custom MIME type encapsulated in an NDEF record
     *
     * @param mimeType
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
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
}

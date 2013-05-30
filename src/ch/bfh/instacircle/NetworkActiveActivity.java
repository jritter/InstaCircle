/*
 *  UniCrypt Cryptographic Library
 *  Copyright (c) 2013 Berner Fachhochschule, Biel, Switzerland.
 *  All rights reserved.
 *
 *  Distributable under GPL license.
 *  See terms of license at gnu.org.
 *  
 */

package ch.bfh.instacircle;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ch.bfh.instacircle.db.NetworkDbHelper;
import ch.bfh.instacircle.service.NetworkService;
import ch.bfh.instacircle.wifi.AdhocWifiManager;
import ch.bfh.instacircle.wifi.WifiAPManager;

/**
 * Activity which is displayed during an active conversation
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 * 
 */
public class NetworkActiveActivity extends FragmentActivity implements
		ActionBar.TabListener {

	private static final String TAG = NetworkActiveActivity.class
			.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	SectionsPagerAdapter mSectionsPagerAdapter;

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
	private long repairTime;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_network_active);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		// Handle the change of the Wifi configuration
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWifiManager(wifi);
		wifiapmanager = new WifiAPManager();
		registerReceiver(wifiReceiver, new IntentFilter(
				WifiManager.WIFI_STATE_CHANGED_ACTION));

		// Is NFC available on this device?
		nfcAvailable = this.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_NFC);

		// only set up the NFC stuff if NFC is also available
		if (nfcAvailable) {
			nfcAdapter = NfcAdapter.getDefaultAdapter(this);
			if (nfcAdapter.isEnabled()) {

				// Setting up a pending intent that is invoked when an NFC tag
				// is tapped on the back
				pendingIntent = PendingIntent.getActivity(this, 0, new Intent(
						this, getClass())
						.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

				nfcIntentFilter = new IntentFilter();
				nfcIntentFilter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
				nfcIntentFilter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
				intentFiltersArray = new IntentFilter[] { nfcIntentFilter };
			} else {
				nfcAvailable = false;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_network_active, menu);

		// only display the NFC action if NFC is available
		if (!this.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_NFC)) {
			menu.removeItem(R.id.write_nfc_tag);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.display_qrcode:
			// displaying the QR code
			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			String config = preferences.getString("SSID", "N/A") + "||"
					+ preferences.getString("password", "N/A");
			try {
				intent = new Intent("com.google.zxing.client.android.ENCODE");
				intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
				intent.putExtra("ENCODE_DATA", config);
				intent.putExtra("ENCODE_FORMAT", "QR_CODE");
				intent.putExtra("ENCODE_SHOW_CONTENTS", false);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {

				// if the "Barcode Scanner" application is not installed ask the
				// user if he wants to install it
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
								// redirect to Google Play
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

			// Display a confirm dialog asking whether really to leave
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Leave Network?");
			builder.setMessage("Do you really want to leave this conversation?");
			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							if (isServiceRunning()) {
								String identification = getSharedPreferences(
										PREFS_NAME, 0).getString(
										"identification", "N/A");
								Message message = new Message(identification,
										Message.MSG_MSGLEAVE, identification,
										NetworkDbHelper.getInstance(
												NetworkActiveActivity.this)
												.getNextSequenceNumber());
								Intent intent = new Intent("messageSend");
								intent.putExtra("message", message);
								LocalBroadcastManager.getInstance(
										NetworkActiveActivity.this)
										.sendBroadcast(intent);
							} else {
								NetworkDbHelper.getInstance(NetworkActiveActivity.this)
										.closeConversation();
								NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								notificationManager.cancelAll();
								Intent intent = new Intent(
										NetworkActiveActivity.this,
										MainActivity.class);
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

			if (!nfcAdapter.isEnabled()) {

				// if nfc is available but deactivated ask the user whether he
				// wants to enable it. If yes, redirect to the settings.
				AlertDialog alertDialog = new AlertDialog.Builder(this)
						.create();
				alertDialog.setTitle("InstaCircle - NFC needs to be enabled");
				alertDialog
						.setMessage("In order to use this feature, NFC must be enabled. Enable now?");
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								startActivity(new Intent(
										android.provider.Settings.ACTION_WIRELESS_SETTINGS));
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
			} else {
				// display a progress dialog waiting for the NFC tag to be
				// tapped
				writeNfcEnabled = true;
				writeNfcTagDialog = new ProgressDialog(this);
				writeNfcTagDialog
						.setTitle("InstaCircle - Share Networkconfiguration with NFC Tag");
				writeNfcTagDialog
						.setMessage("Please tap a writeable NFC Tag on the back of your device...");
				writeNfcTagDialog.setCancelable(false);
				writeNfcTagDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						"Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabUnselected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabSelected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabReselected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * FragmentPagerAdapter which handles the tabs
	 * 
	 * @author Juerg Ritter (rittj1@bfh.ch)
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		private ArrayList<Fragment> fragments;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);

			fragments = new ArrayList<Fragment>();
			fragments.add(new MessageFragment());
			fragments.add(new ParticipantsListFragment());
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

			// set the labels
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent intent) {
		if (writeNfcEnabled) {
			// Handle the NFC part...

			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			String text = preferences.getString("SSID", "N/A") + "||"
					+ preferences.getString("password", "N/A");

			// create a new NdefRecord
			NdefRecord record = createMimeRecord(
					"application/ch.bfh.instacircle", text.getBytes());

			// create a new Android Application Record
			NdefRecord aar = NdefRecord
					.createApplicationRecord(getPackageName());

			// create a ndef message
			NdefMessage message = new NdefMessage(new NdefRecord[] { record,
					aar });

			// extract tag from the intent
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			// write the tag
			writeTag(tag, message);

			// close the dialog
			writeNfcEnabled = false;
			writeNfcTagDialog.dismiss();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(wifiReceiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onResume()
	 */
	@Override
	protected void onResume() {

		super.onResume();

		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAvailable = true;
		}

		// make sure that this activity is the first one which can handle the
		// NFC tags
		if (nfcAvailable) {
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					intentFiltersArray, null);
		}

		checkNetworkState();
	}

	/**
	 * Writes an NFC Tag
	 * 
	 * @param tag
	 *            The reference to the tag
	 * @param message
	 *            the message which should be writen on the message
	 * @return true if successful, false otherwise
	 */
	public boolean writeTag(Tag tag, NdefMessage message) {

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("InstaCircle - write NFC Tag failed");
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
				new DialogInterface.OnClickListener() {
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
					alertDialog
							.setMessage("Tag doesn't have enough free space.");
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
					alertDialog
							.setMessage("Tag doesn't appear to support NDEF format.");
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

	/**
	 * Broadcast receiver which gets invoked when the network configuration
	 * changes
	 */
	private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			checkNetworkState();
		}
	};

	/**
	 * Checks whether the current network configuration is how it is supposed to
	 * be
	 */
	public void checkNetworkState() {
		ConnectivityManager conn = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		final String configuredSsid = preferences.getString("SSID", "N/A");
		final String password = preferences.getString("password", "N/A");

		NetworkInfo nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		String ssid = wifi.getConnectionInfo().getSSID();
		Log.d(TAG, "Currently active SSID: " + ssid);
		// Only check the state if this device is not an access point
		if (!wifiapmanager.isWifiAPEnabled(wifi)) {
			Log.d(TAG, "Configured SSID: " + configuredSsid);
			if (!(nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED
					&& nInfo.getState() == NetworkInfo.State.CONNECTED && ssid
						.equals(configuredSsid))) {
				if (repairInitiated == false
						&& repairTime + 6000 < System.currentTimeMillis()) {
					repairInitiated = true;
					repairTime = System.currentTimeMillis();
					// create a dialog that ask whether you want to repair the
					// network connection
					AlertDialog alertDialog = new AlertDialog.Builder(this)
							.create();
					alertDialog
							.setTitle("InstaCircle - Network Connection Lost");
					alertDialog
							.setMessage("The connection to the network "
									+ configuredSsid
									+ " has been lost. Try to repair?");
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
							"Repair", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									adhoc.connectToNetwork(configuredSsid,
											password,
											NetworkActiveActivity.this, false);
									repairInitiated = false;
								}
							});
					alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Leave",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									WifiManager wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
									new AdhocWifiManager(wifiman)
											.restoreWifiConfiguration(getBaseContext());
									new WifiAPManager().disableHotspot(wifiman,
											getBaseContext());
									NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
									notificationManager.cancelAll();
									Intent stopIntent = new Intent(
											NetworkActiveActivity.this,
											NetworkService.class);
									stopService(stopIntent);
									Intent intent = new Intent(
											NetworkActiveActivity.this,
											MainActivity.class);
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
									startActivity(intent);
								}
							});
					alertDialog.show();
				}
			} else {
				repairInitiated = false;
			}
		}

	}

	/**
	 * Creates a custom MIME type encapsulated in an NDEF record
	 * 
	 * @param mimeType
	 *            The string with the mime type name
	 */
	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	/**
	 * checks whether the NetworkService is running or not
	 * 
	 * @return true if service is running, false otherwise
	 */
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

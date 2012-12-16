package ch.bfh.instacircle.wifi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import ch.bfh.instacircle.NetworkActiveActivity;
import ch.bfh.instacircle.service.NetworkService;

public class AdhocWiFiManager {
	
	private static final String TAG = AdhocWiFiManager.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	
	private WifiManager wifiManager;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	
	public AdhocWiFiManager(WifiManager wifiManager){
		this.wifiManager = wifiManager;
	}
	
	public void connectToNetwork(ScanResult result, String password, Context context){
		new ConnectWifiTask(result, password, context).execute();
	}
	
	public void connectToNetwork(WifiConfiguration wifiConfiguration,
			Context context) {
		new ConnectWifiTask(wifiConfiguration, context).execute();
	}
	
	public void connectToNetwork(final String SSID, final String password, final Context context){
		List<ScanResult> results = wifiManager.getScanResults();
		for (ScanResult result : results){
			Log.d(TAG, "SSID: " + result.SSID);
			if (result.SSID.equals(SSID)){
				
				new ConnectWifiTask(result, password, context).execute();
				return;
			}
		}
	}
	
	public void restoreWifiConfiguration(Context context){
		new RestoreWifiConfigurationTask(context).execute();
	}
	
	
	class ConnectWifiTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private ProgressDialog d;
		private WifiConfiguration config;
		private boolean success;
		private SupplicantState supplicantState;
		private String password;
		
		public ConnectWifiTask(ScanResult result, String password, Context context) {
			this.context = context;
			d = new ProgressDialog(context);
			
			this.password = password;
			
			config = new WifiConfiguration();
			//config.SSID = result.SSID;
			config.SSID = result.SSID;

			config.BSSID = result.BSSID;
			config.hiddenSSID = false;
			config.priority = 10000;
			config.preSharedKey = "\"".concat(password).concat("\"");
			config.status = WifiConfiguration.Status.ENABLED;        
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			
			Log.d(TAG, "Connecting configuration");
			//Log.d(TAG, new SerializableWifiConfiguration(config).toString());
			
		}
		
		public ConnectWifiTask(WifiConfiguration config, Context context) {
			this.context = context;
			this.config = config;
			this.password = config.preSharedKey;
			d = new ProgressDialog(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			// Backup current Network configuration
			preferences = context.getSharedPreferences(PREFS_NAME, 0);
			editor = preferences.edit();
			editor.putInt("originalNetId", wifiManager.getConnectionInfo().getNetworkId());
			editor.putString("password", password);
			editor.putString("SSID", config.SSID);
			editor.commit();
			config.SSID = "\"" + config.SSID + "\"";
			
			Log.d(TAG, "PSK: " + config.preSharedKey);
			Log.d(TAG, "=========================");
			Log.d(TAG, config.toString());
			Log.d(TAG, "=========================");
			int netid = wifiManager.addNetwork(config);
			Log.d(TAG, "Netid: " + netid);
			success = wifiManager.enableNetwork(netid, true);
			Log.d(TAG, "Success: " + success);
			int maxLoops = 10;
			int i = 0;
			
			ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo nInfo = null;
			
			while (i < maxLoops){
				nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				Log.d(TAG, nInfo.getDetailedState().toString() + "  " + nInfo.getState().toString());
				wifiManager.getConnectionInfo();
				if (nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && nInfo.getState() == NetworkInfo.State.CONNECTED && getBroadcastAddress() != null){
					Log.d(TAG, "Connected!");
					break;
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
			}
			
			if (!(nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && nInfo.getState() == NetworkInfo.State.CONNECTED && getBroadcastAddress() != null)){
				success = false;
			}
			else {
				preferences = context.getSharedPreferences(PREFS_NAME, 0);
				editor = preferences.edit();
				editor.remove("networkUUID");
				editor.commit();
				
				
				Intent intent = new Intent(context, NetworkService.class);
				intent.putExtra("action", "joinnetwork");
				context.stopService(intent);
				context.startService(intent);
			}
			Log.d(TAG, "Success: " + success);
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			d.setTitle("Connecting to Network " + config.SSID + "...");
			d.setMessage("...please wait a moment.");
			d.show();
		}
		
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			d.dismiss();
			if (success){
				Intent intent = new Intent(context, NetworkActiveActivity.class);	
				context.startActivity(intent);
			}
			else {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Connection failed");
				builder.setMessage("The attempt to connect to the network failed.");
				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								return;
							}
						});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
		
	}
	
	
	class RestoreWifiConfigurationTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private ProgressDialog d;
		private boolean success;
		
		public RestoreWifiConfigurationTask (Context context){
			this.context = context;
			//d = new ProgressDialog(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			
			preferences = context.getSharedPreferences(PREFS_NAME, 0);
			success = wifiManager.enableNetwork(preferences.getInt("originalNetId", 0), true);
			
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
//			d.setTitle("Restoring original network configuration...");
//			d.setMessage("...please wait a moment.");
//			d.show();
		}
		
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
//			d.dismiss();
		}
	}
	
	private InetAddress getBroadcastAddress() {
		DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
		if (myDhcpInfo == null) {
			System.out.println("Could not get broadcast address");
			return null;
		}
		int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
					| ~myDhcpInfo.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
		quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		try {
			return InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
}

package ch.bfh.instacircle.wifi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import ch.bfh.instacircle.NetworkActiveActivity;
import ch.bfh.instacircle.service.NetworkService;

public class AdhocWifiManager {
	
	private static final String TAG = AdhocWifiManager.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	
	private WifiManager wifiManager;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	
	public AdhocWifiManager(WifiManager wifiManager){
		this.wifiManager = wifiManager;
		wifiManager.setWifiEnabled(true);
	}
	
	public void connectToNetwork(WifiConfiguration wifiConfiguration,
			Context context) {
		new ConnectWifiTask(wifiConfiguration, context).execute();
	}
	
	public void connectToNetwork(final String SSID, final String password, final Context context){
		connectToNetwork(SSID, password, context, true);
	}
	
	public void connectToNetwork(final String SSID, final String password, final Context context, final boolean startActivity){
		new ConnectWifiTask(SSID, password, context, startActivity).execute();
	}
	
	public void connectToNetwork(final int networkId, final Context context){
		new ConnectWifiTask(networkId, context).execute();
	}
	
	public void restoreWifiConfiguration(Context context){
		new RestoreWifiConfigurationTask(context).execute();
	}
	
	
	class ConnectWifiTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private ProgressDialog d;
		private WifiConfiguration config;
		private boolean success;
		private String password;
		private String ssid;
		private int networkId = -1;
		
		private boolean startActivity = true;
		
		public ConnectWifiTask(String ssid, String password, Context context, boolean startActivity) {

			this.context = context;
			this.startActivity = startActivity;
			d = new ProgressDialog(context);
			
			this.password = password;
			this.ssid = ssid;
			
			config = new WifiConfiguration();

			config.hiddenSSID = false;
			config.priority = 10000;
			if (password != null){
				config.preSharedKey = "\"".concat(password).concat("\"");
				config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
				config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				config.wepKeys = new String [] { password };
			}
			else {
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			}
			config.status = WifiConfiguration.Status.ENABLED;        
			
		}
		
		public ConnectWifiTask(WifiConfiguration config, Context context) {
			this.context = context;
			this.config = config;
			this.password = config.preSharedKey;
			d = new ProgressDialog(context);
		}
		
		public ConnectWifiTask(int networkId, Context context) {
			this.context = context;
			this.networkId = networkId;
			for (WifiConfiguration config : wifiManager.getConfiguredNetworks()){
				if (config.networkId == networkId){
					this.config = config;
					break;
				}
			}
			this.ssid = config.SSID;
			d = new ProgressDialog(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			wifiManager.setWifiEnabled(true);
			
			// Backup current Network configuration
			preferences = context.getSharedPreferences(PREFS_NAME, 0);
			editor = preferences.edit();
			editor.putInt("originalNetId", wifiManager.getConnectionInfo().getNetworkId());
			editor.commit();
			config.SSID = "\"" + config.SSID + "\"";
						
			if (networkId != -1){
				// Configuration already exists, no need to create a new one...
				success = wifiManager.enableNetwork(networkId, true);
			} else {
				
				// Configuration has to be created and added
				wifiManager.startScan();
				
				int maxLoops = 10;
				int i = 0;
				while (i < maxLoops){
					if (wifiManager.getScanResults() != null){
						Log.d(TAG, "got Scanresults");
						break;
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					i++;
				}
				if (wifiManager.getScanResults() == null){
					success = false;
					return null;
				}
				
				List<ScanResult> results = wifiManager.getScanResults();
				for (ScanResult result : results){
					Log.d(TAG, "SSID: " + result.SSID);
					if (result.SSID.equals(ssid)){
						config.SSID = result.SSID;
						config.BSSID = result.BSSID;
						break;
					}
				}	
	
				networkId = wifiManager.addNetwork(config);
				
				success = wifiManager.enableNetwork(networkId, true);
			}
			
			ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo nInfo = null;
			
			int i = 0;
			int maxLoops = 10;
			while (i < maxLoops){
				nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				Log.d(TAG, nInfo.getDetailedState().toString() + "  " + nInfo.getState().toString());
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
				
				if (startActivity){
					Intent intent = new Intent(context, NetworkService.class);
					intent.putExtra("action", "joinnetwork");
					context.stopService(intent);
					context.startService(intent);
				}
			}
			Log.d(TAG, "Success: " + success);
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			d.setTitle("Connecting to Network " + ssid + "...");
			d.setMessage("...please wait a moment.");
			d.show();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			d.dismiss();
			if (startActivity) {
				if (success) {
					Intent intent = new Intent(context, NetworkActiveActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					context.startActivity(intent);
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							context);
					builder.setTitle("Connection failed");
					builder.setMessage("The attempt to connect to the network failed.");
					builder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									return;
								}
							});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
		}
		
	}
	
	
	class RestoreWifiConfigurationTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		
		public RestoreWifiConfigurationTask (Context context){
			this.context = context;
			//d = new ProgressDialog(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			
			preferences = context.getSharedPreferences(PREFS_NAME, 0);
			wifiManager.enableNetwork(preferences.getInt("originalNetId", 0), true);
			
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
	
	public InetAddress getBroadcastAddress() {
		DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
		if (myDhcpInfo == null) {
			System.out.println("Could not get broadcast address");
			return null;
		}
		int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
					| ~myDhcpInfo.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++){
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		}
		try {
			return InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public InetAddress getIpAddress() {
		DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
		int ipaddress = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++){
			quads[k] = (byte) ((ipaddress >> k * 8) & 0xFF);
		}
		try {
			return InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
}

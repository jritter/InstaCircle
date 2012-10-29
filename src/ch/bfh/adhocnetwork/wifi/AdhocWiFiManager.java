package ch.bfh.adhocnetwork.wifi;

import ch.bfh.adhocnetwork.NetworkActiveActivity;
import ch.bfh.adhocnetwork.service.NetworkService;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class AdhocWiFiManager {
	
	private static final String TAG = AdhocWiFiManager.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	
	private WifiManager wifiManager;
	
	public AdhocWiFiManager(WifiManager wifiManager){
		this.wifiManager = wifiManager;
	}
	
	public void connectToNetwork(ScanResult result, String password, Context context){
		new ConnectWifiTask(result, password, context).execute();
	}
	
	
	class ConnectWifiTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private ProgressDialog d;
		private WifiConfiguration config;
		private boolean success;
		private SupplicantState s;
		
		
		
		
		public ConnectWifiTask(ScanResult result, String password, Context context) {
			this.context = context;
			d = new ProgressDialog(context);
			
			config = new WifiConfiguration();
			//config.SSID = result.SSID;
			config.SSID = "\"" + result.SSID + "\"";

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
			
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			Log.d(TAG, "=========================");
			Log.d(TAG, config.toString());
			Log.d(TAG, "=========================");
			int netid = wifiManager.addNetwork(config);
			Log.d(TAG, "Netid: " + netid);
			success = wifiManager.enableNetwork(netid, true);
			
			int maxLoops = 10;
			int i = 0;
			while (i < maxLoops){
				s = wifiManager.getConnectionInfo().getSupplicantState();
				Log.d(TAG, s.name());
				if (s == SupplicantState.COMPLETED){
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
			}
			
			
			SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = preferences.edit();
			editor.remove("networkUUID");
			editor.commit();
			
			
			Intent intent = new Intent(context, NetworkService.class);
			intent.putExtra("action", "joinnetwork");
			context.startService(intent);
			
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
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
			    alertDialog.setTitle("Alert 2");
			    alertDialog.setMessage("This is another alert");
			    alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       return;
	                   }
	               });
			    alertDialog.create();
			    alertDialog.show();
			}
		}
	}
}

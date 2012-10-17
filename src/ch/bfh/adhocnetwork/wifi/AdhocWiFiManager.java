package ch.bfh.adhocnetwork.wifi;

import ch.bfh.adhocnetwork.AdhocNetworkListActivity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class AdhocWiFiManager {
	
	private static final String TAG = AdhocWiFiManager.class.getSimpleName();
	
	private WifiManager wifiManager;
	
	public AdhocWiFiManager(WifiManager wifiManager){
		this.wifiManager = wifiManager;
	}
	
	public void connectToNetwork(ScanResult result, Context context){
		new ConnectWifiTask(result, context).execute();
	}
	
	
	class ConnectWifiTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private ScanResult result;
		private ProgressDialog d;
		private WifiConfiguration config;
		private boolean success;
		
		public ConnectWifiTask(ScanResult result, Context context) {
			this.context = context;
			this.result = result;
			d = new ProgressDialog(context);
			
			String psk = "testtest";
			
			config = new WifiConfiguration();
			//config.SSID = result.SSID;
			config.SSID = "\"" + result.SSID + "\"";

			config.BSSID = result.BSSID;
			config.hiddenSSID = false;
			config.priority = 10000;
			config.preSharedKey = "\"".concat(psk).concat("\"");
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
				Toast.makeText(context, "Successfully connected to Network " + config.SSID + ".", Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(context, AdhocNetworkListActivity.class);
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

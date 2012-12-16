package ch.bfh.instacircle.wifi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

import android.net.wifi.WifiConfiguration;

public class SerializableWifiConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String BSSID;
	public String SSID;
	public BitSet allowedAuthAlgorithms;
	public BitSet allowedGroupCiphers;
	public BitSet allowedKeyManagement;
	public BitSet allowedPairwiseCiphers;
	public BitSet allowedProtocols;
	public boolean hiddenSSID;
	public String preSharedKey;
	public int priority;
	public int status;
	public String[] wepKeys;
	public int wepTxKeyIndex;



	public SerializableWifiConfiguration (WifiConfiguration wificonfiguration) {
		this.BSSID = wificonfiguration.BSSID;
		this.SSID = wificonfiguration.SSID;
		this.allowedAuthAlgorithms = wificonfiguration.allowedAuthAlgorithms;
		this.allowedGroupCiphers = wificonfiguration.allowedGroupCiphers;
		this.allowedKeyManagement = wificonfiguration.allowedKeyManagement;
		this.allowedPairwiseCiphers = wificonfiguration.allowedPairwiseCiphers;
		this.allowedProtocols = wificonfiguration.allowedProtocols;
		this.hiddenSSID = wificonfiguration.hiddenSSID;
		this.preSharedKey = wificonfiguration.preSharedKey;
		this.priority = wificonfiguration.priority;
		this.wepKeys = wificonfiguration.wepKeys;
		this.wepTxKeyIndex = wificonfiguration.wepTxKeyIndex;
	}
	
	public WifiConfiguration getWifiConfiguration() {
		WifiConfiguration config = new WifiConfiguration();
		if (this.BSSID == null){
			config.BSSID = "";
		}
		else {
			config.BSSID = this.BSSID;
		}
		config.SSID = this.SSID;
		config.allowedAuthAlgorithms = this.allowedAuthAlgorithms;
		config.allowedGroupCiphers = this.allowedGroupCiphers;
		config.allowedKeyManagement = this.allowedKeyManagement;
		config.allowedPairwiseCiphers = this.allowedPairwiseCiphers;
		config.allowedProtocols = this.allowedProtocols;
		config.hiddenSSID = this.hiddenSSID;
		config.preSharedKey = this.preSharedKey;
		config.priority = this.priority;
		config.wepKeys = this.wepKeys;
		config.wepTxKeyIndex = this.wepTxKeyIndex;

		return config;
	}
	
	@Override
	public String toString() {
		return "SerializableWifiConfiguration [BSSID=" + BSSID + ", SSID="
				+ SSID + ", allowedAuthAlgorithms=" + allowedAuthAlgorithms
				+ ", allowedGroupCiphers=" + allowedGroupCiphers
				+ ", allowedKeyManagement=" + allowedKeyManagement
				+ ", allowedPairwiseCiphers=" + allowedPairwiseCiphers
				+ ", allowedProtocols=" + allowedProtocols + ", hiddenSSID="
				+ hiddenSSID + ", preSharedKey="
				+ preSharedKey + ", priority=" + priority + ", status="
				+ status + ", wepKeys=" + Arrays.toString(wepKeys)
				+ ", wepTxKeyIndex=" + wepTxKeyIndex + "]";
	}
}

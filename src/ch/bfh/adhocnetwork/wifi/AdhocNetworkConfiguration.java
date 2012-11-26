package ch.bfh.adhocnetwork.wifi;

import java.io.Serializable;

public class AdhocNetworkConfiguration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private String ssid;
	private String password;
	private String key;
	
	
	public AdhocNetworkConfiguration(String ssid, String password, String key) {
		this.ssid = ssid;
		this.password = password;
		this.key = key;
	}


	public String getSsid() {
		return ssid;
	}


	public String getPassword() {
		return password;
	}


	public String getKey() {
		return key;
	}


	@Override
	public String toString() {
		return "AdhocNetworkConfiguration [ssid=" + ssid + ", password="
				+ password + ", key=" + key + "]";
	}
	
}

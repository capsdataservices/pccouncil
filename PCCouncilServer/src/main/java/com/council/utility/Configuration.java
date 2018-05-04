package com.council.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

	private String chromeDriverPath;
	private String chromeDriverLogFilePath;
	private String homePageURL;
	private String adBlockPath;
	private String proxy;
	private String chromeVerboseLogging;
	private String ipifyURL;
	private String display;

	public String getChromeDriverPath() {
		return chromeDriverPath;
	}

	public void setChromeDriverPath(String chromeDriverPath) {
		this.chromeDriverPath = chromeDriverPath;
	}

	public String getChromeDriverLogFilePath() {
		return chromeDriverLogFilePath;
	}

	public void setChromeDriverLogFilePath(String chromeDriverLogFilePath) {
		this.chromeDriverLogFilePath = chromeDriverLogFilePath;
	}

	public String getHomePageURL() {
		return homePageURL;
	}

	public void setHomePageURL(String homePageURL) {
		this.homePageURL = homePageURL;
	}

	public String getAdBlockPath() {
		return adBlockPath;
	}

	public void setAdBlockPath(String adBlockPath) {
		this.adBlockPath = adBlockPath;
	}

	public String getProxy() {
		return proxy;
	}

	public void setProxy(String proxy) {
		this.proxy = proxy;
	}

	public String getChromeVerboseLogging() {
		return chromeVerboseLogging;
	}

	public void setChromeVerboseLogging(String chromeVerboseLogging) {
		this.chromeVerboseLogging = chromeVerboseLogging;
	}

	public String getIpifyURL() {
		return ipifyURL;
	}

	public void setIpifyURL(String ipifyURL) {
		this.ipifyURL = ipifyURL;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public void loadProperties() {

		Properties properties = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("config//config.properties");
			properties.load(input);

			setChromeDriverPath(properties.getProperty("chromeDriver"));
			setChromeDriverLogFilePath(properties.getProperty("chromeDriverLogFile"));
			setChromeVerboseLogging(properties.getProperty("chromeVerboseLogging"));
			setAdBlockPath(properties.getProperty("adBlockPath"));
			setIpifyURL(properties.getProperty("ipifyURL"));
			setProxy(properties.getProperty("proxy"));
			setDisplay(properties.getProperty("display"));

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

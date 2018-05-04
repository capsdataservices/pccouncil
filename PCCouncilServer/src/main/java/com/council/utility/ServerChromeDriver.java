package com.council.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.google.common.collect.ImmutableMap;

public class ServerChromeDriver {

	private static WebDriver driver;
	private static ChromeDriverService service;

	private static Logger logger = Logger.getLogger(ServerChromeDriver.class);

	public WebDriver getDriver(URL serviceUrl) {

		Configuration configuration = new Configuration();
		configuration.loadProperties();
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--headless");
		chromeOptions.addArguments("--disable-gpu");
		chromeOptions.addArguments("start-maximized");
		// chromeOptions.addExtensions(new File(configuration.getAdBlockPath()));

		Proxy proxy = new Proxy();
		proxy.setAutodetect(false);
		proxy.setProxyType(ProxyType.MANUAL);
		proxy.setHttpProxy(configuration.getProxy());
		proxy.setFtpProxy(configuration.getProxy());
		proxy.setSslProxy(configuration.getProxy());

		System.setProperty("webdriver.chrome.driver", configuration.getChromeDriverPath());
		System.setProperty("webdriver.chrome.logfile", configuration.getChromeDriverLogFilePath());
		System.setProperty("webdriver.chrome.verboseLogging", configuration.getChromeVerboseLogging());

		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		capabilities.setJavascriptEnabled(true);
		capabilities.setCapability(CapabilityType.PROXY, proxy);

		try {
			driver = new RemoteWebDriver(serviceUrl, capabilities);
		} catch (Exception e) {
			logger.error("Error creating a new chrome instance");
			throw new RuntimeException(e.getMessage());
		}
		driver.manage().timeouts().implicitlyWait(180, TimeUnit.SECONDS);

		return driver;
	}

	public String getHostName(WebDriver driver) {

		driver.get("http://api.ipify.org/");
		String hostName = Jsoup.parse(driver.getPageSource()).body().text();
		return hostName;
	}

	public ChromeDriverService loadService() throws Exception {

		Configuration configuration = new Configuration();
		configuration.loadProperties();

		Properties props = new Properties();
		try {
			props.load(new FileInputStream("config//log4j.properties"));
		} catch (IOException e) {
			logger.error(e);
		}
		PropertyConfigurator.configure(props);

		service = new ChromeDriverService.Builder().usingDriverExecutable(new File(configuration.getChromeDriverPath()))
				.usingAnyFreePort().withEnvironment(ImmutableMap.of("DISPLAY", configuration.getDisplay())).build();
		service.start();

		return service;
	}

}

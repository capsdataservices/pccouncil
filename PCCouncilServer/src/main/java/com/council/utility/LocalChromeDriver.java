package com.council.utility;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class LocalChromeDriver {

	private static Logger logger = Logger.getLogger(LocalChromeDriver.class);

	private static WebDriver driver;

	public WebDriver getDriver() {

		ChromeOptions chromeOptions = new ChromeOptions();
		// chromeOptions.addArguments("--headless");
		// chromeOptions.addArguments("--disable-gpu");
		// chromeOptions.addExtensions(new File(Constant.AD_BLOCK_PATH));

		Proxy proxy = new Proxy();
		proxy.setHttpProxy("uk.proxymesh.com:31280");
		proxy.setFtpProxy("uk.proxymesh.com:31280");
		proxy.setSslProxy("uk.proxymesh.com:31280");

		System.setProperty(Constant.CHROME_DRIVER, Constant.CHROME_DRIVER_PATH);

		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
//		capabilities.setCapability(CapabilityType.PROXY, proxy);
		capabilities.setJavascriptEnabled(true);

		try {
			driver = new ChromeDriver(capabilities);
		} catch (Exception e) {
			logger.error("Error creating a new chrome instance");
			throw new RuntimeException(e.getMessage());
		}
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		return driver;
	}

	public String getHostName(WebDriver driver) {

		driver.get("http://api.ipify.org/");
		String hostName = Jsoup.parse(driver.getPageSource()).body().text();
		return hostName;
	}
}

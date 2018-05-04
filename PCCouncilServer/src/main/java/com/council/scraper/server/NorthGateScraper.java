package com.council.scraper.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import com.council.utility.DBOperations;
import com.council.utility.ScreenShot;
import com.council.utility.ServerChromeDriver;

public class NorthGateScraper {

	private static Logger logger = Logger.getLogger(NorthGateScraper.class);

	private static final Proxy proxy = new Proxy(Proxy.Type.HTTP,
			InetSocketAddress.createUnresolved("46.101.40.23", 31280));
		
	private static int number = 2;
	
	public void extractData(PlanningPortal portal) throws Exception {

		WebDriver driver = null;
		ChromeDriverService service = null;

		try {

			ServerChromeDriver serverChromeDriver = new ServerChromeDriver();
			service = serverChromeDriver.loadService();
			driver = serverChromeDriver.getDriver(service.getUrl());


			String hostName = serverChromeDriver.getHostName(driver);
			logger.info("Running the application on host: " + hostName);

			getApplicationsOfType("Validated in this week", driver, portal);

			getApplicationsOfType("Decided in this week", driver, portal);

			DBOperations.updateNPortalStatus(portal, "COMPLETED");
			
		} catch (Exception e) {
			portal.setMessage(e.getMessage());
			portal.setStatus("TERMINATED");
			DBOperations.updateNPortalToError(portal, e.getMessage());
			ScreenShot.takeScreenShot(driver, portal, "error");
			logger.error("Business Object : " + portal.toString());
			logger.error("Error Occurred in Scraper: " + e);
		} finally {
			driver.close();
			driver.quit();
			service.stop();
			logger.info("Quitting the driver and closing every associated window.");
		}
	}
	public void getApplicationsOfType(String type, WebDriver driver, PlanningPortal portal)
			throws InterruptedException, IOException, ParseException {

		driver.get(portal.getURL());

		if (type.equals("Validated in this week")) {
			selectDateValidated(driver);
		} else if (type.equals("Decided in this week")) {
			selectDateDecided(driver);
		}

		selectWeek(driver);

		submitSearch(driver);

		getApplications(driver, type, portal);
	}

	public void selectDateValidated(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cboSelectDateValue")));

		wait.until((ExpectedCondition<Boolean>) new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				Select select = new Select(driver.findElement(By.id("cboSelectDateValue")));
				return select.getOptions().size() > 1;
			}
		});

		Select selectDateValidated = new Select(driver.findElement(By.id("cboSelectDateValue")));
		selectDateValidated.selectByValue("DATE_VALID");
	}

	public void selectDateDecided(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cboSelectDateValue")));

		wait.until((ExpectedCondition<Boolean>) new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				Select select = new Select(driver.findElement(By.id("cboSelectDateValue")));
				return select.getOptions().size() > 1;
			}
		});

		Select selectDateDecided = new Select(driver.findElement(By.id("cboSelectDateValue")));
		selectDateDecided.selectByValue("DATE_DECISION");
	}

	public void selectWeek(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("rbRange")));

		WebElement rangeButton = driver.findElement(By.id("rbRange"));
		rangeButton.click();

		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dateStart")));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dateEnd")));

		WebElement startDate = driver.findElement(By.id("dateStart"));
		startDate.clear();
		startDate.sendKeys(getStartDate());

		WebElement endDate = driver.findElement(By.id("dateEnd"));
		endDate.clear();
		endDate.sendKeys(getTodaysDate());

	}

	private String getStartDate() {
		return "27-02-2018";
	}

	private String getTodaysDate() {
		return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
	}

	public void submitSearch(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("csbtnSearch")));

		WebElement searchButton = driver.findElement(By.id("csbtnSearch"));
		searchButton.click();
	}

	@SuppressWarnings("unused")
	public void getApplications(WebDriver driver, String type, PlanningPortal portal)
			throws InterruptedException, IOException, ParseException {
		String pageSource = driver.getPageSource();

		Elements searchResults = Jsoup.parse(pageSource, portal.getURL()).select("table > tbody > tr");
		if (searchResults.size() > 1) {
			searchResults.remove(0);
		}
		// > td[class='TableData']

		for (Element result : searchResults) {

			Application data = new Application();

			String applicationNumber = result.select("td[class='TableData'] > a").text();
			String linkPageSource = getLinkPageSource(driver, applicationNumber, data);

			data.setAuthority(portal.getAuthority());
			data.setType(type);
			data.setStatus(result.select("td:nth-child(4)").text());
			data.setReceived(getDate(result.select("td:nth-child(5)").text()));
			data.setDecision(result.select("td:nth-child(6)").text());

			processUrl(data, linkPageSource);

		}

		String nextPageSelector = "a[title='Goto Page " + number + "']";
		if (driver.findElements(By.cssSelector(nextPageSelector)).size() > 0
				&& driver.findElement(By.cssSelector(nextPageSelector)).isDisplayed()) {
			clickNext(driver);
			number = number + 1;
			getApplications(driver, type, portal);
		}

		number = 2;
	}

	private String getLinkPageSource(WebDriver driver, String applicationNumber, Application data) {

		String linkPageSource = "";
		WebElement link = driver.findElement(By.linkText(applicationNumber));
		link.click();

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".page-content > div > ul > li")));
		
		data.setURL(driver.getCurrentUrl());
		linkPageSource = driver.getPageSource();

		driver.navigate().back();
		
		return linkPageSource;
	}

	public void clickNext(WebDriver driver) throws InterruptedException {

		String nextPageSelector = "a[title='Goto Page " + number + "']";
		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(nextPageSelector)));

		WebElement nextPageLink = driver.findElements(By.cssSelector(nextPageSelector)).get(0);
		Thread.sleep(3000);
		nextPageLink.click();
		logger.info("Navigating to next page");
	}

	private void processUrl(Application data, String linkPageSource) throws IOException, ParseException {

		logger.info("Processing URL : " + data.getURL());

		Document document = Jsoup.parse(linkPageSource);
		List<Element> rows = document.select(".page-content > div > ul > li");
		rows.remove(0);

		for (Element row : rows) {

			String head = row.select("span").text().trim();
			String tableData = row.text().replaceAll(head, "").trim();

			if (head.equalsIgnoreCase("Application Number")) {
				data.setReferenceNumber(tableData);
			} else if (head.equalsIgnoreCase("Application Type")) {
				data.setApplicationType(tableData);
			} else if (head.equalsIgnoreCase("Site Address")) {
				data.setAddress(tableData);
			} else if (head.equalsIgnoreCase("Proposal")) {
				data.setProposal(tableData);
			} else if (head.equalsIgnoreCase("Planning Officer")) {
				data.setCaseOfficer(tableData);
			} else if (head.equalsIgnoreCase("Applicant")) {
				data.setApplicantName(tableData);
			} else if (head.equalsIgnoreCase("Ward")) {
				data.setWard(tableData);
			} else if (head.equalsIgnoreCase("Valid From")) {
				data.setValidated(getDate(tableData));
			} else if (head.equalsIgnoreCase("Appeal Decision Date")) {
				data.setDecisionMadeDate(getDate(tableData));
			}

		}

		data.setScrapedOn(new Date());

		String locationURL = constructLocationURL(data.getAddress());
		data.setLocationURL(locationURL);

		DBOperations.updateApplicationData(data);
	}

	private String constructLocationURL(String address) {

		String locationURL = null;

		if (address == null || address.equals("") || address.toLowerCase().equals("not available")
				|| address.toLowerCase().equals("not applicable")) {
			logger.info("Address not present");
		} else {
			String URL = "https://www.google.com/maps/search/";
			String charset = "UTF-8";
			try {
				locationURL = URL + URLEncoder.encode(address, charset);
			} catch (UnsupportedEncodingException e) {
				logger.error("Error occured : " + e);
			}
		}
		return locationURL;
	}

	private Date getDate(String tableData) throws ParseException {

		tableData = tableData.toLowerCase();

		if (tableData == null || tableData.contains("not available") || tableData.equals("-")
				|| tableData.contains("not applicable") || tableData.equalsIgnoreCase("")
				|| tableData.equalsIgnoreCase(".")) {
			return null;
		} else {
			Date date = getDMYShortDate(tableData);
			if (date == null) {
				date = getDMYDate(tableData);
				if (date == null) {
					date = getWDMYDate(tableData);
					if (date == null) {
						logger.warn("Unknown date format : " + tableData);
					}
				}
			}
			return date;
		}
	}

	private Date getDMYShortDate(String tableData) {
		try {
			return new SimpleDateFormat("dd-MM-yyyy").parse(tableData);
		} catch (ParseException e) {
			return null;
		}
	}

	private Date getDMYDate(String tableData) {
		try {
			return new SimpleDateFormat("dd MMM yyyy").parse(tableData);
		} catch (ParseException e) {
			return null;
		}
	}

	private Date getWDMYDate(String tableData) {
		try {
			return new SimpleDateFormat("E dd MMM yyyy").parse(tableData);
		} catch (ParseException e) {
			return null;
		}
	}

}


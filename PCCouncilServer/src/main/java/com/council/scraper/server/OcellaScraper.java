package com.council.scraper.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import com.council.utility.DBOperations;
import com.council.utility.ScreenShot;
import com.council.utility.ServerChromeDriver;
import com.council.utility.SessionManager;

public class OcellaScraper {

	private static Logger logger = Logger.getLogger(OcellaScraper.class);

	private static final Proxy proxy = new Proxy(Proxy.Type.HTTP,
			InetSocketAddress.createUnresolved("46.101.40.23", 31280));

	public void extractData(PlanningPortal portal) throws Exception {

		WebDriver driver = null;
		ChromeDriverService service = null;

		try {

			ServerChromeDriver serverChromeDriver = new ServerChromeDriver();
			service = serverChromeDriver.loadService();
			driver = serverChromeDriver.getDriver(service.getUrl());

			String hostName = serverChromeDriver.getHostName(driver);
			logger.info("Running the application on host: " + hostName);

			getApplicationsOfType("Received in this week", driver, portal);

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

		dismissButton(driver);

		if (type.equals("Received in this week")) {
			selectDateReceived(driver);
		} else if (type.equals("Decided in this week")) {
			selectDateDecided(driver);
		}

		submitSearch(driver);

		showAllResults(driver);

		getApplications(driver, type, portal);
	}

	private void dismissButton(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);

		if (driver.findElements(By.cssSelector(".cc-btn.cc-dismiss")).size() > 0
				&& driver.findElement(By.cssSelector(".cc-btn.cc-dismiss")).isDisplayed()) {

			wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".cc-btn.cc-dismiss")));

			WebElement dismissButton = driver.findElement(By.cssSelector(".cc-btn.cc-dismiss"));
			dismissButton.click();
		}
	}

	public void selectDateReceived(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("receivedFrom")));

		WebElement dateReceivedFromText = driver.findElement(By.id("receivedFrom"));
		dateReceivedFromText.clear();
		dateReceivedFromText.sendKeys(getStartDate());

		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("receivedTo")));

		WebElement dateReceivedToText = driver.findElement(By.id("receivedTo"));
		dateReceivedToText.clear();
		dateReceivedToText.sendKeys(getTodaysDate());

	}

	public void selectDateDecided(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);

		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("decidedFrom")));
		WebElement dateReceivedFromText = driver.findElement(By.id("decidedFrom"));
		dateReceivedFromText.clear();
		dateReceivedFromText.sendKeys(getStartDate());

		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("decidedTo")));

		WebElement dateReceivedToText = driver.findElement(By.id("decidedTo"));
		dateReceivedToText.clear();
		dateReceivedToText.sendKeys(getTodaysDate());
	}

	public void submitSearch(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='submit'][value='Search']")));

		if (driver.findElements(By.cssSelector("input[type='submit'][value='Search']")).size() > 0
				&& driver.findElement(By.cssSelector("input[type='submit'][value='Search']")).isDisplayed()) {

			wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='submit'][value='Search']")));

			WebElement searchButton = driver.findElement(By.cssSelector("input[type='submit'][value='Search']"));
			searchButton.click();
		}
	}

	private void showAllResults(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);

		if (driver.findElements(By.cssSelector("input[type='submit'][value='Show all results']")).size() > 0
				&& driver.findElement(By.cssSelector("input[type='submit'][value='Show all results']")).isDisplayed()) {

			wait.until(ExpectedConditions
					.elementToBeClickable(By.cssSelector("input[type='submit'][value='Show all results']")));

			WebElement searchButton = driver
					.findElement(By.cssSelector("input[type='submit'][value='Show all results']"));
			searchButton.click();
		}
	}

	public void getApplications(WebDriver driver, String type, PlanningPortal portal)
			throws InterruptedException, IOException, ParseException {
		String pageSource = driver.getPageSource();

		Elements searchResults = Jsoup.parse(pageSource, portal.getURL()).select("table > tbody > tr > td > a");

		for (Element result : searchResults) {

			String applicationURL = result.absUrl("href");

			processUrl(applicationURL, type, portal.getAuthority());

		}
	}

	private void processUrl(String applicationURL, String type, String authority) throws IOException, ParseException {

		logger.info("Processing URL : " + applicationURL);

		Document document = Jsoup.connect(applicationURL).proxy(proxy).timeout(10 * 1000).validateTLSCertificates(false)
				.get();
		List<Element> rows = document.select("table > tbody > tr");
		rows.remove(0);

		Application data = new Application();
		data.setAuthority(authority);
		data.setType(type);
		data.setURL(applicationURL);

		for (Element row : rows) {

			String head = row.select("td:nth-child(1)").text().trim();
			String tableData = row.select("td:nth-child(2)").text().trim();

			if (head.equalsIgnoreCase("Reference") || head.equalsIgnoreCase("Application Reference")) {
				data.setReferenceNumber(tableData);
			} else if (head.equalsIgnoreCase("Status")) {
				data.setStatus(tableData);
			} else if (head.equalsIgnoreCase("Proposal")) {
				data.setProposal(tableData);
			} else if (head.equalsIgnoreCase("Location") || head.equalsIgnoreCase("Address")) {
				data.setAddress(tableData);
			} else if (head.equalsIgnoreCase("Ward")) {
				data.setWard(tableData);
			} else if (head.equalsIgnoreCase("Application Validated") || head.equalsIgnoreCase("Validated")) {
				data.setValidated(getDate(tableData));
			} else if (head.equalsIgnoreCase("Application Received") || head.equalsIgnoreCase("Received")) {
				data.setRecieved(getDate(tableData));
			} else if (head.equalsIgnoreCase("Case Officer")) {
				data.setCaseOfficer(tableData);
			} else if (head.equalsIgnoreCase("Applicant")) {
				data.setApplicantName(tableData);
				data.setApplicantAddress(tableData);
			} else if (head.equalsIgnoreCase("Agent")) {
				data.setAgentName(tableData);
				data.setAgentAddress(tableData);
			} else if (head.equalsIgnoreCase("Decision By")) {
				data.setDecisionMadeDate(getDate(tableData));
			} else if (head.equalsIgnoreCase("Decided")) {
				data.setDecisionIssuedOn(getDate(tableData));
			}

		}

		data.setScrapedOn(new Date());

		String locationURL = constructLocationURL(data.getAddress());
		data.setLocationURL(locationURL);

		updateApplicationData(data);
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
			Date date = getWDMYDate(tableData);
			if (date == null) {
				date = getDMYDate(tableData);
				if (date == null) {
					date = getDMYShortDate(tableData);
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
			return new SimpleDateFormat("dd-MM-yy").parse(tableData);
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

	private String getStartDate() {
		return "20-02-18";
	}

	private String getTodaysDate() {
		return new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(new Date());
	}

	private String getMondayDate() {

		LocalDate today = LocalDate.now();
		LocalDate monday = today;
		while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
			monday = monday.minusDays(1);
		}

		Date date = Date.from(monday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
		return new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(date);
	}

	public void updateApplicationData(Application data) {

		Session session = SessionManager.getSessionFactory().openSession();
		session.beginTransaction();

		Query query = session.createQuery(
				"from Application where authority = :authority and reference = :reference and type = :type");
		query.setParameter("authority", data.getAuthority());
		query.setParameter("reference", data.getReferenceNumber());
		query.setParameter("type", data.getType());
		List list = query.list();
		if (list.isEmpty()) {
			session.save(data);
			logger.info("Inserting new application");
		} else {
			logger.info("Application already exists");
		}
		session.getTransaction().commit();
	}
}

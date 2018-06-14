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
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import com.council.utility.DBOperations;
import com.council.utility.LocalChromeDriver;
import com.council.utility.ScreenShot;
import com.council.utility.ServerChromeDriver;
import com.council.utility.SessionManager;

public class IdoxScraper {

	private static Logger logger = Logger.getLogger(IdoxScraper.class);

	private static final Proxy proxy = new Proxy(Proxy.Type.HTTP,
			InetSocketAddress.createUnresolved("46.101.40.23", 31280));

	private static String OS = System.getProperty("os.name").toLowerCase();

	public void extractData(PlanningPortal portal) throws Exception {

		WebDriver driver = null;
		ChromeDriverService service = null;

		boolean isWindows = OS.indexOf("win") >= 0;

		logger.info("operating System : " + OS);

		if (!isWindows) {
			service = new ServerChromeDriver().loadService();
		}

		try {

			driver = getPIDriver(service, isWindows);

			getApplicationsOfType("Validated in this week", driver, portal);

			getApplicationsOfType("Decided in this week", driver, portal);

			DBOperations.updateNPortalStatus(portal, "COMPLETED");

		} catch (Exception e) {

			portal.setMessage(e.getMessage());
			portal.setStatus("TERMINATED");

			DBOperations.updateNPortalToError(portal, e.getMessage());
			if (!isWindows) {
				ScreenShot.takeScreenShot(driver, portal, "error");
			}

			logger.error("Business Object : " + portal.toString());
			logger.error("Error Occurred in Scraper: " + e);

		} finally {

			driver.close();
			driver.quit();
			if (!isWindows) {
				service.stop();
			}
			logger.info("Quitting the driver and closing every associated window.");
		}
	}

	private WebDriver getPIDriver(ChromeDriverService service, boolean isWindows) {

		WebDriver driver;

		if (isWindows) {
			driver = new LocalChromeDriver().getDriver();
		} else {
			driver = new ServerChromeDriver().getDriver(service.getUrl());
		}

		String hostName = new ServerChromeDriver().getHostName(driver);
		logger.info("Running the application on host: " + hostName);

		return driver;
	}

	public void getApplicationsOfType(String type, WebDriver driver, PlanningPortal portal)
			throws InterruptedException, IOException, ParseException {

		driver.get(portal.getURL());

		selectWeek(driver);

		if (type.equals("Validated in this week")) {
			selectDateValidated(driver);
		} else if (type.equals("Decided in this week")) {
			selectDateDecided(driver);
		}

		submitSearch(driver);

		getApplications(driver, type, portal);
	}

	public void getApplications(WebDriver driver, String type, PlanningPortal portal)
			throws InterruptedException, IOException, ParseException {
		String pageSource = driver.getPageSource();

		Elements searchResults = Jsoup.parse(pageSource, portal.getURL()).select(".col-a > ul > li");

		for (Element result : searchResults) {

			String applicationURL = result.select("a").get(0).absUrl("href");

			processUrl(applicationURL, type, portal.getAuthority());

		}

		if (driver.findElements(By.cssSelector(".pager.bottom > .next")).size() > 0
				&& driver.findElement(By.cssSelector(".pager.bottom > .next")).isDisplayed()) {
			clickNext(driver);
			getApplications(driver, type, portal);
		}
	}

	private void processUrl(String applicationURL, String type, String authority) throws IOException, ParseException {

		logger.info("Processing URL : " + applicationURL);

		// String userAgent = "Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3)
		// Gecko/20040924 Epiphany/1.4.4 (Ubuntu)";
		Document document = Jsoup.connect(applicationURL).proxy(proxy).timeout(10 * 1000).validateTLSCertificates(false)
				.get();
		List<Element> rows = document.select(".tabcontainer > table > tbody > tr");

		Application data = new Application();
		data.setAuthority(authority);
		data.setType(type);
		data.setURL(applicationURL);

		String proposal = document.select(".description").text();
		data.setProposal(proposal);

		for (Element row : rows) {

			String head = row.select("th").text().replace(':', ' ').trim();
			String tableData = row.select("td").text();

			if (head.equalsIgnoreCase("Reference") || head.equalsIgnoreCase("Application Reference")) {
				data.setReferenceNumber(tableData);
			} else if (head.equalsIgnoreCase("Alternative Reference")) {
				data.setAltReferenceNumber(tableData);
			} else if (head.equalsIgnoreCase("Application Validated")) {
				data.setValidated(getDate(tableData));
			} else if (head.equalsIgnoreCase("Application Received")) {
				data.setRecieved(getDate(tableData));
			} else if (head.equalsIgnoreCase("Address") || head.equalsIgnoreCase("Location")) {
				data.setAddress(tableData);
			} else if (head.equalsIgnoreCase("Status")) {
				data.setStatus(tableData);
			} else if (head.equalsIgnoreCase("Decision")) {
				data.setDecision(tableData);
			} else if (head.equalsIgnoreCase("Decision Issued Date")) {
				data.setDecisionIssuedOn(getDate(tableData));
			}
		}

		String locationURL = constructLocationURL(data.getAddress());
		data.setLocationURL(locationURL);

		applicationURL = applicationURL.replace("activeTab=summary", "activeTab=details");
		processFurtherInformation(applicationURL, data);
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
		if (tableData == null || tableData.equalsIgnoreCase("Not Available") || tableData.equals("-")
				|| tableData.equalsIgnoreCase("Not Applicable") || tableData.equalsIgnoreCase("")
				|| tableData.equalsIgnoreCase(".")) {
			return null;
		} else {
			Date date = getWDMYDate(tableData);
			if (date == null) {
				date = getDMYDate(tableData);
				if (date == null) {
					logger.warn("Unknown date format : " + tableData);
				}
			}
			return date;
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

	private void processFurtherInformation(String applicationURL, Application data) throws IOException, ParseException {

		logger.info("Processing further information for URL : " + applicationURL);

		Document document = Jsoup.connect(applicationURL).proxy(proxy).timeout(10 * 1000).validateTLSCertificates(false)
				.get();
		List<Element> rows = document.select(".tabcontainer > table > tbody > tr");

		for (Element row : rows) {

			String head = row.select("th").text().replaceAll(":", "").trim();
			String tableData = row.select("td").text();

			if (head.equalsIgnoreCase("Application Type")) {
				data.setApplicationType(tableData);
			} else if (head.equalsIgnoreCase("Case Officer")) {
				data.setCaseOfficer(tableData);
			} else if (head.equalsIgnoreCase("Ward")) {
				data.setWard(tableData);
			} else if (head.equalsIgnoreCase("Applicant Name")) {
				data.setApplicantName(tableData);
			} else if (head.equalsIgnoreCase("Applicant Address")) {
				data.setApplicantAddress(tableData);
			} else if (head.equalsIgnoreCase("Agent Name")) {
				data.setAgentName(tableData);
			} else if (head.equalsIgnoreCase("Agent Company Name")) {
				data.setAgentCompanyName(tableData);
			} else if (head.equalsIgnoreCase("Agent Address")) {
				data.setAgentAddress(tableData);
			} else if (head.equalsIgnoreCase("Agent Phone Number")) {
				if (!(tableData.matches(".*[a-z].*"))) {
					data.setAgentPhoneNumber(tableData);
				}
			}
		}

		applicationURL = applicationURL.replace("activeTab=details", "activeTab=contacts");
		processContacts(applicationURL, data);
	}

	private void processContacts(String applicationURL, Application data) throws IOException, ParseException {

		logger.info("Processing contacts for URL : " + applicationURL);

		Document document = Jsoup.connect(applicationURL).proxy(proxy).timeout(10 * 1000).validateTLSCertificates(false)
				.get();

		if (data.getAgentName() == null || data.getAgentName().isEmpty()) {
			String name = document.select(".tabcontainer > .agents > p").text();
			data.setAgentName(name);
		}

		List<Element> rows = document.select(".tabcontainer > .agents > .agents > tbody > tr");
		for (Element row : rows) {

			String head = row.select("th").text().replaceAll(":", "").trim();
			String tableData = row.select("td").text();

			if (head.equalsIgnoreCase("Email") || head.equalsIgnoreCase("Personal Email")) {
				data.setAgentEmail(tableData);
			} else if (head.equalsIgnoreCase("Phone") || head.equalsIgnoreCase("Mobile Phone")
					|| head.equalsIgnoreCase("Personal Mobile") || head.equalsIgnoreCase("Personal Phone")) {
				if (!(tableData.toLowerCase().matches(".*[a-z].*"))) {
					data.setAgentPhoneNumber(tableData);
				}
			}
		}

		applicationURL = applicationURL.replace("activeTab=contacts", "activeTab=dates");
		processDates(applicationURL, data);
	}

	private void processDates(String applicationURL, Application data) throws IOException, ParseException {

		logger.info("Processing dates for URL : " + applicationURL);

		Document document = Jsoup.connect(applicationURL).proxy(proxy).timeout(10 * 1000).validateTLSCertificates(false)
				.get();
		List<Element> rows = document.select(".tabcontainer > table > tbody > tr");

		for (Element row : rows) {

			String head = row.select("th").text().replaceAll(":", "").trim();
			String tableData = row.select("td").text();

			if (head.equalsIgnoreCase("Decision Made Date")) {
				data.setDecisionMadeDate(getDate(tableData));
			} else if (head.equalsIgnoreCase("Permission Expiry Date")) {
				data.setPermissionExpiryDate(getDate(tableData));
			} else if (head.equalsIgnoreCase("Application Validated")) {
				data.setValidated(getDate(tableData));
			} else if (head.equalsIgnoreCase("Application Received")) {
				data.setRecieved(getDate(tableData));
			}
		}

		data.setScrapedOn(new Date());
		updateApplicationData(data);
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

	public void clickNext(WebDriver driver) throws InterruptedException {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pager.bottom > .next")));

		WebElement nextPageLink = driver.findElement(By.cssSelector(".next"));
		Thread.sleep(3000);
		nextPageLink.click();
		logger.info("Navigating to next page");
	}

	public void submitSearch(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".button.primary")));

		WebElement searchButton = driver.findElement(By.cssSelector(".button.primary"));
		searchButton.click();
	}

	public void selectWeek(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("week")));

		wait.until((ExpectedCondition<Boolean>) new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				Select select = new Select(driver.findElement(By.id("week")));
				return select.getOptions().size() > 1;
			}
		});

		Select selectWeek = new Select(driver.findElement(By.id("week")));
		selectWeek.selectByIndex(0);
	}

	public void selectDateValidated(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dateValidated")));

		WebElement dateValidatedButton = driver.findElement(By.id("dateValidated"));
		dateValidatedButton.click();

	}

	public void selectDateDecided(WebDriver driver) {

		WebDriverWait wait = new WebDriverWait(driver, 600);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dateDecided")));

		WebElement dateValidatedButton = driver.findElement(By.id("dateDecided"));
		dateValidatedButton.click();
	}

}

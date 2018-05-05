package com.council.scraper;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import com.council.utility.*;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseScraper implements Scraper {

    private static Logger logger = Logger.getLogger(BaseScraper.class);

    private ScraperType scraperType;
    private LocalChromeDriver localChromeDriver;
    private ServerChromeDriver serverChromeDriver;
    private ChromeDriverService chromeDriverService;
    private WebDriver webDriver;

    public BaseScraper(ScraperType scraperType) {
        this.scraperType = scraperType;
    }

    protected WebDriver getWebDriver() throws Exception {

        // local chrome driver
        if(localChromeDriver == null && scraperType.equals(ScraperType.LOCAL)) {
            localChromeDriver = new LocalChromeDriver();
            webDriver = localChromeDriver.getDriver();
            return webDriver;
        } else if(localChromeDriver != null && scraperType.equals(ScraperType.LOCAL)) {
            return webDriver;
        }

        // server chrome driver
        if(serverChromeDriver == null && scraperType.equals(ScraperType.SERVER)) {
            serverChromeDriver = new ServerChromeDriver();
            if(chromeDriverService == null) {
                chromeDriverService = serverChromeDriver.loadService();
            }
            webDriver = serverChromeDriver.getDriver(chromeDriverService.getUrl());
            return webDriver;
        } else if(serverChromeDriver != null && scraperType.equals(ScraperType.SERVER)) {
            if(chromeDriverService == null) {
                chromeDriverService = serverChromeDriver.loadService();
            }
            return webDriver;
        }

        return null;
    }

    protected String constructLocationURL(String address) {

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

    protected String getHostName(WebDriver driver) throws Exception {
        if (localChromeDriver != null) {
            return localChromeDriver.getHostName(driver);
        } else {
            return serverChromeDriver.getHostName(driver);
        }
    }

    protected void openPortalUrl(PlanningPortal portal) throws Exception {
        getWebDriver().get(portal.getURL());
    }

    protected void openUrl(String url) throws Exception {
        getWebDriver().get(url);
    }

    protected Boolean isElementDisplayed(String cssSelector) throws Exception {
        if (getWebDriver().findElements(By.cssSelector(cssSelector)).size() > 0
                && getWebDriver().findElement(By.cssSelector(cssSelector)).isDisplayed()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    protected Object waitUntil(ExpectedCondition<?> condition, long timeoutInSeconds) throws Exception {
        WebDriverWait wait = new WebDriverWait(getWebDriver(), timeoutInSeconds);
        return wait.until(condition);
    }

    protected Object waitUntilElementPresent(String cssSelector, long timeoutInSeconds) throws Exception {
        return waitUntil(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)), 600);
    }

    protected void processUrl(String url, Proxy proxy, Integer timeoutInMs, Consumer<Document> consumer) throws IOException {
        Connection connection;
        if(proxy != null) {
            connection = Jsoup.connect(url).proxy(proxy).validateTLSCertificates(false);
        } else {
            connection = Jsoup.connect(url).validateTLSCertificates(false);
        }
        if(timeoutInMs != null) {
            connection = connection.timeout(timeoutInMs);
        }
        consumer.accept(connection.get());
    }

    protected void updateApplicationData(Application data) {
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

    @Override
    public void extractData(PlanningPortal portal) throws Exception {
        WebDriver driver = null;
        try {
            driver = getWebDriver();
            String hostName = getHostName(driver);
            logger.info("Running the application on host: " + hostName);
            runDataExtraction(driver, portal);
            DBOperations.updateNPortalStatus(portal, "COMPLETED");
        } catch (Exception e) {
            portal.setMessage(e.getMessage());
            portal.setStatus("TERMINATED");
            DBOperations.updateNPortalToError(portal, e.getMessage());
            ScreenShot.takeScreenShot(driver, portal, "error");
            logger.error("Business Object : " + portal.toString());
            logger.error("Error Occurred in Scraper: " + e);
        } finally {
            if(driver != null) {
                driver.close();
                driver.quit();
            }
            if(chromeDriverService != null) {
                chromeDriverService.stop();
            }
            logger.info("Quitting the driver and closing every associated window.");
        }
    }

    /**
     * This method will extract all the details from the website
     * @param driver
     * @param portal
     * @throws Exception
     */
    abstract void runDataExtraction(WebDriver driver, PlanningPortal portal) throws Exception;

    public ScraperType getScraperType() {
        return scraperType;
    }

    public void setScraperType(ScraperType scraperType) {
        this.scraperType = scraperType;
    }

    public LocalChromeDriver getLocalChromeDriver() {
        return localChromeDriver;
    }

    public void setLocalChromeDriver(LocalChromeDriver localChromeDriver) {
        this.localChromeDriver = localChromeDriver;
    }

    public ServerChromeDriver getServerChromeDriver() {
        return serverChromeDriver;
    }

    public void setServerChromeDriver(ServerChromeDriver serverChromeDriver) {
        this.serverChromeDriver = serverChromeDriver;
    }

    public ChromeDriverService getChromeDriverService() {
        return chromeDriverService;
    }

    public void setChromeDriverService(ChromeDriverService chromeDriverService) {
        this.chromeDriverService = chromeDriverService;
    }
}

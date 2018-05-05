package com.council.scraper;

import com.council.entity.PlanningPortal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class IctConnectScraper extends BaseScraper {

    public IctConnectScraper(ScraperType scraperType) {
        super(scraperType);
    }

    @Override
    void runDataExtraction(WebDriver driver, PlanningPortal portal) throws Exception {
        openPortalUrl(portal);
        waitUntilElementPresent("table.ImmTS_Default", 99999);
        WebElement weekStartRow = driver.findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(2)"));
        String receivedUrl = weekStartRow.findElement(By.cssSelector("td:nth-child(2) > a")).getAttribute("href");
        String decidedUrl = weekStartRow.findElement(By.cssSelector("td:nth-child(3) > a")).getAttribute("href");
        processUrl(receivedUrl, null, null, document -> {

        });
    }
}

package com.council.scraper;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
        processApplications(portal, receivedUrl);
        processApplications(portal, decidedUrl);
    }

    private void processApplications(PlanningPortal portal, String url) throws Exception {
        openUrl(url);
        processUrl(url, null, null, document -> {
            List<Element> rows = document.select("table.immTS_Default > tbody > tr");
            rows.remove(0);

            // get all data
            rows.stream().forEach(row -> {
                Application data = new Application();
                data.setAuthority(portal.getAuthority());
                data.setType(portal.getType());

                String referenceNumberUrl = row.select("td:nth-child(1) > a").attr("href");
                if(referenceNumberUrl != null && !referenceNumberUrl.isEmpty()) {
                    referenceNumberUrl = referenceNumberUrl.replace("..","http://planning.allerdale.gov.uk/portal");
                    data.setURL(referenceNumberUrl);
                    try {
                        openUrl(referenceNumberUrl);
                        waitUntilElementPresent("table.ImmTS_Default", 99999);

                        // set reference number
                        String referenceNumber = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(2) > td")).getText();
                        data.setReferenceNumber(referenceNumber);

                        // address
                        String address = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(3) > td")).getText();
                        data.setAddress(address);
                        data.setLocationURL(constructLocationURL(address));

                        // agent data
                        List<WebElement> colsOfAgent = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(7)")).findElements(By.cssSelector("td"));
                        String agentName = colsOfAgent.get(1).getText();
                        data.setAgentName(agentName);

                        String agentPhone = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(8)")).findElements(By.cssSelector("td")).get(1).getText();
                        data.setAgentPhoneNumber(agentPhone);

                        String agentCompany = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(9)")).findElements(By.cssSelector("td")).get(1).getText();
                        data.setAgentCompanyName(agentCompany);

                        String agentAddress = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(10)")).findElements(By.cssSelector("td")).get(1).getText();
                        data.setAgentAddress(agentAddress);

                        // applicant name
                        String applicantName = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(8) > td")).getText();
                        data.setApplicantName(applicantName);

                        // case/handling officer
                        String handlingOfficer = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(7) > td")).getText();
                        data.setCaseOfficer(handlingOfficer);

                        // decision line
                        List<WebElement> decisionDataCols = getWebDriver().findElement(By.cssSelector("table.ImmTS_Default > tbody > tr:nth-child(5)")).findElements(By.cssSelector("td"));
                        String decision = decisionDataCols.get(0).getText();
                        data.setDecision(decision);
                        Date decisionDate = getDecisionDate(decisionDataCols.get(1).getText());
                        data.setDecisionMadeDate(decisionDate);

                        data.setScrapedOn(new Date());

                        updateApplicationData(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });

        });
    }

    private Date getDecisionDate(String dateStr) {
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

}

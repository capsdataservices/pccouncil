package com.council.scraper;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AshfordScraper extends BaseScraper {

    public AshfordScraper(ScraperType scraperType) {
        super(scraperType);
    }

    @Override
    void runDataExtraction(WebDriver driver, PlanningPortal portal) throws Exception {
        driver.manage().deleteAllCookies();
        openPortalUrl(portal);
        acceptTerms(portal);
        submitForm("new");
        processApplications(portal);
    }

    private void acceptTerms(PlanningPortal portal) throws Exception {
        waitUntilElementPresent("#CPH_Details_Agree_CheckBox", 9999);
        clickOnElement("#CPH_Details_Agree_CheckBox");
        clickOnElement("#CPH_Details_Submit_Button");
        waitUntilElementPresent("#CPH_Details_Button7", 60);
        openPortalUrl(portal);
    }

    private void submitForm(String listType) throws Exception {
        waitUntilElementPresent("#__tab_CPH_Details_Details_TabContainer_List_Tab", 9999);
        clickOnElement("#__tab_CPH_Details_Details_TabContainer_List_Tab");
        waitUntilElementPresent("#CPH_Details_Details_TabContainer_List_Tab_searchby_weeklyLists1_WeekCommencing_cal_div", 9999);
        clickOnElement("#CPH_Details_Details_TabContainer_List_Tab_searchby_weeklyLists1_WeekCommencing_LB_today");
        setWeeklyList(listType);
        clickOnElement("#CPH_Details_Details_TabContainer_List_Tab_searchby_weeklyLists1_Search_Button");
    }

    private void processApplications(PlanningPortal portal) throws Exception {
        waitUntilElementPresent("table.results_table", 9999);
        List<WebElement> rows = getWebDriver().findElements(By.cssSelector("table.results_table > tbody > tr"));
        rows.remove(0);
        String mainUrl = getWebDriver().getCurrentUrl();
        List<String> applicationDetailUrls =  rows.stream().map(row -> row.findElement(By.cssSelector("td")).findElement(By.cssSelector("a")).getAttribute("href")).collect(Collectors.toList());
        applicationDetailUrls.forEach(url -> {
            try {
                processApplicationDetails(url, portal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processApplicationDetails(String url, PlanningPortal portal) throws Exception {
        openUrl(url);
        waitUntilElementPresent("#CPH_Details_Details_TabContainer_Summary_Tab_CaseSummary_UpdatePanel1", 9999);

        Application data = new Application();
        data.setAuthority(portal.getAuthority());
        data.setType(portal.getType());
        data.setURL(url);

        String referenceNumber = getWebDriver().findElement(By.cssSelector("#CPH_Details_Details_TabContainer_Summary_Tab_CaseSummary_CaseFullRef")).getText();
        data.setReferenceNumber(referenceNumber);

        String address = getWebDriver().findElement(By.cssSelector("#CPH_Details_Details_TabContainer_Summary_Tab_CaseSummary_LocAddress1")).getText();
        data.setAddress(address);
        data.setLocationURL(constructLocationURL(address));

        String status = getWebDriver().findElement(By.cssSelector("#CPH_Details_Details_TabContainer_Summary_Tab_CaseSummary_Status")).getText();
        data.setStatus(status);

        String decision = getWebDriver().findElement(By.cssSelector("#CPH_Details_Details_TabContainer_Summary_Tab_CaseSummary_CouncilDecision")).getText();
        data.setDecision(decision);

        clickOnElement("#__tab_CPH_Details_Details_TabContainer_Application_Tab");
        waitUntilElementPresent("table.details_table",9999);

        String applicant = getWebDriver().findElement(By.cssSelector("table.details_table > tbody > tr:nth-child(13) > td")).getText();
        data.setApplicantName(applicant);

        String agent = getWebDriver().findElement(By.cssSelector("table.details_table > tbody > tr:nth-child(14) > td")).getText();
        data.setAgentName(agent);

        
    }

    private void setWeeklyList(String type) throws Exception {
        switch (type) {
            case "new":
                clickOnElement("#CPH_Details_Details_TabContainer_List_Tab_searchby_weeklyLists1_WeeklyListType_0");
                break;
        }
    }

}

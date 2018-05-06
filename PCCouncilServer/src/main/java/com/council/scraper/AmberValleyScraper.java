package com.council.scraper;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AmberValleyScraper extends BaseScraper {

    public AmberValleyScraper(ScraperType scraperType) {
        super(scraperType);
    }

    @Override
    void runDataExtraction(WebDriver driver, PlanningPortal portal) throws Exception {
        openPortalUrl(portal);
        sendForm();
        processApplications(portal);
    }

    private void sendForm() throws Exception {
        waitUntilElementPresent("#planAppAccordion", 9999);
        List<WebElement> buttons = getWebDriver().findElements(By.cssSelector("#planAppAccordion > h3"));
        WebElement btn = buttons.get(buttons.size() - 1);
        waitUntil(ExpectedConditions.elementToBeClickable(btn), 9999);
        btn.click();
        waitUntilElementPresent(".ui-accordion-content", 9999);
        WebElement accordionContent = getWebDriver().findElement(By.cssSelector(".ui-accordion-content"));
        List<WebElement> rows = accordionContent.findElements(By.cssSelector("div"));
        waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstDayStartCus")), 9999);
        rows.forEach(row -> {
            try {
                Select fromDaySelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstDayStartCus")));
                Select fromMonthSelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstMonthStartCus")));
                Select fromYearSelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstYearStartCus")));
                Select toDaySelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstDayEndCus")));
                Select toMonthSelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstMonthEndCus")));
                Select toYearSelect = new Select(getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lstYearEndCus")));

                LocalDate now = LocalDate.now();
                LocalDate monday = now.with(DayOfWeek.MONDAY);
                LocalDate sunday = now.with(DayOfWeek.SUNDAY);

                // set from date
                setYear(fromYearSelect, monday);
                setMonth(fromMonthSelect, monday);
                setDay(fromDaySelect, monday);

                // set to date
                setYear(toYearSelect, sunday);
                setMonth(toMonthSelect, sunday);
                setDay(toDaySelect, sunday);

                // submit form
                getWebDriver().findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_btnViewCustom")).click();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void processApplications(PlanningPortal portal) throws Exception {
        waitUntilElementPresent("#planAppList", 9999);
        List<WebElement> applicationsRows = getWebDriver().findElements(By.cssSelector("#planAppList > table > tbody > tr.even"));
        applicationsRows.forEach(row -> {
            try {
                WebElement applicationLink = row.findElement(By.cssSelector("td > b > a"));
                applicationLink.click();
                WebElement dialog = getWebDriver().findElement(By.cssSelector(".ui-dialog[aria-describedby='dialogPlanAppDetails']"));
                waitUntil(ExpectedConditions.visibilityOf(dialog), 9999);
                WebElement closeDialogButton = dialog.findElement(By.cssSelector("button.ui-dialog-titlebar-close"));
                List<WebElement> detailItems = dialog.findElements(By.cssSelector("#tabs-1 > .detailItem"));

                Application data = new Application();
                data.setAuthority(portal.getAuthority());
                data.setType(portal.getType());
                data.setURL(getWebDriver().getCurrentUrl());

                detailItems.forEach(detailItem -> {
                    String detailName = detailItem.findElement(By.cssSelector(".detailName")).getText();
                    String detailContent = detailItem.findElement(By.cssSelector(".detailContent")).getText();

                    if(detailName.contains("Application Reference:")) {
                        data.setReferenceNumber(detailContent);
                    }

                    if(detailName.contains("Address:")) {
                        data.setAddress(detailContent);
                        data.setLocationURL(constructLocationURL(detailContent));
                    }

                    if(detailName.contains("Proposal:")) {
                        data.setProposal(detailContent);
                    }

                    if(detailName.contains("Applicant:")) {
                        String applicantName = detailItem.findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lblApplicantName")).getText();
                        String applicantAddress = detailItem.findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lblApplicantAddress")).getText();
                        data.setApplicantName(applicantName);
                        data.setApplicantAddress(applicantAddress);
                    }

                    if(detailName.contains("Agent:")) {
                        String agentName = detailItem.findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lblAgentName")).getText();
                        String agentAddress = detailItem.findElement(By.cssSelector("#ContentPlaceHolderDefault_MasterTemplateBodyMainPlaceHolder_ctl01_PlanApps_9_lblAgentAddress")).getText();
                        data.setAgentName(agentName);
                        data.setAgentAddress(agentAddress);
                    }

                    if(detailName.contains("Case Officer:")) {
                        data.setCaseOfficer(detailContent);
                    }

                    if(detailName.contains("Status:")) {
                        data.setStatus(detailContent);
                    }

                });

                data.setScrapedOn(new Date());
                updateApplicationData(data);
                closeDialogButton.click();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setDay(Select daySelect, LocalDate date) {
        daySelect.selectByValue(String.valueOf(date.getDayOfMonth()));
    }

    private void setMonth(Select monthSelect, LocalDate date) {
        String monthMediumName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        monthSelect.selectByValue(monthMediumName);
    }

    private void setYear(Select yearSelect, LocalDate date) {
        yearSelect.selectByValue(String.valueOf(date.getYear()));
    }

}

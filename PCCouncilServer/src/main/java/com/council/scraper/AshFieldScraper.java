package com.council.scraper;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class AshFieldScraper extends BaseScraper {

    public AshFieldScraper(ScraperType scraperType) {
        super(scraperType);
    }

    @Override
    void runDataExtraction(WebDriver driver, PlanningPortal portal) throws Exception {
        openPortalUrl(portal);
        formSubmit();
        processApplications(portal);
    }

    private void formSubmit() throws Exception {
        waitUntilElementPresent("form[action='plan_arc_results2.cfm']", 9999);
        WebElement form = getWebDriver().findElement(By.cssSelector("form[action='plan_arc_results2.cfm']"));

        // from
        Select fromDay = new Select(form.findElement(By.cssSelector("select[name='fromday']")));
        Select fromMonth = new Select(form.findElement(By.cssSelector("select[name='frommonth']")));
        Select fromYear = new Select(form.findElement(By.cssSelector("select[name='fromyear']")));

        // to
        Select toDay = new Select(form.findElement(By.cssSelector("select[name='to_day']")));
        Select toMonth = new Select(form.findElement(By.cssSelector("select[name='to_month']")));
        Select toYear = new Select(form.findElement(By.cssSelector("select[name='to_year']")));

        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        LocalDate sunday = now.with(DayOfWeek.SUNDAY);

        // set from date
        setYear(fromYear, monday);
        setMonth(fromMonth, monday);
        setDay(fromDay, monday);

        // set to date
        setYear(toYear, sunday);
        setMonth(toMonth, sunday);
        setDay(toDay, sunday);

        form.findElement(By.cssSelector("input[name='search_date']")).click();
    }

    private void processApplications(PlanningPortal portal) throws Exception {
        waitUntilElementPresent("table.planning_search", 9999);
        List<WebElement> rows = getWebDriver().findElements(By.cssSelector("table.planning_search > tbody > tr > td > p > a"));
        rows.remove(0);
        rows.forEach(row -> {
            try {
                if(!row.getText().equalsIgnoreCase("Click for\nMap")) {
                    String applicationDataUrl = row.getAttribute("href");
                    Application data = new Application();
                    data.setAuthority(portal.getAuthority());
                    data.setType(portal.getType());
                    data.setURL(applicationDataUrl);

                    processUrl(applicationDataUrl, null, null, document -> {
                        List<Element> planningHeadings = document.select(".planning_heading");

                        planningHeadings.forEach(heading -> {

                            if(heading.text().equalsIgnoreCase("Location")) {
                                String nextText = heading.nextElementSibling().text();
                                nextText = nextText.replace(" View Location in Interactive Map via MOLE", "");
                                data.setAddress(nextText);
                                data.setLocationURL(constructLocationURL(nextText));
                            }

                            if(heading.text().equalsIgnoreCase("Proposal")) {
                                String nextText = heading.nextElementSibling().text();
                                data.setProposal(nextText);
                            }

                            if(heading.text().equalsIgnoreCase("Applicant")) {
                                String nextText = heading.nextElementSibling().text();
                                data.setApplicantName(nextText);
                            }

                            if(heading.text().equalsIgnoreCase("Agent")) {
                                String nextText = heading.nextElementSibling().text();
                                data.setAgentName(nextText);
                            }

                            if(heading.text().equalsIgnoreCase("Case Officer")) {
                                String nextText = heading.nextElementSibling().text();
                                data.setCaseOfficer(nextText);
                            }

                            if(heading.text().equalsIgnoreCase("Decision Date")) {
                                String nextText = heading.nextElementSibling().text();
                                data.setDecisionMadeDate(getDate(nextText));
                            }

                        });

                    });

                    data.setScrapedOn(new Date());
                    updateApplicationData(data);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            WebElement nextBtn = getWebDriver().findElement(By.linkText("Next >>"));
            nextBtn.click();
            processApplications(portal);
        } catch (Exception ex) {
        }
    }

    private void setDay(Select daySelect, LocalDate date) {
        daySelect.selectByValue(String.format("%02d", date.getDayOfMonth()));
    }

    private void setMonth(Select monthSelect, LocalDate date) {
        monthSelect.selectByValue(String.format("%02d", date.getMonthValue()));
    }

    private void setYear(Select yearSelect, LocalDate date) {
        yearSelect.selectByValue(String.valueOf(date.getYear()));
    }

    private Date getDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

}

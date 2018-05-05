package com.council.scraper;

import com.council.entity.PlanningPortal;
import org.openqa.selenium.WebDriver;

public interface Scraper {
    void extractData(PlanningPortal portal) throws Exception;
}

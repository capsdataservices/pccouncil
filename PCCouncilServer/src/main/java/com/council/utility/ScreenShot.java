package com.council.utility;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.council.entity.PlanningPortal;
import com.council.scraper.OcellaScraper;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

public class ScreenShot {
	
	private static Logger logger = Logger.getLogger(OcellaScraper.class);
	
	public static void takeScreenShot(WebDriver driver, PlanningPortal portal, String type) {

		logger.warn("Capturing screenshot of cause");
		String name = portal.getAuthority() + "_" + type + "_"
				+ new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date());

		String imageName = "/home/username/application/screenshots/" + name + ".jpg";

		// Screenshot takeScreenshot = new
		// AShot().shootingStrategy(ShootingStrategies.viewportPasting(100))
		// .takeScreenshot(driver);
		Screenshot takeScreenshot = new AShot().shootingStrategy(ShootingStrategies.viewportRetina(100, 0, 0, 2))
				.takeScreenshot(driver);
		BufferedImage image = takeScreenshot.getImage();

		try {
			ImageIO.write(image, "jpg", new File(imageName));
		} catch (IOException e1) {
			System.out.println("Unable to save screenshot");
			e1.printStackTrace();
		}
		File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(src, new File(imageName));
		} catch (IOException e) {
			logger.error("Error while capturing screenshot " + e);
		}
	}
}

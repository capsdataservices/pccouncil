package com.council.scraper.local;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.council.scraper.IctConnectScraper;
import com.council.scraper.ScraperType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.council.entity.PlanningPortal;
import com.council.utility.SessionManager;

public class Scraper {

	private static Logger logger = Logger.getLogger(Scraper.class);

	public static void main(String[] args) {

		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.hibernate").setLevel(Level.WARN);

		try {

			System.setProperty("jsse.enableSNIExtension", "false");

			List<PlanningPortal> portalUrls = getAllPortalURLs();
			for (PlanningPortal portal : portalUrls) {

				portal.setLogFile("E:\\PCCouncilScraper\\logs\\logging.log");

				if (portal.getAttempts() <= 3) {

					com.council.scraper.Scraper foundScraper = getScraper(portal);
					if(foundScraper != null) {
						updatePortalStatus(portal, "IN PROGRESS");
						logger.info("Started scraping data of : " + portal.getAuthority());
						foundScraper.extractData(portal);
						logger.info("Completed scraping data of : " + portal.getAuthority());
					} else {
						logger.warn("There is no scraper available for the portal : " + portal.getURL());
					}

				} else {
					updatePortalStatus(portal, "FAILED");
				}
			}
		} catch (Exception e) {
			logger.error("Error occured in main method : " + e);
		} finally {
			SessionManager.shutdown();
		}
	}

	private static Boolean isPortalType(PlanningPortal portal, String type) {
		if (portal.getType().equals(type) && !(portal.getURL() == null) && !(portal.getURL().isEmpty())) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	private static com.council.scraper.Scraper getScraper(PlanningPortal portal) {

		// idox
		if(isPortalType(portal,"idox")) {
			return new IdoxScraper();
		}

		// ocella
		if(isPortalType(portal,"ocella")) {
			return new OcellaScraper();
		}

		// ocella
		if(isPortalType(portal,"northgate")) {
			return new NorthGateScraper();
		}

		// ict connect
		if(isPortalType(portal,"ict connect")) {
			return new IctConnectScraper(ScraperType.LOCAL);
		}

		return null;
	}

	private static void updatePortalStatus(PlanningPortal portal, String status) {

		Session session = SessionManager.getSessionFactory().openSession();
		session.beginTransaction();

		Query query = session.createQuery("from PlanningPortal where id = :id");
		query.setParameter("id", portal.getId());
		List list = query.list();
		if (list.isEmpty()) {
			logger.info("Unable to get the portal to update the status");
		} else {
			PlanningPortal planningPortal = (PlanningPortal) list.get(0);
			planningPortal.setStatus(status);
			planningPortal.setMessage(status);
			planningPortal.setAttempts(1);
			planningPortal.setLogFile(portal.getLogFile());
			session.save(planningPortal);
		}
		session.getTransaction().commit();
	}

	private static List<PlanningPortal> getAllPortalURLs() {

		Session session = SessionManager.getSessionFactory().openSession();
		session.beginTransaction();

		List<PlanningPortal> portals = new ArrayList<>();

//		Query query = session.createQuery("from PlanningPortal where type = :type and status = :status");
		Query query = session.createQuery("from PlanningPortal where status = :status");
//		query.setParameter("type", "northgate");
		query.setParameter("status", "PENDING");
		// query.setFirstResult(140);
		query.setMaxResults(1);
		List list = query.list();
		if (list.isEmpty()) {
			logger.info("There are no URL's found in the database");
		} else {
			for (Object object : list) {
				PlanningPortal portal = (PlanningPortal) object;
				portals.add(portal);
			}
		}
		session.getTransaction().commit();
		return portals;
	}

}

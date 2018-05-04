package com.council.scraper.local;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.council.entity.PlanningPortal;
import com.council.utility.SessionManager;

public class TerminatedPortalsScraper {

	private static Logger logger = Logger.getLogger(TerminatedPortalsScraper.class);

	public static void main(String[] args) {

		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.hibernate").setLevel(Level.WARN);

		try {

			System.setProperty("jsse.enableSNIExtension", "false");

			List<PlanningPortal> portalUrls = getFailedPortalURLs();
			for (PlanningPortal portal : portalUrls) {

				portal.setLogFile("E:\\PCCouncilScraper\\logs\\logging.log");

				if (portal.getAttempts() <= 3) {

					if (portal.getType().equals("idox") && !(portal.getURL() == null) && !(portal.getURL().isEmpty())) {
						updatePortalStatus(portal, "IN PROGRESS");
						logger.info("Started scraping data of : " + portal.getAuthority());
						IdoxScraper idoxScraper = new IdoxScraper();
						idoxScraper.extractData(portal);
						logger.info("Completed scraping data of : " + portal.getAuthority());

					} else if (portal.getType().equals("ocella") && !(portal.getURL() == null)
							&& !(portal.getURL().isEmpty())) {
						updatePortalStatus(portal, "IN PROGRESS");
						logger.info("Started scraping data of : " + portal.getAuthority());
						OcellaScraper ocellaScraper = new OcellaScraper();
						ocellaScraper.extractData(portal);
						logger.info("Completed scraping data of : " + portal.getAuthority());
					} else if (portal.getType().equals("northgate") && !(portal.getURL() == null)
							&& !(portal.getURL().isEmpty())) {
						updatePortalStatus(portal, "IN PROGRESS");
						logger.info("Started scraping data of : " + portal.getAuthority());
						NorthGateScraper northGateScraper = new NorthGateScraper();
						northGateScraper.extractData(portal);
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
			planningPortal.setAttempts((portal.getAttempts() + 1));
			planningPortal.setMessage(portal.getMessage());
			planningPortal.setLogFile(portal.getLogFile());
			session.save(planningPortal);
		}
		session.getTransaction().commit();
	}

	private static List<PlanningPortal> getFailedPortalURLs() {

		Session session = SessionManager.getSessionFactory().openSession();
		session.beginTransaction();

		List<PlanningPortal> portals = new ArrayList<>();

		Query query = session.createQuery("from PlanningPortal where type = :type and status = :status");
		query.setParameter("type", "ocella");
		query.setParameter("status", "TERMINATED");
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

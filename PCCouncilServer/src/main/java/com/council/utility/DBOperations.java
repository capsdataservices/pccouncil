package com.council.utility;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.council.entity.Application;
import com.council.entity.PlanningPortal;

public class DBOperations {

	private static Logger logger = Logger.getLogger(DBOperations.class);

	public static void updateNPortalStatus(PlanningPortal portal, String status) {

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
			session.save(planningPortal);
		}
		session.getTransaction().commit();

	}

	public static void updateNPortalToError(PlanningPortal portal, String message) {

		Session session = SessionManager.getSessionFactory().openSession();
		session.beginTransaction();

		Query query = session.createQuery("from PlanningPortal where id = :id");
		query.setParameter("id", portal.getId());
		List list = query.list();
		if (list.isEmpty()) {
			logger.info("Unable to get the portal to update the status to -1");
		} else {
			PlanningPortal planningPortal = (PlanningPortal) list.get(0);
			planningPortal.setStatus("TERMINATED");
			planningPortal.setMessage(message);
			session.save(planningPortal);
		}
		session.getTransaction().commit();
	}

	public static void updateApplicationData(Application data) {

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
}

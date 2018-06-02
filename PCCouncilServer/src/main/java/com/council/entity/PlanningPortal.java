package com.council.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "planning_portal")
public class PlanningPortal {

	@Id
	// @GenericGenerator(name = "generator", strategy = "increment")
	// @GeneratedValue(generator = "generator")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "authority_name")
	private String authority;

	@Column(name = "url")
	private String URL;

	@Column(name = "portal_type")
	private String type;

	@Column(name = "status")
	private String status;

	@Column(name = "message")
	private String message;
	
	@Column(name = "attempts")
	private int attempts;
	
	@Column(name = "log_file")
	private String logFile;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "PlanningPortal [Id : " + id + ", Authority : " + authority + ", URL : " + URL + ", type : " + type + ", status : "
				+ status + ", message : " + message + "]";
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

}

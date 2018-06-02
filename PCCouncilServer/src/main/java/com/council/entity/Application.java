package com.council.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "applications")
public class Application {

	@Id
	// @GenericGenerator(name = "generator", strategy = "increment")
	// @GeneratedValue(generator = "generator")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "authority")
	private String authority;

	@Column(name = "type")
	private String type;

	@Column(name = "application_url")
	private String URL;

	@Column(name = "reference")
	private String referenceNumber;

	@Column(name = "alt_reference")
	private String altReferenceNumber;

	@Temporal(TemporalType.DATE)
	@Column(name = "received")
	private Date received;

	@Temporal(TemporalType.DATE)
	@Column(name = "validated")
	private Date validated;

	@Column(name = "address")
	private String address;

	@Column(name = "location_url")
	private String locationURL;

	@Column(name = "proposal")
	private String proposal;

	@Column(name = "status")
	private String status;

	@Column(name = "decision")
	private String decision;

	@Temporal(TemporalType.DATE)
	@Column(name = "decision_issued_on")
	private Date decisionIssuedOn;

	@Column(name = "application_type")
	private String applicationType;

	@Column(name = "case_officer")
	private String caseOfficer;

	@Column(name = "ward")
	private String ward;

	@Column(name = "applicant_name")
	private String applicantName;

	@Column(name = "applicant_address")
	private String applicantAddress;

	@Column(name = "agent_name")
	private String agentName;

	@Column(name = "agent_company_name")
	private String agentCompanyName;

	@Column(name = "agent_email")
	private String agentEmail;

	@Column(name = "agent_phone_number")
	private String agentPhoneNumber;

	@Column(name = "agent_address")
	private String agentAddress;

	@Temporal(TemporalType.DATE)
	@Column(name = "decision_made_date")
	private Date decisionMadeDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "permission_expiry_date")
	private Date permissionExpiryDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "scraped_on")
	private Date scrapedOn;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public String getAltReferenceNumber() {
		return altReferenceNumber;
	}

	public void setAltReferenceNumber(String altReferenceNumber) {
		this.altReferenceNumber = altReferenceNumber;
	}

	public Date getReceived() {
		return received;
	}

	public void setRecieved(Date received) {
		this.received = received;
	}

	public Date getValidated() {
		return validated;
	}

	public void setValidated(Date validated) {
		this.validated = validated;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getProposal() {
		return proposal;
	}

	public void setProposal(String proposal) {
		this.proposal = proposal;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public Date getDecisionIssuedOn() {
		return decisionIssuedOn;
	}

	public void setDecisionIssuedOn(Date decisionIssuedOn) {
		this.decisionIssuedOn = decisionIssuedOn;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(String applicationType) {
		this.applicationType = applicationType;
	}

	public String getCaseOfficer() {
		return caseOfficer;
	}

	public void setCaseOfficer(String caseOfficer) {
		this.caseOfficer = caseOfficer;
	}

	public String getWard() {
		return ward;
	}

	public void setWard(String ward) {
		this.ward = ward;
	}

	public String getApplicantName() {
		return applicantName;
	}

	public void setApplicantName(String applicantName) {
		this.applicantName = applicantName;
	}

	public String getApplicantAddress() {
		return applicantAddress;
	}

	public void setApplicantAddress(String applicantAddress) {
		this.applicantAddress = applicantAddress;
	}

	public Date getDecisionMadeDate() {
		return decisionMadeDate;
	}

	public void setDecisionMadeDate(Date decisionMadeDate) {
		this.decisionMadeDate = decisionMadeDate;
	}

	public Date getPermissionExpiryDate() {
		return permissionExpiryDate;
	}

	public void setPermissionExpiryDate(Date permissionExpiryDate) {
		this.permissionExpiryDate = permissionExpiryDate;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentCompanyName() {
		return agentCompanyName;
	}

	public void setAgentCompanyName(String agentCompanyName) {
		this.agentCompanyName = agentCompanyName;
	}

	public String getAgentEmail() {
		return agentEmail;
	}

	public void setAgentEmail(String agentEmail) {
		this.agentEmail = agentEmail;
	}

	public String getAgentPhoneNumber() {
		return agentPhoneNumber;
	}

	public void setAgentPhoneNumber(String agentPhoneNumber) {
		this.agentPhoneNumber = agentPhoneNumber;
	}

	public String getAgentAddress() {
		return agentAddress;
	}

	public void setAgentAddress(String agentAddress) {
		this.agentAddress = agentAddress;
	}

	public Date getScrapedOn() {
		return scrapedOn;
	}

	public void setScrapedOn(Date scrapedOn) {
		this.scrapedOn = scrapedOn;
	}

	public void setReceived(Date received) {
		this.received = received;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public String getLocationURL() {
		return locationURL;
	}

	public void setLocationURL(String locationURL) {
		this.locationURL = locationURL;
	}

}

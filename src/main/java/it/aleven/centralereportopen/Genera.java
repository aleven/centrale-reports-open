/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.aleven.centralereportopen;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import it.aleven.centralereportopen.entities.Profilo;

/**
 * 
 * @author Mirco
 */
public class Genera extends HttpServlet {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {

			ServletContext context = getServletContext();

			String path = getServletContext().getRealPath(context.getInitParameter("path_reports"));

			String connNum = StringUtils.defaultIfBlank(request.getParameter("conn"), "");

			String connectionStringDriver = context.getInitParameter("connectionStringDriver" + connNum);
			String connectionStringURL = context.getInitParameter("connectionStringURL" + connNum);
			String connectionStringUserName = context.getInitParameter("connectionStringUserName" + connNum);
			String connectionStringPassword = context.getInitParameter("connectionStringPassword" + connNum);

			String email_server = context.getInitParameter("email_server");
			String email_username = context.getInitParameter("email_username");
			String email_password = context.getInitParameter("email_password");
			String email_from = context.getInitParameter("email_from");
			String email_subject = context.getInitParameter("email_subject");
			String email_sender = context.getInitParameter("email_sender");

			String to = request.getParameter("to");
			String ccn = request.getParameter("ccn");

			String report = request.getParameter("report");
			String paramTipoReport = StringUtils.defaultString(request.getParameter("tipo"), "");
			String[] tipiReport = paramTipoReport.split(",");
			
			String paramAnno = request.getParameter("anno");
			String paramMese = request.getParameter("mese");

			String profilo = request.getParameter("profilo");
			logger.info("configurazione predefinita da profilo: " + profilo);
			/* configurazione dati da configurazione profilo */
			if (StringUtils.isNotBlank(profilo)) {
				/* caricamento dati PROFILO da configurazione JPA, configurazione "base" semplice */
				EntityManagerFactory emf = null;
				EntityManager em = null;
				try {
					emf = Persistence.createEntityManagerFactory("DEFAULT_PU");
					em = emf.createEntityManager();
					Profilo profiloData = em.find(Profilo.class, profilo);
					if (profiloData != null) {
						to = profiloData.getTo();
						ccn = profiloData.getCcn();
						report = profiloData.getReport();
					}
				} catch (Exception ex) {
					logger.error("errore JPA", ex);
					if (em != null)
						em.close();
					if (emf != null)
						emf.close();
				}
			}

			int anno = 0;
			if (paramAnno == null || paramAnno.equalsIgnoreCase("")) {
				anno = (Calendar.getInstance().get(Calendar.YEAR));
			} else {
				anno = Integer.parseInt(paramAnno);
			}
			int mese = 0;
			if (paramMese == null || paramMese.equalsIgnoreCase("")) {
				// zero based
				mese = (Calendar.getInstance().get(Calendar.MONTH) + 1);
			} else {
				mese = Integer.parseInt(paramMese);
			}

			String fileJasper = path + File.separator + report + ".jasper";

			Map<String, Object> parametri = new HashMap<String, Object>();
			parametri.put("PATH_REPORT", path + File.separator);
			parametri.put("REPORT_LOCALE", Locale.ITALY);
			// passaggio parametri "personalizzati" al report
			parametri.put("ANNO", anno);
			parametri.put("MESE", mese);

			logger.info(String.format("connectionStringDriver=%s, connectionStringURL=%s, connectionStringUserName=%s, connectionStringPassword=%S", connectionStringDriver, connectionStringURL, connectionStringUserName, connectionStringPassword));

			String queryTest = "";
			if (StringUtils.isBlank(connNum)) {
				queryTest = "SELECT 1;";
			}
			String queryUpdate = "";

			/* generazione dei PDF per famiglia */
			Map<String, File> filesGenerati = new HashMap<String, File>();
			if (tipiReport.length > 1) {
				for (String tipoReport : tipiReport) {
					logger.info("inizio generazione pdf " + report + " " + tipoReport + " " + anno + "" + mese);
					/* Parametro SPECIFICO per TIPO */
					parametri.put("PARAM_TIPO", tipoReport);
	
					File filePdf = new GestoreReport().generaPdf2(fileJasper, parametri, connectionStringDriver, connectionStringURL, connectionStringUserName, connectionStringPassword, queryTest, queryUpdate);
					logger.info(String.format("pdf generato correttamente %s", filePdf));
					filesGenerati.put(tipoReport, filePdf);
				}
			} else {
				File filePdf = new GestoreReport().generaPdf2(fileJasper, parametri, connectionStringDriver, connectionStringURL, connectionStringUserName, connectionStringPassword, queryTest, queryUpdate);
				logger.info(String.format("pdf generato correttamente %s", filePdf));
				filesGenerati.put(paramTipoReport, filePdf);				
			}

			// verifico se devo inviare il report via email o generarlo a
			// video
			if (to == null || to.equalsIgnoreCase("")) {
				logger.info("online preview ...");

				File filePdf = filesGenerati.get(paramTipoReport);
				ServletOutputStream out = null;
				byte[] fileStream = FileUtils.readFileToByteArray(filePdf);
				response.reset();
				addNoCacheHeaders(response);
				response.setContentType("application/pdf");
				response.setContentLength(fileStream.length);
				out = response.getOutputStream();
				out.write(fileStream);
				out.close();
			} else {
				logger.info("invio email ...");

				String attachName = "";
				String attachDesc = "";
				// String senderName = email_sender;

				// Create the email message
				// MultiPartEmail email = new MultiPartEmail();
				HtmlEmail email = new HtmlEmail();
				email.setHostName(email_server);

				if (email_username != null && !email_username.equals("") && email_password != null && !email_password.equals("")) {
					email.setAuthentication(email_username, email_password);
				}

				/* TO */
				if (to.indexOf(",") > 0) {
					String[] res = to.split(",");
					for (int i = 0; i < res.length; i++) {
						email.addTo(res[i], "");
					}
				} else {
					email.addTo(to, "");
				}

				/* CCN */
				if (StringUtils.isNotBlank(ccn)) {
					if (ccn.indexOf(",") > 0) {
						String[] res = ccn.split(",");
						for (int i = 0; i < res.length; i++) {
							email.addBcc(res[i], "");
						}
					} else {
						email.addBcc(ccn, "");
					}
				}

				for (String tipoReport : filesGenerati.keySet()) {

					File filePdf = filesGenerati.get(tipoReport);

					if ((anno == Calendar.getInstance().get(Calendar.YEAR)) && (mese == Calendar.getInstance().get(Calendar.MONTH) + 1)) {
						attachName = report + "-" + tipoReport + "-" + anno + "-" + mese + "-" + String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
						email_subject = "" + getReportDecrizione(report) + " " + paramTipoReport + " " + String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) + "-" + mese + "-" + anno;
					} else {
						attachName = report + "-" + tipoReport + "-" + anno + "-" + mese;
						email_subject = "" + getReportDecrizione(report) + " " + paramTipoReport + " " + mese + "-" + anno;
					}

					// Create the attachment
					EmailAttachment attachment = new EmailAttachment();

					/* File da Temporaneo */
					attachment.setPath(filePdf.getPath());
					attachment.setDisposition(EmailAttachment.ATTACHMENT);
					attachment.setDescription(attachDesc);
					attachment.setName(attachName + ".pdf");

					logger.info("add the attachment ...");
					// add the attachment
					email.attach(attachment);
				}

				email.setFrom(email_from, email_sender);
				email.setSubject(email_subject);

				String body = "Vedi il documento PDF allegato";
				if (tipiReport.length > 1) {
					body = "Vedi i documenti PDF allegati";
				}
				email.setMsg(body);

				File bodyFile = new File(path, report + ".html");
				if (bodyFile.exists()) {
					logger.info("loading html from " + bodyFile.getPath());
					body = FileUtils.readFileToString(bodyFile);

					int i = 1;
					for (String tipoReport : filesGenerati.keySet()) {

						/* prendo solo i parametri che mi servono + data */
						String liveUrlParams = request.getQueryString();
						// corretto il fatto che se genero mail con link da MAILING devo usare il tipo specificato nel record (e potrei non avere un parametro via url)
						String liveReport = report; // request.getParameter("tipo");
						// String liveFamiglia =
						// request.getParameter("famiglia");
						String liveFamiglia = tipoReport;
						long liveTimestamp = new Date().getTime();

						List<String> usedParams = new ArrayList<String>();
						if (StringUtils.isNotBlank(liveReport))
							usedParams.add(String.format("report=%s", liveReport));
						if (StringUtils.isNotBlank(liveFamiglia))
							usedParams.add(String.format("tipo=%s", liveFamiglia));

						usedParams.add(String.format("timestamp=%s", liveTimestamp));
						liveUrlParams = StringUtils.join(usedParams, "&");

						String encodedData = Base64.encodeBase64URLSafeString(liveUrlParams.getBytes());
						String authParam = new StringBuffer().append("").append(encodedData).toString();
						
						// Email body parser
						body = StringUtils.replace(body, "#{auth" + i + "}", authParam);
						body = StringUtils.replace(body, "#{desc" + i + "}", getFamigliaDecrizione(tipoReport));
						i++;
					}

					email.setHtmlMsg(body);
				}

				logger.info("sending email ...");

				// send the email
				email.send();

				String msg = String.format("email sent to:%s ccn:%s", to, ccn);
				logger.info(msg);

				printOut(response, msg);
			}

		} catch (Exception ex) {
			logger.error("processRequest", ex);
			printOut(response, "ERROR: " + ex.getMessage());

		} finally {
		}
	}

	// <editor-fold defaultstate="collapsed"
	// desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 * 
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Hydromec Reports";
	}// </editor-fold>

	private void printOut(HttpServletResponse response, String message) throws IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		out.print(message);
		out.close();
	}

//	private static void testConnessione(String connectionStringDriver, String connectionStringURL, String connectionStringUserName, String connectionStringPassword) throws Exception {
//
//		JdbcConnector as400 = new JdbcConnector(connectionStringURL, connectionStringDriver, connectionStringUserName, connectionStringPassword);
//
//		ResultSet rs = as400.executeSelect(true, "SELECT * FROM HYDR_STAT.STCIE01L");
//		ResultSet rs2 = as400.executeSelect(true, "SELECT * FROM HYDR_STAT.STIIE01L");
//
//		as400.close();
//	}

	private String getFamigliaDecrizione(String famiglia) {
		String famigliaDescrizione = famiglia;
		if (StringUtils.isNotBlank(famigliaDescrizione))
			if (famiglia.equalsIgnoreCase("VSF")) {
				famigliaDescrizione = "Vite Senza Fine";
			} else if (famiglia.equalsIgnoreCase("VFQ")) {
				famigliaDescrizione = "Vite a Vite Senza Fine Quadrati";
			} else if (famiglia.equalsIgnoreCase("BVX")) {
				famigliaDescrizione = "a coppia conia (beveltriebe)";
			} else if (famiglia.equalsIgnoreCase("RCX")) {
				famigliaDescrizione = "coassiali";
			}

		return famigliaDescrizione;
	}

	private String getReportDecrizione(String report) {
		String reportDescrizione = report;
		if (StringUtils.isNotBlank(reportDescrizione))
			if (report.equalsIgnoreCase("test")) {
				reportDescrizione = "TESTREPORT";
			} else {
				reportDescrizione = "DESCRIZIONE NON DISPONIBILE";
			}
		return reportDescrizione;
	}
	
	public static void addNoCacheHeaders(HttpServletResponse response) {
		/* HTTP 1.1. */
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		/* HTTP 1.0. */
		response.setHeader("Pragma", "no-cache");
		/* Proxies. */
		response.setDateHeader("Expires", 0);
		
		response.setHeader("content-disposition", "inline; filename=" + java.util.UUID.randomUUID());
	}
}

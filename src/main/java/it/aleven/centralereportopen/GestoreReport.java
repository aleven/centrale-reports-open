/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.aleven.centralereportopen;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * 
 * @author Mirco
 */
public class GestoreReport {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Metodo che dato il nome del file jasper ed i suoi paramteri, restituisce
	 * l'array di bit del pdf generato
	 * 
	 * @param nomeFileJasper
	 * @param parametri
	 * @return
	 * @throws java.lang.Exception
	 */
	private byte[] exportReportToPdf(String nomeFileJasper, Map<String, Object> parametri, String driver, String url, String username, String password, String queryTest, String queryUpdate) throws Exception {
		byte[] returnValue = null;
		JasperPrint reportGenerato = null;
		try {
			/* Genero il report dal jasper */
			reportGenerato = fillReport(nomeFileJasper, parametri, driver, url, username, password, queryTest, queryUpdate);
			/* Lo esporto in PDF */
			returnValue = JasperExportManager.exportReportToPdf(reportGenerato);
		} catch (Exception e) {
			logger.error("exportReportToPdf", e);
			throw e;
		} finally {
			reportGenerato = null;
		}
		return returnValue;
	}

	/**
	 * Metodo che genera un report Jasper dato il nome del file ed i parametri
	 * 
	 * @param nomeFileJasper
	 * @param parametri
	 * @return
	 * @throws java.lang.Exception
	 */
	private JasperPrint fillReport(String nomeFileJasper, Map<String, Object> parametri, String driver, String url, String username, String password, String queryTest, String queryUpdate) throws Exception {
		JasperPrint returnValue = null;
		String nomeFileJasperTemp = null;
		JasperReport jasperReport = null;
		String pathReports = null;
		Connection conn = null;
		try {
			if (StringUtils.isNoneBlank(driver)) {
				/* Apro una connessione al dataBase */
				conn = getConnection(driver, url, username, password);
	
				try {
					/* ESEGUO QUERY di TEST SE SPECIFICAT */
					if (StringUtils.isNotBlank(queryTest)) {
						logger.info(String.format("executing query test %s", queryTest));
						boolean res = conn.prepareStatement(queryTest).execute();
					}
				} catch (Exception ex) {
					logger.error("fillReport " + queryTest, ex);
				}
	
				try {
					/* QUERY di AGGIORNAMENTO DATI */
					if (StringUtils.isNotBlank(queryUpdate)) {
						logger.info(String.format("executing query update %s", queryUpdate));
						boolean res = conn.prepareStatement(queryUpdate).execute();
					}
				} catch (Exception ex) {
					logger.error("fillReport " + queryTest, ex);
			}
			}
			/* Genero il report */
			jasperReport = (JasperReport) JRLoader.loadObjectFromFile(nomeFileJasper);

			/* Effettuo il fill del report */
			returnValue = JasperFillManager.fillReport(jasperReport, parametri, conn);
		} catch (Exception e) {
			logger.error("fillReport", e);
			throw e;
		} finally {
			// Chiusura connection e Clean-up
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
			nomeFileJasperTemp = null;
			jasperReport = null;
			pathReports = null;
			conn = null;
		}
		return returnValue;

	}

	public File generaPdf2(String nomeFileJasper, Map<String, Object> parametri, String driver, String url, String username, String password, String queryTest, String queryUpdate) throws Exception {
		byte[] fileStream = null;
		File fileTmp = null;
		try {
			fileTmp = File.createTempFile(FilenameUtils.getBaseName(nomeFileJasper), ".pdf");
			/* Genero il report in pdf */
			fileStream = exportReportToPdf(nomeFileJasper, parametri, driver, url, username, password, queryTest, queryUpdate);
			FileUtils.writeByteArrayToFile(fileTmp, fileStream);
		} catch (Exception e) {
			logger.error("generaPdf", e);
			throw e;
		} finally {
			fileStream = null;
		}
		return fileTmp;
	}
	
	/**
	 * Takes 3 parameters: databaseName, userName, password and connects to the
	 * database.
	 * 
	 * @param databaseName
	 *            holds database name,
	 * @param userName
	 *            holds user name
	 * @param password
	 *            holds password to connect the database,
	 * @return Returns the JDBC connection to the database
	 */
	public Connection getConnection(String driverClass, String connString, String userName, String password) throws Exception {
		Connection conn = null;
		try {
			Class.forName(driverClass).newInstance();
		} catch (Exception ex) {
			logger.error("Check classpath. Cannot load db driver: " + driverClass, ex);
			throw ex;
		}
		try {
			conn = DriverManager.getConnection(connString, userName, password);
		} catch (SQLException ex) {
			logger.error("Driver loaded, but cannot connect to db: " + connString, ex);
			throw ex;
		}
		return conn;
	}	
}

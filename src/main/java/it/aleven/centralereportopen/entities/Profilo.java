package it.aleven.centralereportopen.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "rpt01_profili")
public class Profilo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "rpt01_nome", unique = true, nullable = false)
	private String nome;

	@Column(name = "rpt01_report")
	private String report;

	@Column(name = "rpt01_to")
	@Lob
	private String to;

	@Column(name = "rpt01_ccn")
	@Lob
	private String ccn;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getReport() {
		return report;
	}

	public void setReport(String report) {
		this.report = report;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getCcn() {
		return ccn;
	}

	public void setCcn(String ccn) {
		this.ccn = ccn;
	}
}

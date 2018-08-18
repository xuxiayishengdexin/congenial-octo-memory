package A1;


public class rd {
	private String datatype;
	private String dip;
	private String dport;
	private String[] proto;
	private String sip;
	private String sport;
	private String other;
	 // 构造函数
	public rd() {
		datatype = "log";
		dip = "";
		dport = "";
		proto = new String[] {"", ""};
		sip = "";
		sport = "";
		other = "";
	}
	
	public void setdatatype(String datatype) {// datatype
        this.datatype = datatype;
    }
	public String getdatatype() {
        return this.datatype;
    }
	public String getDip() {
		return dip;
	}
	public void setDip(String dip) {
		this.dip = dip;
	}
	public void setdport(String dport) {//dport
        this.dport = dport;
    }
	public String getdport() {
        return this.dport;
    }	
	public void setprotocol(String[] proto) {//protocol
        this.proto = proto;
    }
	public String[] getprotocol() {
        return this.proto;
    }
	public void setSip(String name) {//sip
        this.sip = name;
    }
	public String getSip() {
        return this.sip;
    }
	public void setsport(String sport) {//sport
        this.sport = sport;
    }
	public String getsport() {
        return this.sport;
    }
	
	public void setother(String sport) {//sport
        this.other = other;
    }
	public String getother() {
        return this.other;
    }
}
package A1;
import A1.rd;

public class ALERTcom {	
	private String[] asset;
	private String behaviortype;  //违规
	private String datatype;  //alert
	private String description;
	private String extractor;
	private long logtime;
	private String object;
	private rd rd_entity;
	private String risklevel;
	private String ver;
	private String author;
	
	public ALERTcom() {
		asset = new String[] {"", ""};
		behaviortype = "违规";
		datatype = "alert";
		description = "";
		extractor = "";
		logtime = 0;
		object = "";
		rd_entity = new rd();
		risklevel = "";
		ver = "";
		author = "";
	}
	public void setasset(String[] asset) {// asset
        this.asset = asset;
    }
	public String[] getasset() {
        return this.asset;
    }
	public void setbehaviortype(String behavior){// behaviortype
		this.behaviortype = behavior;
	}
	public String getbehaviortype(){
		return this.behaviortype;
	}
	public void setdatatype(String data) {//datatype
        this.datatype = data;
    }
	public String getdatatype() {
        return this.datatype;
    }
	public void setdescription(String des) {//description
        this.description = des;
    }
	public String getdescription() {
        return this.description;
    }	
	public void setextractor(String extractor) {//extractor
        this.extractor = extractor;
    }
	public String getextractor() {
        return this.extractor;
    }	
	public void setlogtime(long logtime) {// logtime
        this.logtime = logtime;
    }
	public long getlogtime() {
        return this.logtime;
    }
	public void setobject(String object) {// object
        this.object= object;
    }
	public String getobject() {
        return this.object;
    }
	public void setrd(rd rd_entity) {// rd
        this.rd_entity = rd_entity;
    }
	public rd getrd() {
        return this.rd_entity;
    }
	public void setrisklevel(String level) {//risklevel
        this.risklevel = level;
    }
	public String getrisklevel() {
        return this.risklevel;
    }
	public void setver(String ver) {//ver
        this.ver = ver;
    }
	public String getver() {
        return this.ver;
    }
	public void setauthor(String author) {//ver
        this.author = author;
    }
	public String getauthor() {
        return this.author;
    }
}

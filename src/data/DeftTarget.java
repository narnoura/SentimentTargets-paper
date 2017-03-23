/**
 * 
 */
package data;

/**
 * @author noura
 *
 */
public class DeftTarget extends Target {

	/**
	 * inherits Target. Allows for regeneration of best file
	 */
	public String ere_id;
	public String offset;
	public String length;
	public String sarcasm;
	public String source_ere_id;
	public String type;
	public String source;
	public String source_length;
	public String source_offset;

	public DeftTarget() {
		source = "";
		source_ere_id="";
		source_length="";
		source_offset="";
	}
	
	public DeftTarget(String sentiment, String text) {
		super(sentiment,text);
		source = "";
		source_ere_id="";
		source_length="";
		source_offset="";
	}
	public void SetID(String ere_id) {
		this.ere_id = ere_id;
	}
	public void SetDeftOffset(String offset) {
		this.offset = offset;
	}
	public void SetLength(String length) {
		this.length = length;
	}
	public void SetSarcasm(String sarcasm) {
		this.sarcasm = sarcasm;
	}
	public void SetSourceID(String source_id) {
		source_ere_id = source_id;
	}
	// can be "entity", "event", or "relation"
	public void SetType(String type) {
		this.type = type;
	}
	public void SetSource(String source) {
		this.source = source;
	}
	public void SetSourceOffset(String offset) {
		this.source_offset = offset;
	}
	public void SetSourceLength(String length) {
		this.source_length = length;
	}
	
	
	

}

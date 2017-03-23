package data;

public class Entity {
	
	public String ere_id;
	public String offset;
	public String length;
	public String text;
	public String sentiment;
	public String sarcasm;
	public String source;
	public String source_id;
	public String source_length;
	public String source_offset;
	public String type; // relation, entity, event
	
	public Entity() {
		ere_id = "";
		offset = "";
		length = "";
		text = "";
		sentiment = "";
		sarcasm = "";
		source = "";
		source_id = "";
		source_length= "";
		source_offset= "";
		type = "";
	}
	
	public void SetEntity(String ere_id,
			String offset,
			String length,
			String text,
			String sentiment,
			String sarcasm,
			String source,
			String source_id,
			String source_length,
			String source_offset,
			String type) {
		
		this.ere_id = ere_id;
		this.offset = offset;
		this.length = length;
		this.text = text ;
		this.sentiment = sentiment;
		this.sarcasm = sarcasm;
		this.source = source;
		this.source_id = source_id;
		this.source_length= source_length;
		this.source_offset= source_offset;		
		this.type = type;
	}
		
	
	
}

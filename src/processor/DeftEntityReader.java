package processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.FileReader;

/*
 * Reads a DEFT rich ere file and stores all entities, relations and events
 * 
 */
public class DeftEntityReader {

	// should we also store the doc id? or have an object for each id? e.g DEFTInputReader has an entityreader for each post?
	
	/*List<HashMap<String,HashMap<String,String>>> entities;
	//HashMap<String,HashMap<String,String>> entities;
	// for each mention, it will have among other fields, a field referring back to the 
	// original entity id. It's the mention that will basically be output in the .best file
	// relations and events can also refer back to the entities map
	HashMap<String,HashMap<String,String>> entity_mentions; // offset - HashMap offset - Hashmap
	HashMap<String,HashMap<String,String>> relations;
	HashMap<String,HashMap<String,String>> events;*/
	// For entities: hash by entity id
	// For entity mentions: hash by char offst
	public HashMap<String,List<String>> entities; // list of entities. for each entity, a list of mention offsets (or ids?)
	// TODO: Didn't do the same for the relation and event mentions
	public HashMap<String,String[]> entity_mentions; // for each char offset, return the ere id and source id
	public HashMap<String,String[]> relation_mentions; 
	public HashMap<String,String[]> event_mentions; 
	String doc_id;
	String ere_file;
	
	public DeftEntityReader() {
		entities = new HashMap<String,List<String>>();
		entity_mentions = new HashMap<String,String[]>();
		relation_mentions = new HashMap<String,String[]>();
		event_mentions = new HashMap<String,String[]>();
	}
	
	public DeftEntityReader(String doc_id) {
		this.doc_id = doc_id;
		entities =  new HashMap<String,List<String>>();
		entity_mentions = new HashMap<String,String[]>();
		relation_mentions = new HashMap<String,String[]>();
		event_mentions = new HashMap<String,String[]>();
	}
	
	public void SetEreFile(String ere_file){
		this.ere_file = ere_file;
	}
	
	// reads entities, mentions, relations and events into maps 
	public void ReadEREFile(String ere_file) {
		if (doc_id.isEmpty() || doc_id == null) {
			System.out.println("Entity reader: empty doc id. Exiting \n");
		}
		if (ere_file.isEmpty()) {
			return;
		}
		Document ere_doc = FileReader.ReadXMLFile(ere_file, "english");
		ere_doc.getDocumentElement().normalize();
		NodeList ere_entities = ere_doc.getElementsByTagName("entity");
		for (int e=0; e<ere_entities.getLength(); e++) {
			//String[] mention_attributes = new String[7];
			List<String> entity_mention_offsets = new ArrayList<String>();
			Node entity_node = ere_entities.item(e);
			Element entity = (Element) entity_node;
			String id = entity.getAttribute("id");
			String type = entity.getAttribute("type");
			String specificity = entity.getAttribute("specific");
			
			NodeList mentions = entity.getElementsByTagName("entity_mention");
			for (int m=0;m<mentions.getLength();m++) {
				String[] mention_attributes = new String[8]; // put it here
				Node mention_node = mentions.item(m);
				Element mention = (Element) mention_node;
				String mention_id = mention.getAttribute("id");
				String noun_type = mention.getAttribute("noun_type");
				String doc = mention.getAttribute("source");
				String offset = mention.getAttribute("offset");
				String length = mention.getAttribute("length");
				String mention_text = 
						mention.getElementsByTagName("mention_text").item(0).getTextContent();
				String full_text = mention_text;
			
				if (mention.getElementsByTagName("nom_head").item(0) != null) {
					Node head_node = mention.getElementsByTagName("nom_head").item(0);
					Element head_element = (Element) head_node;
					//offset = head_element.getAttribute("offset");
					//length = head_element.getAttribute("length");
					mention_text = head_element.getTextContent();
				}
				entity_mention_offsets.add(offset);
				//entity_mention_offsets.add(mention_id);
				//mention_attributes[0] = offset;
				mention_attributes[0] = mention_id;
				mention_attributes[1] = length;
				mention_attributes[2] = mention_text;
				mention_attributes[3] = doc;
				mention_attributes[4] = noun_type;
				mention_attributes[5] = type;
				mention_attributes[6] = specificity;
				mention_attributes[7] = full_text;
				entity_mentions.put(offset, mention_attributes);
			}
			entities.put(id, entity_mention_offsets);
		}
		NodeList ere_relations = ere_doc.getElementsByTagName("relation");
		for (int e=0; e<ere_relations.getLength(); e++) {
			String[] relation_attributes = new String[8];
			Node relation_node = ere_relations.item(e);
			Element relation = (Element) relation_node;
			String rid = relation.getAttribute("id");
			String rtype = relation.getAttribute("type");
			String subtype = relation.getAttribute("subtype");
			NodeList relation_mentions = relation.getElementsByTagName("relation_mention");
			for (int r=0;r<relation_mentions.getLength();r++) {
				Node rmention_node = relation_mentions.item(r);
				Element rmention = (Element) rmention_node;
				String rmention_id = rmention.getAttribute("id");
				String realis = rmention.getAttribute("realis");
				Node trigger_node = rmention.getElementsByTagName("trigger").item(0);
				Element trigger_element = (Element) trigger_node;
				String tdoc= "";
				String toffset = "";
				String tlength = "";
				String relation_text = "";
		
				if (trigger_element != null) {
					 tdoc = trigger_element.getAttribute("source");
					 toffset = trigger_element.getAttribute("offset");
					 tlength = trigger_element.getAttribute("length");
					 relation_text = trigger_element.getTextContent();
			}
				//relation_attributes[0] = toffset;
				relation_attributes[0] = rmention_id;
				relation_attributes[1] = tlength;
				relation_attributes[2] = relation_text;
				relation_attributes[3] = tdoc;
				relation_attributes[4] = realis;
				relation_attributes[5] = rtype;
				relation_attributes[6] = subtype;
				relation_attributes[7] = rid;
				this.relation_mentions.put(toffset, relation_attributes);
			}
			NodeList events = ere_doc.getElementsByTagName("hopper");
			for (int h=0; h<events.getLength(); h++) {
				Node hopper_node = events.item(h);
				Element hopper = (Element) hopper_node;
				String hid = hopper.getAttribute("id");
				NodeList hopper_mentions = hopper.getElementsByTagName("event_mention");
				for (int v=0;v<hopper_mentions.getLength();v++) {
					String[] event_attributes = new String[8];
					Node hmention_node = hopper_mentions.item(v);
					Element hmention = (Element) hmention_node;
					String event_mention_id = hmention.getAttribute("id");
					String event_mention_type = hmention.getAttribute("type");
					String event_mention_subtype = hmention.getAttribute("subtype");
					String event_mention_realis = hmention.getAttribute("realis");
					Node trigger_node = hmention.getElementsByTagName("trigger").item(0);
					Element trigger_element = (Element) trigger_node;
					String hdoc = trigger_element.getAttribute("source");
					String hoffset = trigger_element.getAttribute("offset");
					String hlength = trigger_element.getAttribute("length");
					String event_text = trigger_element.getTextContent();
					
					//event_attributes[0] = hoffset;
					event_attributes[0] = event_mention_id;
					event_attributes[1] = hlength;
					event_attributes[2] = event_text;
					event_attributes[3] = hdoc;
					event_attributes[4] = event_mention_realis;
					event_attributes[5] = event_mention_type;
					event_attributes[6] = event_mention_subtype;
					event_attributes[7] = hid;
					this.event_mentions.put(hoffset, event_attributes);
				}
				
			}
		}
		
		
		
		
	}

}

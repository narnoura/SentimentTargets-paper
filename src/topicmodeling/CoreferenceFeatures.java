package topicmodeling;

import java.util.HashMap;
import java.util.List;

import data.*;
import processor.DeftEntityReader;

public class CoreferenceFeatures {
	/*
	 * Features for determining whether two tokens or two targets are co-referrent.
	 */
	public CoreferenceFeatures() {
		// TODO Auto-generated constructor stub
	}
	
	// Use gold entity mentions from DEFT ere to annotate coreference
	public static List<Comment> AnnotateDeftCoreference(List<Comment> comments) {
		for (Comment c: comments) {
			AnnotateCommentDeftCoreference(c);
		}
		return comments;
	}
	
	// can do for all not just for targets (separate function, and then
	// another for targets)
	// TODO: add also for relations and events
	public static void AnnotateCommentDeftCoreference(Comment c) {
		DeftEntityReader er = c.entity_reader;
		HashMap<String,List<String>> entities = er.entities;
		HashMap<String,String[]> entity_mentions = er.entity_mentions;
		for (Target t: c.targets_) {
			DeftTarget dt = (DeftTarget) t;
			//System.out.println("Target:"+dt.text_);
			String offset = dt.offset;
			for (String entity_id: entities.keySet()) {
				List<String> mention_offsets = entities.get(entity_id);
				if (mention_offsets.contains(offset)) {
				// found the entity
				for (String mention: mention_offsets) {
					if (entity_mentions.containsKey(mention)) {
						// Update all tokens which have this offset
						for (Token tok: c.tokens_) {
							int i = c.tokens_.indexOf(tok);
							DeftToken dtok = (DeftToken) tok;
							Integer char_offset = dtok.char_offset;
							if (char_offset.toString().equals(mention)) {
								//System.out.println("Found mention! " + dtok.text_);
								dtok.SetCoreferrentWithTarget(true);
								dtok.SetCoreferringToken(dt.tokens_.get(0));
								Integer temp_offset = char_offset;
								Integer temp_length = dtok.text_.length();
								String length = entity_mentions.get(mention)[1];
								Integer lengthint = Integer.parseInt(length);
								//System.out.println("Length:" + lengthint);
								//System.out.println("Temp offset:" + temp_offset);
								while (i<c.tokens_.size()-1 && 
										temp_offset + temp_length < char_offset + lengthint) {
									DeftToken dtok_next = (DeftToken) c.tokens_.get(i+1);
									temp_offset = dtok_next.char_offset;
									temp_length = dtok_next.text_.length();
									dtok_next.SetCoreferrentWithTarget(true);
									dtok_next.SetCoreferringToken(dt.tokens_.get(0));
									i+=1;
								}
							}
						}
					}
				}
						
				}
				
			}
		
		}
		
	}
	
	
	// Given a target and its mention id, add all mentions of this entity to targets
		/*	public void AddAllMentions(List<Target> targets,
					DeftTarget dt, String offset, 
					HashMap<String,List<String>> entities,
					HashMap<String,String[]> entity_mentions ) {
				
				for (String entity_id: entities.keySet()) {
					List<String> mention_offsets = entities.get(entity_id);
					if (mention_offsets.contains(offset)) {
						// found the entity
						for (String mention: mention_offsets) {
							if (entity_mentions.containsKey(mention)) {
							System.out.println("Found mention!");
							DeftTarget dt_mention = new DeftTarget();
							String mention_id = entity_mentions.get(mention)[0];
							String length = entity_mentions.get(mention)[1];
							String text = entity_mentions.get(mention)[2];
							dt_mention.SetID(mention_id);
							dt_mention.SetLength(length);
							dt_mention.SetDeftOffset(mention);
							dt_mention.SetText(text);
							dt_mention.SetSource(dt.source);
							dt_mention.SetSourceID(dt.source_ere_id);
							dt_mention.SetSourceOffset(dt.source_offset);
							dt_mention.SetSourceLength(dt.source_length);
							// TODO: For pipeline model, can do different sentiment for different metnions
							// TODO: boost by mentions
							dt_mention.SetSentiment(dt.sentiment_);
							if (!targets.contains(dt_mention)) {
								targets.add(dt_mention);
								System.out.println("Added new mention!" + text);
							}
							}
						}
					break;
					}
				}
			}*/
	
	// For antecedants that have pronominal mentions, mark their feature
	public static List<Comment> AnnotateDataWithCoreference(List<Comment> comments) {
		for (Comment c: comments) {
			for (Token t: c.tokens_) {
				if (t.corefers_with_token) {
					Token coreferring_token = t.coreferring_token;
					coreferring_token.SetSubsequentPronominal(true);
					coreferring_token.SetPronominalMention(t);
					
					/*
					// this will happen anyway
					if  (CoreferenceFeatures.IsArabicPronoun(t) 
					  || CoreferenceFeatures.ArabicObjectPronoun(t)
					  || CoreferenceFeatures.ArabicPronoun(t) ) {
					coreferring_token.SetSubsequentPronominal(true);
					coreferring_token.SetPronominalMention(t);
					} 
					// this means that if a pronoun corefers to a pronoun, include it
					// doesn't help and is messy
					else if(CoreferenceFeatures.IsArabicPronoun(coreferring_token) 
							  || CoreferenceFeatures.ArabicObjectPronoun(coreferring_token)
							  || CoreferenceFeatures.ArabicPronoun(coreferring_token) ) {
						t.SetSubsequentPronominal(true);
						t.SetPronominalMention(coreferring_token);
					}*/
					// also the position in which it corefers?, subject or object
					
					// Update corefers with target feature
					if (coreferring_token.target_offset_ >=0 ) {
						t.SetCoreferrentWithTarget(true);
					}
				}
			}
		}
	return comments;
  }
	
	// For all nominals belonging to antecedants that has pronominal mentions,
	// mark their features
	// One pronoun can corefer to many targets but each target can only refer to 
	// one pronoun
	public static List<Comment> AnnotateDataWithAllCoreference(List<Comment> comments) {
		for (Comment c: comments) {
			for (Token t: c.tokens_) {
				if (t.corefers_with_token) {
					// Update the pronominal mention for the head
					Token coreferring_token = t.coreferring_token;
					coreferring_token.SetSubsequentPronominal(true);
					coreferring_token.SetPronominalMention(t);
					if (coreferring_token.target_offset_ >=0 ) {
						t.SetCoreferrentWithTarget(true);
					}
					// Update all nominals in the antecedant
					List<Token> coreferring_tokens = t.coreferring_tokens;
					for (int i=0; i<coreferring_tokens.size(); i++) {
						Token another_coreferring_token = coreferring_tokens.get(i);
						another_coreferring_token.SetSubsequentPronominal(true);
						another_coreferring_token.SetPronominalMention(t);
						if (!t.corefers_with_target 
								&& another_coreferring_token.target_offset_ >= 0) {
							t.SetCoreferrentWithTarget(true);
						}
					}
				}
			}
		}
	return comments;
  }
	
	// For tokens in Arabic that have assumed features extracted from Madamira
	public static Boolean MatchGenderArabic(Token t1, Token t2) {
		if (t1.morph_features == null || t2.morph_features == null) {
			System.out.println("null morph features!");
			return false;
		}
		else if (t1.morph_features.containsKey("NO_ANALYSIS") 
				|| t2.morph_features.containsKey("NO_ANALYSIS")) {
			System.out.println("no analysis features!");
			return false;
		}
		// For suffix pronouns, they will have the same morph features as the token
		// but different POS, so take the suffix field from the POS
		String gen1 = t1.morph_features.get("gen");
		String gen2 = t2.morph_features.get("gen");
		if (ArabicPronoun(t1) || ArabicObjectPronoun(t1) ) {
			System.out.println("Suffix pronoun!");
			String suffix_fields = SuffixFields(t1);
			gen1 = SuffixGender(suffix_fields);
		}
		if (ArabicPronoun(t2) || ArabicObjectPronoun(t2) ) {
			System.out.println("Suffix pronoun!");
			String suffix_fields = SuffixFields(t2);
			gen2 = SuffixGender(suffix_fields);
		}
		if (!gen1.equals(gen2)) {
				System.out.println("gender mismatch!");	
		} 
		System.out.println("t1:" + gen1);
		System.out.println("t2:" + gen2);
		return (gen1.equals(gen2));
	}
	
	public static Boolean MatchPersonArabic(Token t1, Token t2) {
		if (t1.morph_features == null || t2.morph_features == null) {
			return false;
		}
		else if (t1.morph_features.containsKey("NO_ANALYSIS") 
				|| t2.morph_features.containsKey("NO_ANALYSIS")) {
			return false;
		}
		// For suffix pronouns, they will have different morph features
		String per1 = t1.morph_features.get("gen");
		String per2 = t2.morph_features.get("gen");
		if (ArabicPronoun(t1) || ArabicObjectPronoun(t1) ) {
				String suffix_fields = SuffixFields(t1);
				per1 = SuffixPerson(suffix_fields);
		}
		if (ArabicPronoun(t2) || ArabicObjectPronoun(t2) ) {
				String suffix_fields = SuffixFields(t2);
				per2 = SuffixPerson(suffix_fields);
		}
		if (!per1.equals(per2)) {
				System.out.println("person mismatch!");	
		} 
		System.out.println("t1:" + per1);
		System.out.println("t2:" + per2);
		return (per1.equals(per2));
	}
	
	public static Boolean MatchNumberArabic(Token t1, Token t2) {
		if (t1.morph_features == null || t2.morph_features == null) {
			return false;
		}
		else if (t1.morph_features.containsKey("NO_ANALYSIS") 
				|| t2.morph_features.containsKey("NO_ANALYSIS")) {
			return false;
		}
		String num1 = t1.morph_features.get("num");
		String num2 = t2.morph_features.get("num");
		if (ArabicPronoun(t1) || ArabicObjectPronoun(t1) ) {
				String suffix_fields = SuffixFields(t1);
				num1 = SuffixNumber(suffix_fields);
		}
		if (ArabicPronoun(t2) || ArabicObjectPronoun(t2) ) {
				String suffix_fields = SuffixFields(t2);
				num2 = SuffixNumber(suffix_fields);
		}
		if (!num1.equals(num2)) {
				System.out.println("number mismatch!");	
		} 
		System.out.println("t1:" + num1);
		System.out.println("t2:" + num2);
		return (num1.equals(num2));
	}
	// ARABIC PRONOUN MEANS: HIYA HOUWA N7N HM HNNA (NOT CLITIC)
	public static Boolean IsArabicPronoun(Token t) {
		//return (t.morph_features!=null && !t.morph_features.containsKey("NO_ANALYSIS")
			//	& t)
		return t.pos_.equals("pron");
	}
	// ARABIC SUFFIX POSSESSIVE PRONOUN (e.g ktAbhA) OR PRONOUN (e.g EalaYha)
	/*not* OBJECT PRONOUN (e.g yjrhA) */
	public static Boolean ArabicPronoun(Token t) {
		return t.pos_.contains("PRON");
	}
	// ARABIC OBJECT PRONOUN (e.g yjrhA) IVSUFF or (ma anjazh)
	public static Boolean ArabicObjectPronoun(Token t) {
		return (t.pos_.startsWith("IVSUFF_DO") 
				|| t.pos_.startsWith("PVSUFF_DO"));
	}

	// POS for suffix pronouns looks something like this: 
	// POSS_PRON_3FS or PRON_3FS or POSS_PRON_1P
	public static String SuffixFields (Token t) {
		if (t.pos_ == null || (!ArabicPronoun(t) && !ArabicObjectPronoun(t)) ) {
			return null;
		} else  {
			String[] fields = null;
			if (ArabicPronoun(t)) {
			  fields = t.pos_.split("_");
			} else if (ArabicObjectPronoun(t)) {
				fields = t.pos_.split(":");
			}
			return fields[fields.length-1];
		}
	}
	public static String SuffixGender(String fields) {
		String gender = "";
		if (fields.length() == 3) {
			 gender = fields.substring(1, 2);
			 if (gender.equals("M") || gender.equals("F")) {
					return gender.toLowerCase();
				} else {
					return "na";
				}
		}  else {
			 return "na";
		 }
	}
	public static String SuffixPerson(String fields) {
		   String person = fields.substring(0, 1);
		   if (person.equals("1") || person.equals("2") || person.equals("3")) {
		   return person;
		   } else {
			   return "na";
		   }
	}
   public static String SuffixNumber(String fields) {
	   String number = fields.substring(fields.length()-1, fields.length());
	   if (number.equals("S") || number.equals("P")) {
		   return number.toLowerCase();
	   } else {
		   return "na";
	   }
	}
	
	/*public static String PronounGender(Token t) {
		
	}*/
	/*Maybe clitics shouldn't get the same target annotation as their content word. Only if Al+*/
	/*Change it only in increase training data?*/ /*Since annotations are performed on the whole word before tok*/
	// Note that in the dependency features the suffix get a different dependency rep than the word
	// If it is an OBJ pronoun (or any pronoun) then we have to find the entity that refers to and predict that to be a target!
	// Easier to do for now, if it's already annotated as a target, find the pronouns that could refer to it & mark those
	// as targets in training data (Or replace them by the original named target)
}

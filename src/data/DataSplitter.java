/**
 * 
 */
package data;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Narnoura
 * Given a set of comments, create splits for train, test, and dev.
 */
public class DataSplitter {
	
	public List<Comment> train_set;
	public List<Comment> test_set;
 	public List<Comment> dev_set;
 	
 	public DataSplitter() {
 		
 	};
 	
 	/* Split comments by given percentages given in % */
 	/* Does not modify comments */
 	public void SplitComments(List<Comment> comments, int train, int test,
 			int dev) {
 		List<Comment> shuffled = comments;
 		Collections.shuffle(shuffled);
 		train_set = new ArrayList<Comment>();
 		test_set = new ArrayList<Comment>();
 		dev_set = new ArrayList<Comment>();
 		int t = (int) (train * 0.01 * shuffled.size());
 		int d = (int) (dev * 0.01 * shuffled.size());
 		int b = (int) (test * 0.01 * shuffled.size());
 		System.out.println("t: " + t + " d: " + d + " b: " + b);
 		for (int i=0; i<t; i++){
 			train_set.add(shuffled.get(i));
 		}
 		for (int i=t+1;i<(t+d); i++){
 			dev_set.add(shuffled.get(i));
 		}
 		for (int i=(t+d+1); i<comments.size(); i++){
 			test_set.add(shuffled.get(i));
 		}
 		//assert(comments.size() == (train_set.size() + test_set.size() + dev_set.size()));
 		System.out.println("Comments size: " + shuffled.size());
 		System.out.println("Train size: " + train_set.size());
 		System.out.println("Dev size: " + dev_set.size());
 		System.out.println("Test size: " + test_set.size());
 	}
}

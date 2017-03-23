package eval;

import java.util.List;
import java.util.Random;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import util.BuckwalterConverter;
import util.FileWriter;
import util.Tokenizer;
import data.Comment;
import data.DeftTarget;
import data.Target;
import data.Token;
import data.Comment;

// Calculate significance using approxmiate randomization

public class ApproximateRandomization {
	
	List<Comment> method_1_comments;
	List<Comment> method_2_comments;
	List<Target> method_1_targets; // string?
	List<Target> method_2_targets;
	//List<Target> different_targets;
	//List<List<Target>> same_targets; 
	HashMap<String,Double> significance_thresholds;
	List<Comment> gold_comments;
	
	public double N = 1048576; // compare n_difference with N
	public static int MAX = 1;
	public static int MIN = 0;
	
	public String match_type = "subset-overlap";
	
	// For sentiment evaluation
	// public List<List<Target>> matching_targets;
	
	public void SetGoldComments(List<Comment> comments){
		this.gold_comments = comments;
	}
	
	public void SetMatchType(String match_type) {
		this.match_type = match_type;
	}
	
	public ApproximateRandomization(List<Comment> method_1_comments, List<Comment> method_2_comments,
			String thresholds) {
		significance_thresholds = new HashMap<String,Double>();
		//same_targets = new ArrayList<List<Target>>();
		this.method_1_comments = method_1_comments;
		this.method_2_comments = method_2_comments;
		System.out.println("thresholds:"+thresholds);
		String[] sig_thresholds = thresholds.split(",");
		for (String thresh: sig_thresholds){
			String[] fields = thresh.split("=");
			significance_thresholds.put(fields[0], Double.valueOf(fields[1]));
		}
	}
	
	public HashMap<String,Double> GetSignificance () {
		
		HashMap<String,Double> significance = new HashMap<String,Double>();
		
		// Initialize deltas in signifiance thresholds
		double delta_r = 0;
		double delta_p = 0;
		double delta_f = 0;
		double delta_acc_sent = 0;
		double delta_acc_all = 0;
		double delta_f_all = 0;
		
		// Initialize counts that meet significance criteria
		double nc_r = 0;
		double nc_p = 0;
		double nc_f = 0;
		double nc_acc_sent = 0;
		double nc_acc_all = 0;
		double nc_f_all = 0;
		
		// Get significance thresholds
		for (String measure: significance_thresholds.keySet()) {
			if (measure.equals("r")) {
				delta_r = significance_thresholds.get(measure);
				System.out.println("delta_r:"+delta_r);
			} else if (measure.equals("p")) {
				delta_p = significance_thresholds.get(measure);
				System.out.println("delta_p:"+delta_p);
			} else if (measure.equals("f")) {
				delta_f = significance_thresholds.get(measure);
				System.out.println("delta_f:"+delta_f);
			} else if (measure.equals("acc_sent")) {
				delta_acc_sent = significance_thresholds.get(measure);
				System.out.println("delta_acc_sent:"+delta_acc_sent);
			} else if (measure.equals("acc_all")) {
				delta_acc_all = significance_thresholds.get(measure);
				System.out.println("delta_acc_all:"+delta_acc_all);
			} else if (measure.equals("f_all")) {
				delta_f_all = significance_thresholds.get(measure);
				System.out.println("delta_f_all:"+delta_f_all);
			}
		}
		
		List<Double> scores_1 = new ArrayList<Double>();
		List<Double> scores_2 = new ArrayList<Double>();
		Evaluator e_1 = new Evaluator();
		Evaluator e_2 = new Evaluator();
		
		 
		// Loop over randomized trials
		for (int i=0; i<N; i++) {
		 System.out.println(i);
		 int num_different = 0;
		 List<Comment> randomized_1 = new ArrayList<Comment>();
		 List<Comment> randomized_2 = new ArrayList<Comment>();
		 
		 for (Comment c1: method_1_comments){
			   Comment r1 = c1;
			   Comment c2 = method_2_comments.get(method_1_comments.indexOf(c1));
			   Comment r2 = c2;
			   List<Target> r1_targets = new ArrayList<Target>();
			   List<Target> r2_targets = new ArrayList<Target>();
				for (Target t1: c1.targets_){
					boolean found = false;
					for (Target t2: c2.targets_) {
						if (t1.text_.equals(t2.text_)) {
							List<Target> pair  = new ArrayList<Target>();
							pair.add(t1);
							pair.add(t2);
							//this.same_targets.add(pair);
							found = true;
						}
					}
					// Recalled by m1 but not m2
					if (found == false) {
							// generate random number
							Random random = new Random();
							int randomNum = random.nextInt(MAX - MIN + 1) + MIN;
							if (randomNum == 0) {
							  r1_targets.add(t1);
							  }  
							else if (randomNum == 1){
							 r2_targets.add(t1);
							 }
							  num_different+=1;
						} else{
							r1_targets.add(t1); // keep the targets that are the same, ignore duplicates
							r2_targets.add(t1);
						}
						
					}
				
			 for (Target t2: c2.targets_) {
				boolean found = false;
				for (Target t1: c1.targets_) {
					if (t2.text_.equals(t1.text_)) {
						// no need to add duplicates
						/*List<Target> pair  = new ArrayList<Target>();
						pair.add(t1);
						pair.add(t2);
						this.same_targets.add(pair);*/
						found = true;
					}
				}
					// Recalled by m2 but not m1
				 	if (found == false) {
						Random random = new Random();
						int randomNum = random.nextInt(MAX - MIN + 1) + MIN;
						if (randomNum == 0) {
						  r1_targets.add(t2);
						  }  else {
						  r2_targets.add(t2);
						  }
						 num_different+=1;
				 	} else {
				 	// no need to add duplicates
				 	}
				
			}
			r1.SetTargets(r1_targets);
			r2.SetTargets(r2_targets);
			randomized_1.add(r1);
			randomized_2.add(r2);
		}
	
		// Output just once for debugging 
		// Also, we don't really need to add same targets
		if (num_different == 0 ) {
			System.out.println("Predictions for methods 1 and 2 are identical. Exiting");
			System.exit(0);
		}
		/*if (num_different <= 20) {
			N = Math.pow(2,num_different);
			System.out.println("Only " + num_different + "different targets!");
			System.out.println("You should really use an exact randomization for these methods\n");
		} else {
			System.out.println("Number of different targets:" + num_different);
			//System.out.println("Number of same targets:" + this.same_targets.size());
		}*/
		
		// Evaluate randomized_1, randomized_2
		e_1.Set(randomized_1, gold_comments);
		e_2.Set(randomized_2, gold_comments);
		
		scores_1 = e_1.Evaluate("subset-overlap");
		scores_2 = e_2.Evaluate("subset-overlap");
		// add overall sentiment and overall f-measure to scores
		
		// for each score, compare to delta and increment
		double r_1 = scores_1.get(0);
		double p_1 = scores_1.get(1);
		double f_1 = scores_1.get(2);
		double acc_sent_1 = scores_1.get(6);
		double acc_all_1 = scores_1.get(7);
		double f_all_1 = scores_1.get(8);
		
		double r_2 = scores_2.get(0);
		double p_2 = scores_2.get(1);
		double f_2 = scores_2.get(2);
		double acc_sent_2 = scores_2.get(6);
		double acc_all_2 = scores_2.get(7);
		double f_all_2 = scores_2.get(8);
		
		// should it be abs or absolute?
		
		if ( delta_r >=0 && 100 *(r_2 - r_1)/r_1 >= (delta_r) || 
				delta_r < 0 && 100 *(r_2 - r_1)/r_1 <= (delta_r)) {
			nc_r +=1;
		}
		if ( delta_p >=0 && 100 * (p_2 - p_1)/p_1 >= (delta_p) || 
				delta_p < 0 && 100 * (p_2 - p_1)/p_1 <= (delta_p)) {
			nc_p +=1;
		}
		if ( delta_f >=0 && 100 * (f_2 - f_1)/f_1 >= (delta_f)  || 
				delta_f < 0 && 100 * (f_2 - f_1)/f_1 <= (delta_f) ) {
			nc_f +=1;
		}
		if ( delta_acc_sent >=0 && 100 * (acc_sent_2 - acc_sent_1)/acc_sent_1 >= (delta_acc_sent) || 
				delta_acc_sent < 0 && 100 * (acc_sent_2 - acc_sent_1)/acc_sent_1 <= (delta_acc_sent) ) {
			nc_acc_sent +=1;
		}
		if ( delta_acc_all >= 0 && 100 * (acc_all_2 - acc_all_1)/acc_all_1 >= (delta_acc_all) || 
				delta_acc_all < 0 &&  100 * (acc_all_2 - acc_all_1)/acc_all_1 <= (delta_acc_all)  ) {
				nc_acc_all +=1;
		}
		if ( delta_f_all >=0 && 100 * (f_all_2 - f_all_1)/f_all_1 >= (delta_f_all) || 
				delta_f_all < 0 && 100 * (f_all_2 - f_all_1)/f_all_1 <= (delta_f_all) ) {
				nc_f_all +=1;
		}
	 }
		
	significance.put("r", (nc_r+1)/(N+1) );
	significance.put("p", (nc_p+1)/(N+1) );
	significance.put("f", (nc_f+1)/(N+1) );
	significance.put("acc_sent", (nc_acc_sent+1)/(N+1) );
	significance.put("acc_all", (nc_acc_all+1)/(N+1) );
	significance.put("f_all", (nc_f_all+1)/(N+1) );
	
	System.out.println("Significance measures");
	for (String metric: significance.keySet()) {
		System.out.println(metric + ":" + significance.get(metric));
	}

		
	return significance;
		
}
	
	public void Print(String output_path, HashMap<String,Double> significance) {
		List<String> significance_output = new ArrayList<String>();
		for (String metric: significance.keySet()) {
			significance_output.add(metric + ":" + significance.get(metric));
		}
		FileWriter.WriteToFile(significance_output, output_path);
	}
	
	
	
	/*public static void main (String args[]) {
		
		// args 1: file for method 1
		// args 2: file for method 2
		// args 3: delta in f-measure (or % difference)
		// args 4: delta in recall
		// args 5: delta in precision
		// args 6: delta in sentiment accuracy
		// args 7: delta in overall sentiment accuracy
		// arrgs 8: delta in overall f-measure?
		String meth1file = "";
		String meth2file = "";
		Double delta_f_targets = 0.0;
		Double delta_r_targets = 0.0;
		Double delta_p_targets = 0.0;
		Double delta_sent_acc = 0.0;
		Double delta_overall_sent_acc = 0.0;
		Double delta_overall_f = 0.0;
		
		for (int i=0;i<args.length;i++) {
			if (args[i].startsWith("meth1=")) {
				
			}
		}
		
		// take in output targets from 2 methods (best:read in from two output files using
		// function I already have)
		// take delta as input ( how much should difference be)
		
		// use python script I have for boostrap??
		
		// loop from 1 to 2^20
		// for each, randomly assign 'disagreed targets' to method1 (0) and method2 (1)
		// so just generate a random number between 0 and 1
		// 
		// call evaluate on that, somewhow (For comments or directly targets)
		// increment nb. of times delta is exceeded, for each measure 
		// (actually seems very similar to the bootstrap paper - check)
	}*/

	
	/* old method
	 * 
	 * // extract targets out of comments
		for (Comment c1: method_1_comments){
			for (Target t1: c1.targets_){
				method_1_targets.add(t1);
			}
		}
		for (Comment c2: method_2_comments){
			for (Target t2: c2.targets_){
				method_2_targets.add(t2);
			}
		}
	 * 
	 * 	// Find different targets
		//
		// Should we evaluate using comments and not targets? for order?
		// can reconstruct them back using comment ids
		boolean found = false;
		for (Target t1: method_1_targets){
			found = false;
			for (Target t2: method_2_targets) {
				if (t1.text_.equals(t2.text_)) {
					List<Target> pair  = new ArrayList<Target>();
					pair.add(t1);
					pair.add(t2);
					this.same_targets.add(pair);
					found = true;
				}
			}
			if (found == false) {
				this.different_targets.add(t1);
			}
		}
		for (Target t2: method_2_targets){
			found = false;
			for (Target t1: method_1_targets) {
				if (t2.text_.equals(t1.text_)) {
					// should have been already added. I will ignore duplicates
					// because the evaluation ignores them anyway.
					found = true;
				}
			}
			if (found == false) {
				this.different_targets.add(t2);
			}
		}
	 */


}

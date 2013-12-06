package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.Math;

import nlp.ling.Tree;
import nlp.util.CounterMap;

class BaselineCkyParser implements Parser {

	CounterMap<List<String>, Tree<String>> knownParses;
	CounterMap<Integer, String> spanToCategories;

	TreeAnnotator annotator;

	Lexicon lexicon;
	Grammar grammar;

	UnaryClosure unaryClosure;

	static class Chart {
		/*
		 * TODO This class (and enclosed EdgeInfo) needs to be changed to keep
		 * track of unary rules (and unary chains) used
		 */

		static class EdgeInfo {
			double score;
			UnaryRule unRule = null;
			BinaryRule rule = null;
			int mid = -1;

			EdgeInfo(double score) {
				this.score = score;
			}

			@Override
			public String toString() {
				if (rule == null) {
					return Double.toString(score);
				} else {
					return score + ": " + "[ rule = " + rule + ", mid = " + mid
							+ "]";
				}

			}
		}

		Map<Integer, Map<Integer, Map<String, EdgeInfo>>> chart = new HashMap<Integer, Map<Integer, Map<String, EdgeInfo>>>();

		Chart(int seqLength) {
			for (int i = 0; i < seqLength; i++) {
				chart.put(i, new HashMap<Integer, Map<String, EdgeInfo>>());
				for (int j = i + 1; j <= seqLength; j++) {
					chart.get(i).put(j, new HashMap<String, EdgeInfo>());
				}
			}
		}

		/*----------------------1------------------------------*/
		void set(int i, int j, String label, double score) {
			EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
			if (edgeInfo == null) {
				edgeInfo = new EdgeInfo(score);
			} else {
				edgeInfo.score = score;
			}
			chart.get(i).get(j).put(label, edgeInfo);
		}

		/*----------------------/1------------------------------*/

		double get(int i, int j, String label) {
			Map<String, EdgeInfo> edgeScores = chart.get(i).get(j);
			if (!edgeScores.containsKey(label)) {
				return 0;
			} else {
				return edgeScores.get(label).score;
			}
		}

		Set<String> getAllCandidateLabels(int i, int j) {
			return chart.get(i).get(j).keySet();
		}

		String getBestLabel(int i, int j) {
			Map<String, EdgeInfo> edgeScores = chart.get(i).get(j);
			double bestScore = Double.NEGATIVE_INFINITY;
			String optLabel = null;
			for (String label : edgeScores.keySet()) {
				if (bestScore < edgeScores.get(label).score) {
					optLabel = label;
					bestScore = edgeScores.get(label).score;
				}
			}
			return optLabel;
		}

		/*----------------------2------------------------------*/
		void setBackPointer(int i, int j, String label, UnaryRule rule) {
			EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
			edgeInfo.unRule = rule;
		}

		/*----------------------/2------------------------------*/

		void setBackPointer(int i, int j, String label, BinaryRule rule,
				int midPoint) {
			EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
			edgeInfo.rule = rule;
			edgeInfo.mid = midPoint;
		}

		int getMidPoint(int i, int j, String label) {
			return chart.get(i).get(j).get(label).mid;
		}

		/*---------------------3-------------------------------*/
		UnaryRule getUnRule(int i, int j, String label) {
			return chart.get(i).get(j).get(label).unRule;
		}
		/*---------------------/3-------------------------------*/

		BinaryRule getRule(int i, int j, String label) {
			return chart.get(i).get(j).get(label).rule;
		}

		@Override
		public String toString() {
			return chart.toString();
		}

	}

	/*------------------------4----------------------------
	protected <L> Tree<L> buildUnaryTree(List<L> path,
			List<Tree<L>> leafChildren) {
		List<Tree<L>> trees = leafChildren;
		for (int k = path.size() - 1; k >= 0; k--) {
			trees = Collections.singletonList(new Tree<L>(path.get(k), trees));
		}
		return trees.get(0);
	}
	-----------------------/4-----------------------------*/

	void traverseBackPointersHelper(List<String> sent, Chart chart, int i,
			int j, Tree<String> currTree) {
		String parent = currTree.getLabel();
		/**
		 * TODO This method needs to be updated to keep print out unary rules
		 * used
		 */

		/*---------------------5-------------------------------*/
		UnaryRule unaryRule = chart.getUnRule(i, j, parent);
		
		if (unaryRule != null) {
			
			
			List<String> unPath = unaryClosure.getPath(chart.getUnRule(i, j,
					parent));
			
			for (String node : unPath) {
				if (node == parent)
					continue;
				List<Tree<String>> children = new ArrayList<Tree<String>>(1);
				System.out.println(node);
				Tree<String> tree = new Tree<String>(node);
				children.add(tree);
				currTree.setChildren(children);
				parent = node;
				currTree = tree;
			}
		}
		/*---------------------/5-------------------------------*/
		
		
		//else
		// binary rules
		if (j - i > 1 ) {
			
			//System.out.println("IM BINARY!!");
			BinaryRule rule = chart.getRule(i, j, parent);
			// assert rule != null;
			int mid = chart.getMidPoint(i, j, parent);
			List<Tree<String>> children = new ArrayList<Tree<String>>(2);
			
			Tree<String> t1 = new Tree<String>(rule.getLeftChild());
			traverseBackPointersHelper(sent, chart, i, mid, t1);
			children.add(t1);

			Tree<String> t2 = new Tree<String>(rule.getRightChild());
			//System.out.println("t1 child:");
			//System.out.println(t1.getChildren().size());
			traverseBackPointersHelper(sent, chart, mid, j, t2);
			children.add(t2);

			currTree.setChildren(children);
			//System.out.println("binary children size:");
			//System.out.println(children.size());
			
			
		} 
		// preterminal production
		else {
			assert j - i == 1;
			System.out.println("IM TERMINAL!!");
			Tree<String> termProd = new Tree<String>(sent.get(i));
			currTree.setChildren(Collections.singletonList(termProd));
		}

	}

	// traverse back pointers and create a tree
	Tree<String> traverseBackPointers(List<String> sentence, Chart chart) {

		Tree<String> annotatedBestParse;
		if (!chart.getAllCandidateLabels(0, sentence.size()).contains("ROOT")) {
			// this line is here only to make sure that a baseline without
			// binary rules can output something
			annotatedBestParse = new Tree<String>(chart.getBestLabel(0,
					sentence.size()));
		} else {
			// in reality we always want to start with the ROOT symbol of the
			// grammar
			annotatedBestParse = new Tree<String>("ROOT");
		}
		traverseBackPointersHelper(sentence, chart, 0, sentence.size(),
				annotatedBestParse);
		return annotatedBestParse;

	}

	public Tree<String> getBestParse(List<String> sentence) {

		/*
		 * TODO This method needs to be extended to support unary rules The
		 * UnaryClosure class should simplify this task substantially
		 */

		// note that chart.get(i, j, c) translates to chart[i][j][c] we used in
		// the slides

		Chart chart = new Chart(sentence.size());

		// preterminal rules
		for (int k = 0; k < sentence.size(); k++) {
			for (String preterm : lexicon.getAllTags()) {
				if (lexicon.scoreTagging(sentence.get(k), preterm) > 0)
					chart.set(k, k + 1, preterm,
							lexicon.scoreTagging(sentence.get(k), preterm));

			}
		}
		/*----------------------6------------------------------
		for (int k = 0; k < sentence.size(); k++) {
			for (String parent : grammar.states) {
				double bestScore = Double.NEGATIVE_INFINITY;
				UnaryRule optRule = null;

				for (UnaryRule rule : unaryClosure
						.getClosedUnaryRulesByParent(parent)) {
					double score = chart.get(k, k + 1, rule.getChild());
					double currScore = score * rule.getScore();
					if (currScore > bestScore) {
						bestScore = currScore;
						optRule = rule;

					}
				}
				if (bestScore != Double.NEGATIVE_INFINITY) {
					chart.set(k, k + 1, parent, bestScore);
					chart.setBackPointer(k, k + 1, parent, optRule);
				}
			}
		}
		-----------------------/6-----------------------------*/
		
		// CKY for binary trees
		for (int max = 1; max <= sentence.size(); max++) {
			for (int min = max - 1; min >= 0; min--) {
				// first without unary roles
				for (String parent : grammar.states) {
					double bestScore = Double.NEGATIVE_INFINITY;
					int optMid = -1;
					BinaryRule optRule = null;
					// parent -> c1 c2
					for (BinaryRule rule : grammar
							.getBinaryRulesByParent(parent)) {
						for (int mid = min + 1; mid < max; mid++) {
							double score1 = chart.get(min, mid,
									rule.getLeftChild());
							double score2 = chart.get(mid, max,
									rule.getRightChild());
							double currScore = score1 * score2
									* rule.getScore();
							if (currScore > bestScore) {
								bestScore = currScore;
								optMid = mid;
								optRule = rule;
								;
							}
						}
					}
					if (bestScore != Double.NEGATIVE_INFINITY) {
						chart.set(min, max, parent, bestScore);
						chart.setBackPointer(min, max, parent, optRule, optMid);
					}
				}
				/*--------------------7--------------------------------*/
				// unary
				for (String parent : grammar.states) {
					double bestScoreUnary = chart.get(min, max, parent);
					UnaryRule optRuleUnary = null;
					for (UnaryRule rule : unaryClosure
							.getClosedUnaryRulesByParent(parent)) {
						double score = chart.get(min, max, rule.getChild());
						double currScore = score * rule.getScore();
						if (currScore > bestScoreUnary) {
							bestScoreUnary = currScore;
							optRuleUnary = rule;
						}
					}

					if (optRuleUnary != null) {
						chart.set(min, max, parent, bestScoreUnary);
						chart.setBackPointer(min, max, parent, optRuleUnary);
					}
				}
				/*--------------------/7--------------------------------*/
			}
		}

		// use back pointers to create a tree
		Tree<String> annotatedBestParse = traverseBackPointers(sentence, chart);

		return annotator.unAnnotateTree(annotatedBestParse);
	}

	public BaselineCkyParser(List<Tree<String>> trainTrees,
			TreeAnnotator annotator) {

		this.annotator = annotator;

		System.out.print("Annotating / binarizing training trees ... ");
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);

		System.out.println("done.");

		System.out.print("Building grammar ... ");
		grammar = new Grammar(annotatedTrainTrees);
		lexicon = new Lexicon(annotatedTrainTrees);
		System.out.println("done. (" + grammar.getStates().size() + " states)");

		// use the unary closure to support unary rules in the CKY algorithm
		unaryClosure = new UnaryClosure(grammar);

	}

	private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			annotatedTrees.add(annotator.annotateTree(tree));
		}
		return annotatedTrees;
	}

	List<String[]> word_tokens = new ArrayList<String[]>();

	@Override
	public double getLogScore(Tree<String> tree) {
		Tree<String> annotatedTree = annotator.annotateTree(tree);

		double logSum = 0;

		word_tokens.clear();
		traverseAnnotatedTree(annotatedTree);

		for (int i = 0; i < word_tokens.size(); i++) {
			/*
			 * System.out.println(i); System.out.println(word_tokens.get(i)[0] +
			 * "\t\t\t\t"+ word_tokens.get(i)[1]+ "\t\t\t\t"+
			 * word_tokens.get(i)[2]+ "\t\t\t\t"+ word_tokens.get(i)[3] );
			 */
			logSum += Double.parseDouble(word_tokens.get(i)[3]);
		}
		return logSum;
	}

	public Tree<String> traverseAnnotatedTree(Tree<String> tree) {
		if (tree.isLeaf()) {
			return tree;
		}

		List<Tree<String>> childrenList = tree.getChildren();

		if (childrenList.size() == 1) {
			Tree<String> child = childrenList.get(0);
			String[] params0 = new String[4];
			params0[0] = tree.getLabel();
			params0[1] = child.getLabel();
			params0[2] = "1";
			params0[3] = String.valueOf(grammar.getUnScore(tree.getLabel(),
					child.getLabel()));

			if (child.isLeaf()) {
				params0[2] = "lexicon";
				params0[3] = String.valueOf(Math.log(lexicon.scoreTagging(
						child.getLabel(), tree.getLabel())));
			}

			word_tokens.add(params0);
			traverseAnnotatedTree(child);
		} else if (childrenList.size() == 2) {
			Tree<String> leftChild = childrenList.get(0);
			Tree<String> rightChild = childrenList.get(1);

			String[] params1 = new String[4];
			params1[0] = tree.getLabel();
			params1[1] = leftChild.getLabel();
			params1[2] = "2";
			params1[3] = String.valueOf(grammar.getBiScore(tree.getLabel(),
					leftChild.getLabel(), rightChild.getLabel()));

			word_tokens.add(params1);
			traverseAnnotatedTree(leftChild);

			String[] params2 = new String[4];
			params2[0] = tree.getLabel();
			params2[1] = rightChild.getLabel();
			params2[2] = "2";
			params2[3] = "0";

			word_tokens.add(params2);
			traverseAnnotatedTree(rightChild);
		}
		return tree;
	}
}
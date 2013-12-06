ELPL_Assignments
================
--Extract all files


--Set Path to data files and run arguments for assignment 1

-----path "<path>\assignment-1-src-and-data\data" -scoring-mode


--Set Path to data files and run arguments for assignment 2

-----path "<path>\assignment-1-src-and-data\data" -lossless-binarization


--Set main class for both assignments as:

----nlp.assignments.parsing.PCFGParserTester


##Assignment 1: Compute log probability of a tree

--Changes in [BaselineCkyParser.java]

----Added function to traverse through tree recursively <traverseAnnotatedTree(Tree<String> tree)>

------Traverses tree and saves Parent->child nodes ArrayList<String[]> type list as 
          <String parent, String child, String number_of_children, String log_probability>

----Modified <getLogScore(Tree<String> tree)> function to add log probabilities of unary and binary rules from annotated tree



--Changes in [Grammar.java]

----Added function <getBiScore(String parent, String left_child, String right_child)>

------From <grammar.java>, gets a list of binary rules that have Parent as parent corresponding to left_child and right_child and returns the Log10 score of that rule.

----Added function <getUnScore(String parent, String child)>


------From <grammar.java>, gets a list of unary rules that have Parent as parent corresponding to single child and returns log10 score of that rule.


_______________________________________________________________________________________

##Assignment 2: Getting the best parse tree

--Changes in [BaselineCkyParser.java]

----Modified the class <EdgeInfo>, so as to unary rules can be kept

----Modified the method <set> of class chart to add new best score

----Modified the method <setBackPointer> to support unary rules

----Modified the methods <getBestParse> and traverseBackPointersHelper> to support unary rules
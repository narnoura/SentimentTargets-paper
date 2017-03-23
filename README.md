# SentimentTargets-paper

## Make sure CRF++ is installed (C++ compiler required)

https://taku910.github.io/crfpp/#install


## How to run the scripts

Go to scripts directory

./runcrf++pipeline <experiment-name> <List of features> <cluster-file>

All outputs will be saved in the log files under experiments/paper-experiments/<experiment-name-folder>


1)  To run the full best-linguistic pipeline with full segmentation (D3) mode:
./runcrf++pipeline best-linguistic-D3 Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment

With clusters:

./runcrf++pipeline best-linguistic+clusters-D3 Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment,WordClusters ../data/word-clusters/ar-wiki-classes-500-lemma+D3.sorted.txt 

2) To run the full best-linguistic pipeline with partial segmentation (ATB) mode:

./runcrf++pipeline-ATB best-linguistic-ATB Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment 

With clusters:

./runcrf++pipeline-ATB best-linguistic+clusters-ATB Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment,WordClusters ../data/word-clusters/ar-wiki-classes-250-lemma+ATB.sorted.txt

3)  To run the full best linguistic pipeline (D3+ATB) with  D3 for targets and ATB for sentiment:

./runcrf++pipelineD3+ATB best-linguistic-D3+ATB Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment 

With clusters:

./runcrf++pipelineD3+ATB best-linguistic+clusters-D3+ATB Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment,WordClusters ../data/word-clusters/ar-wiki-classes-8000-lemma+D3.sorted.txt

To specify a variable number of clusters:

./runcrf++pipelineD3+ATBwithk best-linguistic+clusters-k-D3+ATB Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment,WordClusters

4) To run without any segmentation/tokenization:

For lemma and pos:

./runcrf++pipeline-notok lemma+pos Lex,MadamiraPOS

For lemma and surface word:

./runcrf++pipeline-notok surface+pos Surface,MadamiraPOS

Note that you need to uncomment the extra features in the template files (template_sentiment and template_targets) so that only features for word and pos are used by the CRF.

5) For low-resource cluster experiments (only lemma+cluster) use these scripts:

./runcrf++pipelineclusters lemma+pos+clusters Lex,MadamiraPos,WordClusters <cluster file> <k> <tokenization option: notok, D3, ATB>

To run all low-resource cluster experiments, use this script as a guide:

./runclusterexperiments

Note that for low-resource experiments, you need to uncomment the extra features in the template files.

6) To run the model on blind test data:

./runcrf++pipelinetest <experiment-name> <feature list> <cluster file>
This script is currently configured to test in D3+ATB mode. 

7) For a guide to running all paper experiments, use this file:

runcrf++pipelineexperiments

Baseline experiments use the MPQA lexicon by default. 

## Location of data:

You can find the (unprocessed) train and test xml files here:

data/arabic-finegrained

## Location of processed madamira files:

You can find the preprocessed madamira files for all train and test data here:

data/madamira-files

## Location of word clusters:

You can find all the word clusters here:

data/word-clusters

## Location of dependency parses:

You can find the Catib dependency parses for all the data here:

data/catib-parses

## Location of Stanford parses:

data/stanford-parses

You can find the stanford syntactic parses for the data here:

These are used for the All-NP baseline.



For questions or issues, please contact noura@cs.columbia.edu

#!/bin/bash

# iterate through QuickUMLS options for similarity_name; best_match and overlapping_criteria
declare -a StringArray=("jaccard")
for s in ${StringArray[@]}; do
	declare -a StringArray=("score")
	for o in ${StringArray[@]}; do
		declare -a StringArray=("false")
		for b in ${StringArray[@]}; do
			echo $s $o $b
    		python run_qumls.py $s $o $b
		done
	done
done

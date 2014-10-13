#!/bin/bash
if [ "$1x" == "x" ];then
	echo "splice.sh DIR TIME"
	exit 1
fi
dir=$1
dest=$2
length=$3
OIFS="$IFS"
IFS=$'\n'
if [ -d "$dir" ];then
	[ -d "$dest" ] || mkdir -p $dest
	for file in `find $dir -name "*.wav" -type f`
	do
		fn=`basename $file`
		avconv -i $file -t 5 $dest/$fn 
	done
else
	echo "$1 not dir"
	exit 1
fi

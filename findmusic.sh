#!/bin/bash
for f in /home/kevin/Music/*.wav
do
	fn=${f##/*/}
	n=`ls -l /home/kevin/Documents/offrecording_15/ | grep "$fn" |wc -l`
	for f2 in /home/kevin/Documents/offrecording_15/*.wav;do
		n=`echo $f2|grep "$fn" |wc -l`
		if [ "$n" == "1" ];then
			cp "$f2" /home/kevin/Documents
		fi
	done
done

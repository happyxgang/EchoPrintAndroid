#!/bin/bash
rm /home/kevin/Documents/testfiles/*.wav
for f in /home/kevin/Documents/testfiles_convert/*.wav; do
	fn=${f##/*testfiles_convert/}
	fn=${fn%*.wav*}
	fn=`expr "$fn" : '\(.*.wav\)'`
	desc=`cat /home/kevin/Desktop/id_name | grep "$fn"`
	if [ "$desc" != "" ];then
		echo $desc
		i=${desc##*,}	
		mv "$f" "/home/kevin/Documents/testfiles/$i-$fn"
	fi
done

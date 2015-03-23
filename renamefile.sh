#!/bin/bash
for f in /home/kevin/Documents/testfiles_wav/testfiles_convert/*.wav; do
	fn=${f##/*testfiles_convert/}
	#fn=${fn%*.wav*}
	#fn=`expr "$fn" : '\(.*.wav\)'`
	desc=`cat /home/kevin/Desktop/id_name | grep "$fn"`
	if [ "$desc" != "" ];then
		i=${desc##*,}	
		mv "$f" "/home/kevin/Documents/testfiles_wav/testfiles_convert/$i-$fn"
	fi
done

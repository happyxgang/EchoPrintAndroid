#!/bin/bash
rm /home/kevin/Documents/testfiles_convert/*.wav
for f in /media/文档/AudioRelated/record_15/*.wav;
do
	fn=${f##/*record_15/}
	avconv -i "$f" -ac 1 -b 16000 -ar 8000 /home/kevin/Documents/testfiles_convert/"$fn"
done

#!/bin/bash
#for f in /media/software/wav
#do
#fn=${f##/*/}
#n=`ls -l /home/kevin/Documents/offrecording_15/ | grep "$fn" |wc -l`
for f2 in /home/kevin/Documents/offrecording_15/*.wav
do
    fn2=${f2##/*/}
    fn2=${fn2%\.2013*wav}
    #echo $fn2
    n=`find /media/software/wav/ -name "$fn2"`
    echo $n
    #if [ "$n" == "1" ];then
        #echo /home/kevin/Documents/offrecording_15/"$fn2" #/home/kevin/Documents/testfiles/
    #fi
done
#done

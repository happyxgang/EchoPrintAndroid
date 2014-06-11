#!/bin/bash
if [ "$1x" == "x" ]; then
	dbnum=0
else
	dbnum=$1
fi
echo "using db: $dbnum"
redis-cli -n $dbnum flushdb
base="/home/kevin/Desktop/redis_script"
cat $base | redis-cli -n $dbnum --pipe
python /home/kevin/workspace/python/fpserver/addsong_id.py $dbnum
#for f in `ls $base`; do
#	if [[ $f == redis* ]]; then
#		continue
#	fi	
#	cat $base/$f | redis-cli --pipe
#done

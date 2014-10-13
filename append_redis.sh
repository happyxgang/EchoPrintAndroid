#!/bin/bash
dbnum=`cat /home/kevin/workspace/EchoprintForAndroid/EchoprintTest/src/com/xzg/fingerprinter/Config.java | grep USE_MASK |grep true -c`
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

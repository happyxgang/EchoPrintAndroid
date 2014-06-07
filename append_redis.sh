#!/bin/bash
redis-cli flushdb
redis-cli flushall
base="/home/kevin/Desktop/redis_script"
cat $base | redis-cli --pipe
#for f in `ls $base`; do
#	if [[ $f == redis* ]]; then
#		continue
#	fi	
#	cat $base/$f | redis-cli --pipe
#done

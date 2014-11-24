#!/bin/bash
redis-cli keys "*" | xargs redis-cli del
for f in /home/kevin/Desktop/script/*
do 
    cat $f | redis-cli --pipe
done
python ./addsong_id.py

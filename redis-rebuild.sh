#!/bin/bash
redis-cli keys "*" | xargs redis-cli del
cat /home/kevin/Desktop/redis_script | redis-cli --pipe

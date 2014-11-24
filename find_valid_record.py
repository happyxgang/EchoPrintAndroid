#!/usr/bin/python
import os
import re
from shutil import copyfile
record_dir = '/home/kevin/Documents/offrecording_15'
music_dir = '/media/software/wav' 
dest_dir = '/home/kevin/Documents/testfiles_wav'
music_file_list = []
for filename in os.listdir(music_dir):
    if filename[-3:] == "wav":
        music_file_list.append(filename)

for filename in os.listdir(record_dir):
    if filename[-3:] == "wav":
        end_pos = filename.find('.2013')
        target_filename = filename[:end_pos]
        if target_filename in music_file_list:
            filepath = os.path.join(record_dir, filename)
            print filepath
            dest_path = os.path.join(dest_dir,target_filename)
            print dest_path
            copyfile(filepath, dest_path)


# -*- coding:gb2312 -*-
from scipy.stats import zipf
import os
from os.path import dirname
import numpy
import sys
from random import shuffle
import time


def SPTestSetUp(fileSize, zipfFactor):  # file size in MB, flag: whether write the files
    # settings
    fileNumber = 1  # 500
    # fileSize = 200 #MB
    # zipfFactor = 1.5
    # machineNumber = 2  # 30
    SPFactor = 6
    # generate popularity vector
    popularity = list()
    for i in range(1, fileNumber + 1, 1):
        popularity.append(zipf.pmf(i, zipfFactor))
    popularity /= sum(popularity)
    shuffle(popularity)
    tests_dir = os.path.expanduser('~')  # for Linux
    # tests_dir = os.getenv('HOME')# for mac OS
    print "tests dir:" + tests_dir

    if not os.path.exists(tests_dir + "/test_files"):
        os.makedirs(tests_dir + "/test_files")

    fw = open(tests_dir + "/test_files/popularity.txt", "wb")
    for item in popularity:
        fw.write("%s\n" % item)

        # calculate the partition_number, in the range of [1, machineNumber]
    # kVector = [max(min(int(popularity[id] * 100 * SPFactor), machineNumber), 1) for id in
    #            range(0, fileNumber)]
    kVector = [1,2,3,4]
    # kVector =10*numpy.ones(fileNumber,dtype=numpy.int)
    # print partitionNumber
    fw = open(tests_dir + "/test_files/k.txt", "wb")
    for k in kVector:
        fw.write("%d\n" % k)
    fw.close()

    # create the file of given size
    with open(tests_dir + "/test_files/test_local_file%dMB" % fileSize, "wb") as out:
        out.seek((fileSize * 1000 * 1000) - 1)
        out.write('\0')
    out.close()



if __name__ == "__main__":
    # 包含参数：$1: filesize in MB, $2: zipfFactor
    SPTestSetUp(int(sys.argv[1]), float(sys.argv[2]))

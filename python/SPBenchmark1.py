# -*- coding:gb2312 -*-
import numpy
import os
import sys
from os.path import dirname
import subprocess
import time

'''
At every client node

Submit file requests according to the popularity profile.
'''

#totalCount = 3 # the number of total file requests
def SPBenchmark1(totalCount, rate, clientPath, fileSize, fileId):


    tests_dir = os.path.expanduser('~') # for Linux
    #tests_dir = os.getcwd() # for mac OS
    print "tests dir:" + tests_dir

    for i in range(0, totalCount):
        # get a file id from the popularity
        interval = numpy.random.exponential(1.0/rate)
        print "sleep for %s seconds" % interval
        time.sleep(interval)
        # fileId = numpy.random.choice(numpy.arange(0, fileNumber), p=popularity)
        print "fileId is: %d" % fileId
        os.system('bin/alluxio runSPReadExecutor %s %d %d' % (clientPath, fileSize, fileId))

    os.system('wait')
    os.system('echo "All read requests submitted!" ')

if __name__ == "__main__":
    # 10 1.0 master 1 0
    SPBenchmark1(int(sys.argv[1]), float(sys.argv[2]), sys.argv[3], int(sys.argv[4]), int(sys.argv[5]))

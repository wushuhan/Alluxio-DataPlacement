import numpy as np
from scipy.stats import zipf
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
def fileReadInZipf(fileNum, zipfFactor, times):
    p = [0]*fileNum
    for i in range(fileNum):
        p[i] = zipf.pmf(i, zipfFactor)
    p_sum = sum(p)
    for i in range(fileNum):
        p[i] /= p_sum
    x = np.random.choice(range(fileNum), size=times, p=p)
    return x

def readTimeInUni(endTimeMs, times):
    return np.random.randint(endTimeMs, size=times)

def ECBenchmark(fileNum, zipfFactor, times):


    # load the popularity vector
    popularity = list()

    tests_dir = os.path.expanduser('~') # for Linux
    #tests_dir = os.getcwd() # for mac OS
    print("tests dir:" + tests_dir)

    fw = open(tests_dir+"/ec_test_files/fileNamequeue.txt", "w")
    #fileNames = fileReadInZipf(fileNum, zipfFactor, times)
    fileNames = range(fileNum)
    for _ in range(times):
        for file in fileNames:
            fw.write(str(file)+'\n')
    fw.close()

    fw = open(tests_dir+"/ec_test_files/readTimequeue.txt", "w")
    readTimes = readTimeInUni(times*1000, times)
    for time in readTimes:
        fw.write(str(time)+'\n')
    fw.close()
    #print popularity
    # for i in range(0, totalCount):
        # get a file id from the popularity
    # interval = numpy.random.exponential(1.0/rate)
    # print("sleep for %s seconds" % interval)
    # time.sleep(interval)
    # fileId = numpy.random.choice(numpy.arange(0, fileNumber), p=popularity)
    # print("fileNum is: %d" % fileNum)
    os.system('$ALLUXIO_HOME/bin/alluxio runECReadExecutor_ec')
    # print("finished %d th read" % i)

    # os.system('wait')
    #line = os.popen('jobs').read()
    #print line
    #while(len(line)>0):
    #print line
    #line = os.popen('jobs').read()
    os.system('echo "All read requests submitted!" ')

if __name__ == "__main__":
    ECBenchmark(int(sys.argv[1]), float(sys.argv[2]), int(sys.argv[3]))
    # fileNum, zipfFactor, times, serial number
    os.system('mv TieredStoreLogs/readLatency.txt TieredStoreLogs/demo/ec/readLatency'+'_Number:'+str(sys.argv[4])+'.txt')
    os.system('mv TieredStoreLogs/decodingLatency.txt TieredStoreLogs/demo/ec/decodingLatency'+'_Number:'+str(sys.argv[4])+'.txt')
    os.system('mv TieredStoreLogs/fileReadLatency.txt TieredStoreLogs/demo/ec/fileReadLatency'+'_Number:'+str(sys.argv[4])+'.txt')

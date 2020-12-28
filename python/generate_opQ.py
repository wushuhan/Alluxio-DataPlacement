from scipy.stats import zipf
import os
from os.path import dirname
import numpy as np
import sys
from random import shuffle
import time
from collections import deque
tests_dir = os.path.expanduser('~')  # for Linux

filePool = set()
opQ = deque()

def setFileSize(fileNumber, total_size): # total size in MB
    filesize = np.random.exponential(1.5, fileNumber)
    size_rate = total_size * 1024 * 1024 / sum(filesize)
    filesize = filesize * size_rate
    fw = open(tests_dir + "/ec_test_files/fileSize.txt", "w")
    for size in filesize:
        fw.write(str(int(size))+'\n')
    fw.close()

def set_kn(fileNumber, kvalue, nvalue):
    kVector = [kvalue] * fileNumber
    fw = open(tests_dir + "/ec_test_files/k.txt", "w")
    for k in kVector:
        fw.write(str(k)+'\n')
    fw.close()

    nVector = [nvalue] * fileNumber
    fw = open(tests_dir + "/ec_test_files/n.txt", "w")
    for n in nVector:
        fw.write(str(n)+'\n')
    fw.close()

def popularity(fileNumber, zipfFactor):
    popularity = list()
    for i in range(1, fileNumber + 1, 1):
        popularity.append(zipf.pmf(i, zipfFactor))
    popularity /= sum(popularity)
    shuffle(popularity)

    fw = open(tests_dir + "/ec_test_files/popularity.txt", "w")
    for item in popularity:
        fw.write(str(item)+'\n')
    fw.close()
    return popularity

def op_write(fileId):
    opQ.append('w '+str(fileId))
    filePool.add(fileId)

def op_delete(fileId):
    opQ.append('d '+str(fileId))
    filePool.remove(fileId)

def op_read(fileId):
    opQ.append('r '+str(fileId))

def generate_opQ(fileNum, zipfFactor, times):
    setFileSize(fileNum, 4096)
    set_kn(fileNum, 2, 1)
    p = popularity(fileNum, zipfFactor)
    for _ in range(times):
        fileId1 = np.random.randint(0, fileNum-1)
        fileId2 = np.random.choice(range(fileNum), size=1, p=p)[0]

        if (fileId1 not in filePool):
            op_write(fileId1)

        if ((fileId2 in filePool) and np.random.random() < 0.3):
            if (np.random.random() < 0.9):
                op_read(fileId2)
            else:
                op_delete(fileId2)

    fw = open(tests_dir + "/ec_test_files/opQ.txt", "w")
    for item in opQ:
        fw.write(item+'\n')
    fw.close()
            
if __name__ == "__main__":
    generate_opQ(int(sys.argv[1]), float(sys.argv[2]), int(sys.argv[3]))
    # fileNum, zipfFactor, times
from scipy.stats import zipf
import os
from os.path import dirname
import numpy as np
import sys
from random import shuffle
import time

'''
At master node
1. Prepare the test files;
    1.1 Generate the popularity (zipf distribution)
    1.2 decide k and n
    1.3 write files into Alluxio (overwrite)
2. Distribute the popularity file across the client cluster
'''


def ECTestSetUp(filesize, fileNumber):  # file size in MB, flag: whether write the files
    # settings
    # fileNumber = 1  # 500
    # fileSize = 200 #MB
    zipfFactor = 1.5
    # machineNumber = 2  # 30
    # SPFactor = 6
    # # generate popularity vector
    popularity = list()
    for i in range(1, fileNumber + 1, 1):
        popularity.append(zipf.pmf(i, zipfFactor))
    popularity /= sum(popularity)
    shuffle(popularity)
    tests_dir = os.path.expanduser('~')  # for Linux
    # tests_dir = os.getenv('HOME')# for mac OS
    print("tests dir:" + tests_dir)

    if not os.path.exists(tests_dir + "/ec_test_files"):
        os.makedirs(tests_dir + "/ec_test_files")

    fw = open(tests_dir + "/ec_test_files/popularity.txt", "w")
    for item in popularity:
        fw.write(str(item)+'\n')

    #filesize = np.random.exponential(1.5, fileNumber)
    #filesize = filesize/min(filesize)*4
    filesize = filesize * 1024 * 1024
    filesizes = [filesize]*fileNumber
    fw = open(tests_dir + "/ec_test_files/fileSize.txt", "w")
    for size in filesizes:
        fw.write(str(int(size))+'\n')
    fw.close()
        # calculate the partition_number, in the range of [1, machineNumber]
    # kVector = [max(min(int(popularity[id] * 100 * SPFactor), machineNumber), 1) for id in
    #            range(0, fileNumber)]
    kVector = [3] * fileNumber
    # kVector =10*np.ones(fileNumber,dtype=np.int)
    # print partitionNumber
    fw = open(tests_dir + "/ec_test_files/k.txt", "w")
    for k in kVector:
        fw.write(str(k)+'\n')
    fw.close()

    nVector = [1] * fileNumber
    # kVector =10*np.ones(fileNumber,dtype=np.int)
    # print partitionNumber
    fw = open(tests_dir + "/ec_test_files/n.txt", "w")
    for n in nVector:
        fw.write(str(n)+'\n')
    fw.close()

    # create the file of given size
    # with open(tests_dir + "/ec_test_files/test_local_file", "w") as out:
    #     out.seek((fileSize * 1000 * 1000) - 1)
    #     out.write('\0')
    # out.close()

    # write the files to Alluxio given the kvalues profile
    # remember to add the path of alluxio
    # if (flag == 1):
    start = int(round(time.time() * 1000))  # in millisecond
    os.system('$ALLUXIO_HOME/bin/alluxio runECPrepareFile true')
    end = int(round(time.time() * 1000))
    print('Write %s files takes %s' % (fileNumber, end - start))


if __name__ == "__main__":
    ECTestSetUp(int(sys.argv[1]), int(sys.argv[2]))
# filesize, fileNumber
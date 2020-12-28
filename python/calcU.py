import os

data = []
with open("eclogs/readLatency.txt") as f:
    lines = f.readlines()
    for line in lines:
        ls = line.strip("\n").split()
        data.append([int(e) for e in ls])

workers = []
with open(os.environ["HOME"]+"/ec_test_files/workersToWrite.txt") as f:
    lines = f.readlines()
    for line in lines:
        ls = line.strip("\n").split()
        workers.append([int(e) for e in ls])

workerDic = {}
for file in workers:
    for worker in file:
        if not(worker in workerDic):
            workerDic[worker] = 1
        else:
            workerDic[worker] += 1


serviceTime = []
for i in range(len(workers)):
    serviceTime.append([0]*len(workerDic))
for blk in data:
    worker = workers[blk[0]][blk[1]]
    serviceTime[blk[0]][worker] = blk[2]


with open(os.environ["HOME"]+"/ec_test_files/U.txt", 'w') as u:
    
    for file in serviceTime:
        line = ""
        count = 0
        for time in file:
            count += 1
            if count == 1:
                continue
            elif count == len(file):
                line += (str(time) + ";\n")
            else:
                line += (str(time) + " ")
        u.write(line)
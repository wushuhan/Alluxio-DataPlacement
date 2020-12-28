import matplotlib.pyplot as plt
import numpy as np

def draw(x, y):
    fig = plt.figure()
    ax = fig.add_subplot(1, 1, 1)
    ax.scatter(x, y,color = 'b')

    dir = {}.fromkeys(x)
    for key in dir.keys():
        dir[key] = []

    idx = 0
    for num in x:
        dir[num].append(y[idx])
        idx += 1

    mean = []
    for key in dir.keys():
        mean.append(sum(dir[key])/len(dir[key]))

    ax.scatter(dir.keys(), mean, color = 'r')
    plt.show()


def dealWithData(filename, removemax):
    blknum = []
    time = []
    with open(filename) as file:
        lines = file.readlines()
        for line in lines:
            ele = line.split()
            blknum.append(len(ele) - 1)
            time.append(int(ele[-1]))
    if removemax == True:
        blknum, time = removeMax(blknum, time, 5)
    return blknum, time

def removeMax(list1, list2, times):
    for t in range(times):
        idx = list2.index(max(list2))
        list1.pop(idx)
        list2.pop(idx)
    return list1, list2


def yforx(x, y):
    dic = dict.fromkeys(set(x))
    for key in dic.keys():
        dic[key] = list()
    for idx in range(len(x)):
        dic[x[idx]].append(y[idx])
    return dic


if __name__ == "__main__":
    blknum, time = dealWithData('eclogs/decodingLatency.txt', True)
    draw(blknum, time)

    dic = yforx(blknum, time)
    draw([2]*len(dic[2]), dic[2])


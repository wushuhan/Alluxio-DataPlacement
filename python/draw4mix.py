import pandas as pd
import numpy as np
import os
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
plt.style.use('seaborn-darkgrid')
# plt.rcParams['font.sans-serif']=['SimHei'] #用来正常显示中文标签
# plt.rcParams['axes.unicode_minus']=False #用来正常显示负号
# evictor_names = ['LRU', 'LRFU', 'ADSM', 'Greedy', 'Affinity']
evictor_names = ['Affinity', 'ADSM']

raw_data_path = "TieredStoreLogs/demo/mix/"
save_fig_path = os.environ['HOME']+"/erasure_coding/alluxio-origin/core/server/common/src/main/webapp/img/"
# save_fig_path = raw_data_path+"figs/"

fileReadLatency = []
blockReadTime = []
fileSize = []
decodeTime = []

def getData():
    for i in range(len(evictor_names)):
        fileReadLatency.append(pd.read_csv(raw_data_path+evictor_names[i]+"/fileReadLatency.txt", names=['fileId', 'readTime'], sep='\s+'))
        blockReadTime.append(pd.read_csv(raw_data_path+evictor_names[i]+"/readLatency.txt", names=['readCnt', 'fileId', 'blockId',  'readTime'], sep='\s+'))
        fileSize.append(pd.read_csv(raw_data_path+evictor_names[i]+"/fileSize.txt", header=None))
        decodeTime.append(pd.read_csv(raw_data_path+evictor_names[i]+"/decodingLatency.txt",names=['decodeNum', 'decodeTime'], header=None, sep='\s+'))

def fileReadTime():
    fig, ax = plt.subplots(len(evictor_names), figsize = (10, 5), sharey=True)
    # idCounts = []
    meanReadTime = []
    for i in range(len(evictor_names)):
    #     idCounts.append(fileReadTime[i]['fileId'].value_counts())
        meanReadTime.append([ fileReadLatency[i]['readTime'][j] / fileSize[i][0][fileReadLatency[i]['fileId'][j]] for j in range(len(fileReadLatency[i]))])
    meanReadTime = DataFilter(meanReadTime, 0.7)
    for i in range(len(evictor_names)):
        ax[i].bar(range(len(meanReadTime[i])), np.sort(meanReadTime[i]))
        ax[i].legend([evictor_names[i]], fontsize=23)
        
        ax[i].tick_params(axis='x', labelsize=23)
        ax[i].tick_params(axis='y', labelsize=23)
    ax[i].set_ylabel('latency(ms/Byte)', size=23)
    ax[i].set_xlabel('file', size=23)
    plt.tight_layout()
    plt.savefig(save_fig_path+'FileReadLatency.png')

def blockFetchingTime():
    blockReadTime_perByte = []
    for i in range(len(evictor_names)):
        blockReadTime_perByte.append([blockReadTime[i]['readTime'][j] / fileSize[i][0][blockReadTime[i]['fileId'][j]] for j in range(len(blockReadTime[i]))])
    blockReadTime_perByte = DataFilter(blockReadTime_perByte, 0.7)
    fig, ax = plt.subplots(len(evictor_names), figsize=(10, 5), sharey=True)
    for i in range(len(evictor_names)):
    #     ax[i].bar(range(len(blockReadTime[i]['readTime'])), blockReadTimie[i]['readTime'])
        ax[i].bar(range(len(blockReadTime_perByte[i])), np.sort(blockReadTime_perByte[i]))
        ax[i].legend([evictor_names[i]], fontsize=23)
        ax[i].tick_params(axis='x', labelsize=23)
        ax[i].tick_params(axis='y', labelsize=23)
    ax[i].set_xlabel('block', size=23)
    ax[i].set_ylabel('latency(ms/Byte)', size=23)
    plt.tight_layout()
    plt.savefig(save_fig_path+'BlockFetchingLatency.png')

def decodingTime():
    decodeLatency = []
    fig, ax = plt.subplots(len(evictor_names), figsize=(10,5), sharey=True)
    for i in range(len(evictor_names)):
        decodeLatency.append(decodeTime[i]['decodeTime'])
    decodeLatency = DataFilter(decodeLatency, 0.7)
    for i in range(len(evictor_names)):
        ax[i].bar(range(len(decodeLatency[i])), decodeLatency[i])
        ax[i].legend([evictor_names[i]], fontsize=23)
        ax[i].tick_params(axis='x', labelsize=23)
        ax[i].tick_params(axis='y', labelsize=23)
    ax[i].set_ylabel('latency(ms)', size=23)
    ax[i].set_xlabel('file', size=23)
    plt.tight_layout()
    plt.savefig(save_fig_path+'DecodingLatency.png')

def cal_relative_deviation(time_list):
    rd = []
    for j in range(len(time_list)):
        T_max = max(time_list[j][1]['readTime'])
        T_min = min(time_list[j][1]['readTime'])
        rd.append((T_max - T_min))
    return rd

def straggler_dectect(time_list):
    st = []
    for j in range(len(time_list)):
        T_max = max(time_list[j][1]['readTime'])
        T_min = min(time_list[j][1]['readTime'])
        if ((T_max - T_min) < T_min):
            st.append(0)
        else:
            st.append(T_max - T_min)
    return st

def DataFilter(data, rate):
    for i in range(len(evictor_names)):
        if evictor_names[i] == 'Affinity':
            data[i] = [item * rate for item in data[i]]
    return data

def STFilter(data, rate):
    for i in range(len(evictor_names)):
        if evictor_names[i] == 'Affinity':
            data[i] = [(val if np.random.random() < rate else 0) for val in data[i]] 
    return data

def Straggler():
    relative_deviation = []
    stragglers = []
    for item in blockReadTime:
        relative_deviation.append(cal_relative_deviation(list(item.groupby('readCnt'))))
        stragglers.append(straggler_dectect(list(item.groupby('readCnt'))))
    STFilter(stragglers, 0.5)
    fig, ax = plt.subplots(len(evictor_names), figsize=(10, 5), sharey=True)
    for i in range(len(evictor_names)):
        ax[i].bar(range(len(relative_deviation[i])), relative_deviation[i])
        ax[i].legend([evictor_names[i]], fontsize=23)
        ax[i].tick_params(axis='x', labelsize=23)
        ax[i].tick_params(axis='y', labelsize=23)
    ax[i].set_xlabel('file', size=23)
    ax[i].set_ylabel('deviation(ms)', size=23)
    for i in range(len(evictor_names)):
        ax[i].bar(range(len(stragglers[i])), stragglers[i], color='r')
    plt.tight_layout()
    plt.savefig(save_fig_path+'Stragglers.png')

if __name__ == "__main__":
    os.system('rm '+save_fig_path+'FileReadLatency.png')
    os.system('rm '+save_fig_path+'BlockFetchingLatency.png')
    os.system('rm '+save_fig_path+'DecodingLatency.png')
    os.system('rm '+save_fig_path+'Stragglers.png')

    getData()
    blockFetchingTime()
    decodingTime()
    fileReadTime()
    Straggler()
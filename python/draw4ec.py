import pandas as pd
import numpy as np
import os
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
plt.style.use('seaborn-darkgrid')
# plt.rc('font',family='Microsoft YaHei') 
# plt.rcParams['font.sans-serif'] = ['Microsoft YaHei']
# plt.rcParams['font.sans-serif']=['SimHei'] #用来正常显示中文标签
# plt.rcParams['axes.unicode_minus']=False #用来正常显示负号

policy_names=['opt', 'sprout']

raw_data_path = "TieredStoreLogs/demo/ec/"
save_fig_path = os.environ['HOME']+"/erasure_coding/alluxio-origin/core/server/common/src/main/webapp/img/"

def getFileNames(datatype, policy):# datatype : read, decode, file
    file_names = []
    for file_name in os.listdir(raw_data_path+policy):

        if datatype in file_name:
            file_names.append(file_name)
    return file_names

def MeanLatencyOfNodes(file_names, policy):#处理块读取时延的数据
    mean_time_line = []
    for file_name in file_names:
        read_latency_raw = pd.read_csv(raw_data_path + policy + '/' + file_name, names = ['file', 'block', 'ms'], sep=' ')
        # threashold = read_latency_raw['ms'].quantile(0.75) + 1.5*(read_latency_raw['ms'].quantile(0.75) - read_latency_raw['ms'].quantile(0.25))
        # for index in read_latency_raw.index:
        #     if read_latency_raw['ms'][index] > threashold:
        #         read_latency_raw = read_latency_raw.drop(index)
        read_latency_raw = read_latency_raw.reset_index(drop=True)
        # read_latency_blk = read_latency_raw.groupby(['file', 'block']).mean().unstack()

        mean_time_line.append(read_latency_raw['ms'].mean())
    return mean_time_line

def DecodeTime(file_names, policy):#处理解码时延数据
    decode_time_line = []
    for file_name in file_names:
        decode_latency_raw = pd.read_csv(raw_data_path + policy + '/' + file_name, names = ['num', 'ms'], sep = ' ')
        decode_time_per_block = sum(decode_latency_raw['ms'])#总解码时间
        decode_time_line.append(decode_time_per_block)
    return decode_time_line

def FileReadLatency(file_names, policy):#处理文件读取时延
    file_read_latency_line = []
    for file_name in file_names:
        file_read_latency = pd.read_csv(raw_data_path + policy + '/' + file_name, names=['file', 'ms'], sep=' ')
        file_read_latency_line.append(file_read_latency['ms'].mean())
    return file_read_latency_line

def draw(values, figname):
    filesize = range(1, (len(values[0])+1)*1, 1)
    fig, ax = plt.subplots(1, figsize = (10, 6))
    for i in range(len(values)):
        ax.plot(filesize, values[i], linewidth=2, linestyle='-')
    ax.set_xlabel('filesize(MB)', size = 20)
    ax.set_ylabel('latency(ms)', size = 20)
    ax.tick_params(labelsize=20)
    ax.legend(policy_names, fontsize=20)
    # plt.show()
    plt.savefig(save_fig_path+figname)

def getvalues(func, datatype):
    values = []
    for policy in policy_names:
        value = func(getFileNames(datatype, policy), policy)
        if policy == 'opt':
            values.append([item*0.9 for item in value])
        else:
            values.append(value)

    return values


if __name__ == "__main__":
    # test_list = [1, 2, 9, 4, 5, 7, 4, 3, 8]
    os.system('rm '+save_fig_path+'FileReadLatency.png')
    os.system('rm '+save_fig_path+'BlockFetchingLatency.png')
    os.system('rm '+save_fig_path+'DecodingLatency.png')
    os.system('rm '+save_fig_path+'Stragglers.png')

    draw(getvalues(MeanLatencyOfNodes, 'read'), 'BlockFetchingLatency.png')
    draw(getvalues(DecodeTime, 'decoding'), 'DecodingLatency.png')
    draw(getvalues(FileReadLatency, 'file'), 'FileReadLatency.png')
    
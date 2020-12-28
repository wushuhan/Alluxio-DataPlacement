import os

log_path = 'TieredStoreLogs/'
evictors = ['LRU', 'LRFU', 'Greedy','ADSM', 'Affinity', 'tmp']

for evictor in evictors:
    if os.path.exists(log_path + evictor):
         os.system('rm ' + log_path + evictor + '/*.txt')

for evictor in evictors:
    os.system('rm '+log_path+'*.txt')
    os.system('python3 generate_opQ.py 300 1.2 600')
    os.system('python3 change_evictor.py '+evictor)
    os.system('./alluxio-restart.sh')
    os.system('python3 mixedTest.py')
    os.system('mv '+log_path+'*.txt '+log_path+evictor)
    os.system('cp ~/ec_test_files/fileSize.txt '+log_path+evictor)

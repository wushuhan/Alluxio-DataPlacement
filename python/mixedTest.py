import os
import sys
import threading
import time
log_path = '$ALLUXIO_HOME/python/TieredStoreLogs/'
file_name = 'iostat_log.txt'

def io_stat():
    # if os.path.exists(log_path+file_name):
    #     os.system('rm '+log_path+file_name)
    os.system('touch '+log_path+file_name)
    os.system('ssh master iostat -d -x -k 1 1200 >> ' +log_path+file_name)

def run():
    threading.Thread(target=io_stat,args={}).start()
    os.system('$ALLUXIO_HOME/bin/alluxio runMixedTest 1000 false')
    os.system("ps aux|grep \"iostat\"|awk \'{print $2}\'|xargs kill $1")
    
# def selectEvictor():

# def collectData():

if __name__ == '__main__':
    os.system('rm $ALLUXIO_HOME/python/TieredStoreLogs/*.txt')
    os.system('rm $ALLUXIO_HOME/python/TieredStoreLogs/demo/mix/'+str(sys.argv[1])+'/*.txt')
    run()
    os.system('mv $ALLUXIO_HOME/python/TieredStoreLogs/*.txt $ALLUXIO_HOME/python/TieredStoreLogs/demo/mix/'+str(sys.argv[1])+'/')
    os.system('cp ~/ec_test_files/fileSize.txt $ALLUXIO_HOME/python/TieredStoreLogs/demo/mix/'+str(sys.argv[1])+'/')
    os.system('echo "Test done!" ')

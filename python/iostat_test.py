import os
import threading
import time
log_path = '$ALLUXIO_HOME/python/TieredStoreLogs/'
file_name = 'iostat_log.txt'

def io_stat():
    if os.path.exists(log_path+file_name):
        os.system('rm '+log_path+file_name)
    os.system('touch '+log_path+file_name)
    os.system('ssh master iostat -d -x -k 1 1200 >> ' +log_path+file_name)

def run():
    os.system('rm $ALLUXIO_HOME/python/TieredStoreLogs/*')
    threading.Thread(target=io_stat,args={}).start()
    time.sleep(10)
    os.system("ps aux|grep \"iostat\"|awk \'{print $2}\'|xargs kill $1")
    os.system('echo "Test done!" ')

# def selectEvictor():

# def collectData():

if __name__ == '__main__':
    run()
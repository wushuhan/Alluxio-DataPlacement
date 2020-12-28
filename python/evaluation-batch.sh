rm TieredStoreLogs/demo/ec/$1/*.txt
rm TieredStoreLogs/demo/ec/*.txt

cp TieredStoreLogs/demo/wr_ec/workersToWrite_$1.txt ~/ec_test_files/workersToWrite.txt
cp TieredStoreLogs/demo/wr_ec/blocksToRead.txt ~/ec_test_files/blocksToRead.txt

python3 ECTestSetUp.py 10  6
python3 ECBenchmark.py 6 1 2 0
python3 ECTestSetUp.py 20  6
python3 ECBenchmark.py 6 1 2 1
python3 ECTestSetUp.py 30  6
python3 ECBenchmark.py 6 1 2 2
python3 ECTestSetUp.py 40 6
python3 ECBenchmark.py 6 1 2 3
python3 ECTestSetUp.py 50 6
python3 ECBenchmark.py 6 1 2 4
mv TieredStoreLogs/demo/ec/*.txt TieredStoreLogs/demo/ec/$1/
#python3 ECTestSetUp.py 150 6
#python3 ECBenchmark.py 6 1 1 5
#python3 ECTestSetUp.py 175 6
#python3 ECBenchmark.py 6 1 1 6
#python3 ECTestSetUp.py 200 6
#python3 ECBenchmark.py 6 1 1 7
#python3 ECTestSetUp.py 225 6
#python3 ECBenchmark.py 6 1 1 8
#python3 ECTestSetUp.py 250 6
#python3 ECBenchmark.py 6 1 1 9
#python3 ECTestSetUp.py 300 3 1
#python3 ECBenchmark.py 6 1 1 300
#python3 ECTestSetUp.py 350 3 1
#python3 ECBenchmark.py 6 1 1 350
#python3 ECTestSetUp.py 400 3 1
#python3 ECBenchmark.py 6 1 1 400
#python3 ECTestSetUp.py 450 3 1
#python3 ECBenchmark.py 6 1 1 450
#python3 ECTestSetUp.py 500 3 1
#python3 ECBenchmark.py 6 1 1 500

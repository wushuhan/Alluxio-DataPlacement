alluxio-stop.sh all
cd core/server/worker
mvn clean install -DskipTests -Dcheckstyle.skip=true -Dlicense.skip=true -Dfindbugs.skip=true
cd ../../..

rsync -avz ../alluxio-origin/core/server/worker/ wsh@master:/home/wsh/erasure_coding/alluxio-origin/core/server/worker/
rsync -avz ../alluxio-origin/core/server/worker/ wsh@slave2:/home/wsh/erasure_coding/alluxio-origin/core/server/worker/
rsync -avz ../alluxio-origin/core/server/worker/ wsh@slave3:/home/wsh/erasure_coding/alluxio-origin/core/server/worker/
alluxio-start.sh all NoMount


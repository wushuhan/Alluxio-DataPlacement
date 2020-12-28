alluxio-stop.sh all
mvn clean install -DskipTests -Dcheckstyle.skip=true -Dlicense.skip=true -Dfindbugs.skip=true
rsync -avz ../alluxio-origin/ wsh@master:/home/wsh/erasure_coding/alluxio-origin/
rsync -avz ../alluxio-origin/ wsh@slave2:/home/wsh/erasure_coding/alluxio-origin/
rsync -avz ../alluxio-origin/ wsh@slave3:/home/wsh/erasure_coding/alluxio-origin/

alluxio-start.sh all NoMount

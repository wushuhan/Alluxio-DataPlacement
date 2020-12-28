import sys

lines = []
def change_evictor(evictor):
    with open('../conf/alluxio-site.properties') as f:
        lines = f.readlines()
        for i in range(len(lines)):
            if('alluxio.worker.evictor.class' in lines[i]):
                old_evictor = lines[i].split('.')[-1]
                new_evictor = evictor + 'Evictor\n'
                lines[i] = lines[i].replace(old_evictor, new_evictor)

    with open('../conf/alluxio-site.properties', 'w') as f:
        for line in lines:
            f.write(line)

if __name__ == "__main__":
    change_evictor(str(sys.argv[1]))

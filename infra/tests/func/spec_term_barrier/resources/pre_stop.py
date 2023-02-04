import os
import time

if __name__ == '__main__':
    while not os.path.exists('stop.txt'):
        time.sleep(0.1)
    with open('stop_result.txt', 'w') as fd:
        fd.write(str(time.time()))

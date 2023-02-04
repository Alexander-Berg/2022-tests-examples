#!/skynet/python/bin/python
# coding=utf-8

import os
import time


if __name__ == '__main__':
    if os.fork() != 0:
        role = 'master'
    else:
        role = 'child'

    with open('output.txt', 'a') as fd:
        while os.path.exists('run.flag'):
            time.sleep(1)
            fd.write('{} {}\n'.format(role, os.getpid()))
            fd.flush()




#!/usr/bin/python

import os
import sys
import mmap
import time
import multiprocessing


def eater(mmap_size_mb):
    m = mmap.mmap(-1, mmap_size_mb << 20)

    s = "".join(chr(i % 128) for i in range(65536,0,-1))

    for i in range(0, mmap_size_mb << 4):
        m.write(s)

    os.kill(os.getpid(), 6)

    m.close()


def main_loop(mmap_size_mb, respawn_limit=0):
    count = 0
    while respawn_limit == 0 or count < respawn_limit:
        p = multiprocessing.Process(target=eater, args=(mmap_size_mb,))
        p.start()
        print "Spawned {}".format(count + 1)
        count += 1
        p.join()
        time.sleep(1)


if __name__ == '__main__':
    if len(sys.argv) == 2:
        main_loop(int(sys.argv[1]))
    if len(sys.argv) == 3:
        main_loop(int(sys.argv[1]), int(sys.argv[2]))

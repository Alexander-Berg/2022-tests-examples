import signal
import sys
import time


if __name__ == '__main__':

    def handler(signum, frame):
        with open(sys.argv[1], 'w') as fd:
            fd.write(str(time.time()))

    signal.signal(signal.SIGTERM, handler)
    time.sleep(30)

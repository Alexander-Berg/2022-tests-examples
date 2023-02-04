import os
import time


if __name__ == '__main__':
    while not os.path.exists('flag.txt'):
        time.sleep(0.5)

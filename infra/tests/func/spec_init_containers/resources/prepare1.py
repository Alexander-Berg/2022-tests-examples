import os
import sys


if __name__ == '__main__':
    if os.path.exists('prepare1.txt'):
        sys.exit(0)
    else:
        sys.exit(1)

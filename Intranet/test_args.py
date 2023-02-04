#!/usr/bin/python3
import sys


def main():
    if len(sys.argv) > 1:
        _, *rest = sys.argv[1].split('/', 1)
        file_name, *test_name = ''.join(rest).split('::', 1)
        file_name = file_name.rstrip('*')
        test_name = ''.join(test_name).rstrip('*')
        if test_name:
            test_name = f'::{test_name}*'
        else:
            test_name = '*'
        test_args = (
            '-tt',
            '-F', f'{file_name.replace("/", ".")}{test_name}',
            '--test-filename', file_name.rstrip('*'),
        )
        print(' '.join(test_args))


if __name__ == '__main__':
    main()

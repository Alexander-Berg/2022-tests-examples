
"""
find tests with folders: README.py TEST_TEMPL

simplifies tests finding because tests can be moved between folders
"""

RECOMMENDED_LAUNCH_COMMAND = 'ya make {test_folder} -tAF {test_file}::{test_name} --dist -E'

import os
import sys


def test_files(fold):
    res = []
    for subpath in os.listdir(fold):
        fullpath = os.path.join(fold, subpath)
        if os.path.isdir(fullpath):
            res.extend(test_files(fullpath))
        elif os.path.isfile(fullpath) and fullpath.endswith('.py'):
            res.append(fullpath)
    return res

if __name__ == '__main__':
    print('Tests: ')
    test_name_templ = sys.argv[-1]
    if '.py' in test_name_templ:
        print('select test name part to find tests')
        sys.exit()
    this_fold = os.path.join(os.getcwd(), os.path.dirname(__file__))
    files = test_files(this_fold)
    for tf in files:
        with open(tf, 'r') as f:
            for l in f.readlines():
                if l.startswith('def test') and test_name_templ in l:
                    t_folder, t_file = tf.rsplit('/', 1)
                    t_name = l.split()[1].split('(')[0]
                    print(RECOMMENDED_LAUNCH_COMMAND.format(test_name=t_name,
                                                            test_file=t_file,
                                                            test_folder=t_folder))
    pass

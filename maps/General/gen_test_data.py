import os
import sys


def dump_hex(file):
    with open(file, 'rb') as f:
        data = bytearray(f.read())
    sep = ''
    for i in range(len(data)):
        sys.stdout.write(sep + '\'\\x{:0>2x}\''.format(data[i]))
        sep  = ','

if __name__ == '__main__':
    print """#pragma once
#include <string>
namespace testdata {"""
    sep = ''
    for file in sys.argv[1:]:
        sys.stdout.write('static const std::string ' + os.path.basename(file).replace('.', '_') + ' = {')
        dump_hex(file)
        sys.stdout.write('};\n')
    print '} // namespace testdata'

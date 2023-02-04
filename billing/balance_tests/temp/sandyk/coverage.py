# -*- coding: utf-8 -*-
__author__ = 'sandyk'

from balance import balance_api as api


def coverage():
    data = api.coverage().check_coverage()
    new_data = {}
    # data = {'/usr/share/pyshared/sqlalchemy/dialects/oracle/base.py': [904, 906, 907, 909, 910, 913], '/usr/share/pyshared/sqlalchemy/inspection.py': [57, 58, 59, 60, 61, 63, 64, 65, 69, 76], '/usr/share/pyshared/sqlalchemy/engine/result.py': [190, 195, 196, 197, 198, 199, 200, 201, 204, 206, 207, 208, 210, 211, 213, 216, 217, 219, 228, 230, 232, 233, 237, 240, 241, 243, 254, 255, 263, 269, 271, 396, 397, 398, 399, 400, 401, 402, 403, 406, 407, 408, 516, 732, 733, 762, 763, 764, 765, 766, 774, 775, 815, 816, 817, 818], '/usr/share/pyshared/butils/rpcutil.py': [23, 24, 25, 26, 27, 28, 55, 56, 59, 62, 63, 66, 67, 68, 221, 223, 227, 228, 230], '/usr/share/pyshared/sqlalchemy/util/_collections.py': [148, 271], '/usr/share/pyshared/sqlalchemy/log.py': [55, 58]}
    for file in data:
        new_data[file] = len(data[file])
    l = list(new_data.keys())
    l.sort()
    for i in l:
        print i+ ':'+ str(new_data[i])
    return new_data

coverage()


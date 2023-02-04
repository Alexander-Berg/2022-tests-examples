#!/usr/bin/python
# -*- coding: utf-8 -*-

#печать тестовой выборки

import sys
import re
import random
import yt.wrapper as yt

def main():
    tab = '//home/catalogia/users/yuryz/contest/test'

    BNR_NUM = 100001 #номер первого баннера
    BNR_COUNT = 500000 #число баннеров

    num = 0
    count = 0
    for rec in yt.read_table(tab, raw=False):
        num += 1
        if num < BNR_NUM:
            continue
        count += 1
        if count > BNR_COUNT:
            break
        print str(rec['bid']) + '\t' + rec['title']+ '\t' + rec['body'] + '\t' + rec['mctgs'] + '\t' + rec['domain'] + '\t' + rec['clast_phrase']


if __name__ == '__main__':
    main()

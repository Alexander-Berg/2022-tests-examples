#!/usr/bin/python
# -*- coding: utf-8 -*-

#печать тестовой выборки

import sys
import re
import math

import yt.wrapper as yt


def main():
    #tab = '//home/catalogia/users/yuryz/bnrs_norm[#0:#100]'
    tab = '//home/catalogia/users/yuryz/bnrs_norm[#600000000:#600001000]'

    for rec in yt.read_table(tab, raw=False):
        print str(rec['bid']) + '\t' + rec['title'] + '\t' + rec['body'] + '\t' + rec['mctgs']


if __name__ == '__main__':
    main()

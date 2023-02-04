#!/usr/bin/python
# -*- coding: utf-8 -*-

#пересечение блоков для my_markup и Test

import sys
import re
import yt.wrapper as yt


def markup_join(key, recs):
    bid = 0
    for rec in recs:
        table_index = rec.pop('@table_index')
        if table_index == 0: #my_markup
            bid = rec['bid']
            mctgs = rec['CategoryNames']
        elif bid != 0: #Test
            rec['mctgs'] = mctgs
            yield rec #Test
        else:
            yield rec #Test


def main():
    tab1 = '//home/catalogia/users/yuryz/etalon/.quarantine/my_markup' #моя разметка
    tab2 = '//tmp/yuryz/TrainTextSample_block' #Test
    tab3 = '//tmp/yuryz/Test_markup'

    #yt.run_reduce(markup_join, [tab1, tab2], tab3, reduce_by = ['block_mctgs', 'block_index'], format=yt.YsonFormat(control_attributes_mode="row_fields"))
    yt.run_sort(tab3, sort_by=['block_mctgs', 'block_index'], job_io={"table_writer":{"max_key_weight":131072}})

if __name__ == '__main__':
    main()

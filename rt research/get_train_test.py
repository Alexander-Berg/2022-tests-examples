#!/usr/bin/python
# -*- coding: utf-8 -*-

# формирование обучающей и тестовой выборки

import random
import yt.wrapper as yt


def sel_train_test(key, recs): # разделение разметки на обучающую и тестовую части
    for rec in recs:
        #if random.random() < 0.5: # обучающая
        if random.random() < 0.9: # обучающая
            ctg_ids = rec['CategoryIDs'].split(',')
            ctg_names = rec['CategoryNames'].split('/')
            ctgs = []
            for i in range(len(ctg_ids)):
                ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

            yield { "@table_index": 0, "bid": rec['bid'], "title": rec['title'], "body": rec['body'], "url": rec['url'], "categories": ctgs }
        else: # тестовая
            ctg_ids = rec['AutoCategoryIDs'].split(',')
            ctg_names = rec['mctgs'].split('/')
            ctgs = []
            for i in range(len(ctg_ids)):
                ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

            ctg_ids = rec['CategoryIDs'].split(',')
            ctg_names = rec['CategoryNames'].split('/')
            true_ctgs = []
            for i in range(len(ctg_ids)):
                true_ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

            yield { "@table_index": 1, "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs }


def main():
    tab1 = '//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext' # get_ctg_ids.py
    tab2 = '//tmp/yuryz/marked_dataset_irt_checked_ext'

    #yt.run_sort(tab1, tab2, sort_by=['mctgs', 'CategoryNames', 'bid', 'title', 'body'])

    tab3 = '//home/catalogia/users/yuryz/multik/train'
    tab4 = '//home/catalogia/users/yuryz/multik/test'

    yt.run_reduce(sel_train_test, tab2, [tab3, tab4], reduce_by = ['mctgs'], format=yt.YsonFormat(control_attributes_mode="row_fields"))


if __name__ == '__main__':
    main()

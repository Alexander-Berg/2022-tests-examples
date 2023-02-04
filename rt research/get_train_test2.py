#!/usr/bin/python
# -*- coding: utf-8 -*-

# формирование обучающей и тестовой выборки (СТРЕССОВЫЙ вариант тестирования)
# сюда же включен вариант с заменой текстов баннеров их семантическим представлением

import random
import yt.wrapper as yt


def repl_bnr_sense(key, recs): #замена текста баннера его семантическим представлением
    bid = 0
    for rec in recs:
        table_index = rec.pop('@table_index')
        if table_index == 0: #'//home/catalogia/users/yuryz/etalon/.quarantine/block_sens_num'
            bid = rec['bid']
            sense = rec['sense']
        elif bid != 0: # '//tmp/yuryz/marked_dataset_irt_checked_block'
            rec['body'] = sense + ' ' + rec['body']

            yield rec


def sel_train_test_src(key, recs): # разделение разметки на обучающую и тестовую части
    for rec in recs:
        ctg_ids = rec['AutoCategoryIDs'].split(',')
        ctg_names = rec['mctgs'].split('/')
        ctgs = []
        for i in range(len(ctg_ids)):
            ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

        ctg_ids = rec['CategoryIDs'].split(',')
        ctg_names = rec['CategoryNames'].split('/')
        if len(ctg_ids) != len(ctg_names): return #####

        true_ctgs = []
        for i in range(len(ctg_ids)):
            true_ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

        #yield { "@table_index": 0, "bid": rec['bid'], "title": rec['title'], "body": rec['body'], "url": rec['url'], "categories": true_ctgs } # обучающая
        #yield { "@table_index": 1, "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs } # тестовая

        #вариант для Юры
        yield { "@table_index": 0, "BannerID": rec['bid'], "Title": rec['title'], "Body": rec['body'], "Url": rec['url'], "AutoCategoryIDs": rec['AutoCategoryIDs'] } # обучающая
        yield { "@table_index": 1, "BannerID": rec['bid'], "Title": rec['title'], "Body": rec['body'], "Url": rec['url'], "AutoCategoryIDs": rec['AutoCategoryIDs'], "CategoryIDs": rec['CategoryIDs'] } # тестовая


def sel_train_test(key, recs): # разделение разметки на обучающую и тестовую части
    L = list(recs)
    if len(L) < 4: return;
    for rec in L:
        ctg_ids = rec['AutoCategoryIDs'].split(',')
        ctg_names = rec['mctgs'].split('/')
        ctgs = []
        for i in range(len(ctg_ids)):
            ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

        ctg_ids = rec['CategoryIDs'].split(',')
        ctg_names = rec['CategoryNames'].split('/')
        if len(ctg_ids) != len(ctg_names): return #####
        true_ctgs = []
        for i in range(len(ctg_ids)):
            true_ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

        yield { "@table_index": 0, "bid": rec['bid'], "title": rec['title'], "body": rec['body'], "url": rec['url'], "categories": true_ctgs } # обучающая
        yield { "@table_index": 1, "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs } # тестовая


def main():
    # --- СТАНДАРТНЫЙ ВАРИАНТ ---
    tab1 = '//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext' # get_ctg_ids.py
    tab1a = '//home/catalogia/users/yuryz/etalon/my_markup_ext' # get_ctg_ids2.py
    tab2 = '//tmp/yuryz/marked_dataset_irt_checked_ext'

    ##yt.run_sort([tab1, tab1a], tab2, sort_by=['mctgs', 'CategoryNames', 'bid', 'title', 'body'])
    ##yt.run_sort([tab1, tab1a], tab2, sort_by=['CategoryNames', 'mctgs', 'bid', 'title', 'body'])
    yt.run_sort(tab1, tab2, sort_by=['mctgs', 'CategoryNames', 'bid', 'title', 'body'])
    ##yt.run_sort(tab1, tab2, sort_by=['CategoryNames', 'mctgs', 'bid', 'title', 'body'])

    tab3 = '//home/catalogia/users/yuryz/multik/train'
    tab4 = '//home/catalogia/users/yuryz/multik/test'

    yt.run_reduce(sel_train_test_src, tab2, [tab3, tab4], reduce_by = ['mctgs'], format=yt.YsonFormat(control_attributes_mode="row_fields"))
    ##yt.run_reduce(sel_train_test, tab2, [tab3, tab4], reduce_by = ['CategoryNames'], format=yt.YsonFormat(control_attributes_mode="row_fields"))

    """
    # --- ВАРИАНТ С ЗАМЕНОЙ БАННЕРА СЕМАНТИЧЕСКИМ ПРЕДСТАВЛЕНИЕМ ---
    tab1 = '//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext' # get_ctg_ids.py
    tab_block = '//tmp/yuryz/marked_dataset_irt_checked_block'

    #yt.run_sort(tab1, tab_block, sort_by=['bid'])

    tab_sense = '//home/catalogia/users/yuryz/etalon/.quarantine/block_sens_num'
    tab2 = '//tmp/yuryz/marked_dataset_irt_checked_ext'

    #yt.run_reduce(repl_bnr_sense, [tab_sense, tab_block], tab2, reduce_by = ['bid'], format=yt.YsonFormat(control_attributes_mode="row_fields"))

    #yt.run_sort(tab2, sort_by=['mctgs', 'CategoryNames', 'bid', 'title', 'body'])

    tab3 = '//home/catalogia/users/yuryz/multik/train'
    tab4 = '//home/catalogia/users/yuryz/multik/test'

    yt.run_reduce(sel_train_test_src, tab2, [tab3, tab4], reduce_by = ['mctgs'], format=yt.YsonFormat(control_attributes_mode="row_fields"))
    """


if __name__ == '__main__':
    main()

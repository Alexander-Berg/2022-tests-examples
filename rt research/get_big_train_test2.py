#!/usr/bin/python
# -*- coding: utf-8 -*-

#отбор баннеров для обчения и тестирования, чтобы в каждой категории было не менее 3-х баннеров
#и чтобы мультилейблы содержали РОВНО 2 категории

import sys
import re
import yt.wrapper as yt


class get_big_train(object):
    def __init__(self, ids2ctgs):
        self.ids2ctgs = ids2ctgs

    def __call__(self, rec):
        table_index = rec.pop('@table_index')
        if table_index == 0: #//home/broadmatching/users/firefish/ctg/TrainTextShard
            if rec['shard'] == 0: #шард #1
                ctg_ids = rec['AutoCategoryIDs'].split(',')
                ctgs = []
                for ctg_id in ctg_ids:
                    if ctg_id in self.ids2ctgs:
                        ctgs.append( { "id": ctg_id, "name": self.ids2ctgs[ctg_id] } )

                rec['AutoCategoryIDs'] = ','.join(sorted(ctg_ids)) #####
                if len(ctg_ids) >= 1: #число категорий
                    yield { "FOR_JOIN": rec['AutoCategoryIDs'], "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }

                #if len(ctg_ids) >= 2: #число категорий
                #    ctgs = [ { "id": 'MULTI', "name": 'MULTI' } ]
                #else:
                #    ctgs = [ { "id": 'MONO', "name": 'MONO' } ]
                #yield { "FOR_JOIN": rec['AutoCategoryIDs'], "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }
        else: #//home/catalogia/contest/TrainExact
            ctg_ids = rec['CategoryIDs'].split(',')
            ctg_names = rec['CategoryNames'].split('/')
            if len(ctg_ids) != len(ctg_names): return #####

            ctgs = []
            for i in range(len(ctg_ids)):
                ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

            rec['CategoryIDs'] = ','.join(sorted(ctg_ids)) #####
            if len(ctg_ids) >= 1: #число категорий
                yield { "FOR_JOIN": rec['CategoryIDs'], "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }

            #if len(ctg_ids) >= 2: #число категорий
            #    ctgs = [ { "id": 'MULTI', "name": 'MULTI' } ]
            #else:
            #    ctgs = [ { "id": 'MONO', "name": 'MONO' } ]
            #yield { "FOR_JOIN": rec['CategoryIDs'], "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }


def add_field(rec):
    rec['bid'] = rec['BannerID']
    yield rec


def get_big_test(key, recs):
    bid = 0
    for rec in recs:
        table_index = rec.pop('@table_index')
        if table_index == 0: #'//tmp/yuryz/Test'
            bid = rec['bid']
        elif bid != 0: #'//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext'
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

            rec['CategoryIDs'] = ','.join(sorted(ctg_ids)) #####
            if len(ctg_ids) >= 1: #число категорий
                yield { "FOR_JOIN": rec['CategoryIDs'], "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs }

            #if len(ctg_ids) >= 2: #число категорий
            #    true_ctgs = [ { "id": 'MULTI', "name": 'MULTI' } ]
            #else:
            #    true_ctgs = [ { "id": 'MONO', "name": 'MONO' } ]
            #yield { "FOR_JOIN": rec['CategoryIDs'], "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs }


def sel_ctgs(key, recs):
    L = list(recs)
    if len(L) >= 3: #MIN число баннеров в категории из обучающего набора
    ##if len(L) >= 1: #MIN число баннеров в категории из обучающего набора
        for rec in L:
            yield rec


def sel_bnrs(key, recs): #синхронизация обучающего и тестового наборов по категориям
    FOR_JOIN = ''
    for rec in recs:
        table_index = rec.pop('@table_index')
        if table_index == 0: #'//tmp/yuryz/train2'
            FOR_JOIN = rec['FOR_JOIN']
        elif FOR_JOIN != '': # '//tmp/yuryz/test1'
            yield rec


def main():
    # --- 1. Подготовка обучающего набора ---
    ids2ctgs = {} #маппинг CategoryID в CategoryName
    for rec in yt.read_table('//home/catalogia/categories_tree', raw=False): #актуальные категории
        ids2ctgs[str(rec['DirectID'])] = rec['Category']

    ##tab1 = '//home/broadmatching/users/firefish/ctg/TrainTextShard' #Юрины шарды
    ##tab2 = '//home/catalogia/contest/TrainExact'
    tab1 = '//home/catalogia/users/yuryz/multik/TrainTextShard_upd' #Юрины шарды (см. shard_prepare.py)
    tab2 = '//home/catalogia/users/yuryz/multik/TrainExact_ext' #(см. shard_prepare.py)

    tab3 = '//tmp/yuryz/train1' ####################

    yt.run_map(get_big_train(ids2ctgs), [tab1, tab2], tab3, format=yt.YsonFormat(control_attributes_mode="row_fields"))
    yt.run_sort(tab3, sort_by=['FOR_JOIN'])

    # --- 2. Подготовка тестового набора ---
    tab4 = '//home/catalogia/contest/Test'
    tab5 = '//tmp/yuryz/Test'

    yt.run_map(add_field, tab4, tab5)
    yt.run_sort(tab5, sort_by=['bid'])

    tab6 = '//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext'
    tab7 = '//tmp/yuryz/test1' ####################

    yt.run_reduce(get_big_test, [tab5, tab6], tab7, reduce_by = ['bid'], format=yt.YsonFormat(control_attributes_mode="row_fields"))
    yt.run_sort(tab7, sort_by=['FOR_JOIN'])

    # --- 3. Отбор категорий, содержащих не менее 3-х баннеров ---
    tab8 = '//tmp/yuryz/train2' ####################

    yt.run_reduce(sel_ctgs, tab3, tab8, reduce_by = ['FOR_JOIN'], format=yt.YsonFormat(control_attributes_mode="row_fields"))
    yt.run_sort(tab8, sort_by=['FOR_JOIN'])

    # --- 4. Синхронизация обучающего и тестового наборов по категориям ---
    tab9 = '//tmp/yuryz/test2' ####################

    yt.run_reduce(sel_bnrs, [tab8, tab7], tab9, reduce_by = ['FOR_JOIN'], format=yt.YsonFormat(control_attributes_mode="row_fields"))


if __name__ == '__main__':
    main()

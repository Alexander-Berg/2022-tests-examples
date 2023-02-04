#!/usr/bin/python
# -*- coding: utf-8 -*-

#подготовка большого обучающего набора и теста

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
            ##if rec['shard'] <= 1: #шарды ##1,2
                ctg_ids = rec['AutoCategoryIDs'].split(',')
                ctgs = []
                for ctg_id in ctg_ids:
                    if ctg_id in self.ids2ctgs:
                        ctgs.append( { "id": ctg_id, "name": self.ids2ctgs[ctg_id] } )

                yield { "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }
                #--- вариант для Юры ---
                #yield { "BannerID": rec['BannerID'], "Title": rec['Title'], "Body": rec['Body'], "Url": rec['Url'], "AutoCategoryIDs": rec['AutoCategoryIDs'] } # обучающая
        else: #//home/catalogia/contest/TrainExact
            ctg_ids = rec['CategoryIDs'].split(',')
            ctg_names = rec['CategoryNames'].split('/')
            if len(ctg_ids) != len(ctg_names): return #####

            ctgs = []
            for i in range(len(ctg_ids)):
                ctgs.append( { "id": ctg_ids[i], "name": ctg_names[i] } )

            if len(ctg_ids) <= 3:
                yield { "bid": rec['BannerID'], "title": rec['Title'], "body": rec['Body'], "url": rec['Url'], "categories": ctgs }
                #--- вариант для Юры ---
                #yield { "BannerID": rec['BannerID'], "Title": rec['Title'], "Body": rec['Body'], "Url": rec['Url'], "AutoCategoryIDs": rec['CategoryIDs'] } # обучающая


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

            yield { "bid": rec['bid'], "title": rec['title'], "snippet": rec['body'], "url": rec['url'], "categories": ctgs, "true_categories": true_ctgs }
            #--- вариант для Юры ---
            #yield { "BannerID": rec['bid'], "Title": rec['title'], "Body": rec['body'], "Url": rec['url'], "AutoCategoryIDs": rec['AutoCategoryIDs'], "CategoryIDs": rec['CategoryIDs'] } # тестовая


def main():
    # --- подготовка обучающего набора ---
    ids2ctgs = {} #маппинг CategoryID в CategoryName
    for rec in yt.read_table('//home/catalogia/categories_tree', raw=False): #актуальные категории
        ids2ctgs[str(rec['DirectID'])] = rec['Category']

    ##tab1 = '//home/broadmatching/users/firefish/ctg/TrainTextShard' #Юрины шарды
    ##tab2 = '//home/catalogia/contest/TrainExact'
    tab1 = '//home/catalogia/users/yuryz/multik/TrainTextShard_upd' #Юрины шарды (см. shard_prepare.py)
    tab2 = '//home/catalogia/users/yuryz/multik/TrainExact_ext' #(см. shard_prepare.py)

    tab3 = '//home/catalogia/users/yuryz/multik/big_train'

    yt.run_map(get_big_train(ids2ctgs), [tab1, tab2], tab3, format=yt.YsonFormat(control_attributes_mode="row_fields"))

    # --- подготовка тестового набора ---
    tab4 = '//home/catalogia/contest/Test'
    tab5 = '//tmp/yuryz/Test'

    #yt.run_map(add_field, tab4, tab5)
    #yt.run_sort(tab5, sort_by=['bid'])

    tab6 = '//home/catalogia/users/yuryz/etalon/marked_dataset_irt_checked_ext'
    tab7 = '//home/catalogia/users/yuryz/multik/big_test'

    #yt.run_reduce(get_big_test, [tab5, tab6], tab7, reduce_by = ['bid'], format=yt.YsonFormat(control_attributes_mode="row_fields"))


if __name__ == '__main__':
    main()

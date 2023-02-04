#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import time

from subprocess import Popen, PIPE, check_call
from copy import deepcopy
from datetime import datetime, timedelta
from hashlib import md5  # pylint: disable=E0611
from operator import and_, attrgetter, itemgetter
from itertools import groupby
from urllib2 import urlopen, URLError
from json import loads as json_loads
from os import path, mkdir
from tempfile import NamedTemporaryFile

from mapreducelib import MapReduce
from yabs.tabtools import *
from yabs.tabutils import TemporaryTableWithMeta, copyTablesWithMeta, mergeTablesFormats, read_ts_table
from yabs.threadpool import ThreadManager
from yabs.logconfig import get_table, get_properties, get_all_tables, get_logs_regexp, get_logs_regexp_time
from yabs.logger import debug, info, error, warning
# from yabs.servant import Servant
from yabs.funcs import host_option
from random import random

import yabs.ecom_recommendations as ecom_recommendations
from yabs.ecom_recommendations import EcomRecommenderPool, get_ecommerce_stat_converter, to_timestamp


from yabs.notifier import notify
import yabs.conf
yabs.conf.merge({"notifier": {"xmpp": {"notify_level": "exceptions"}}})

from contextlib import nested
from yabs.matrixnet.learn_preprocessors import BasicPreprocessor


def _select_goal(r, rec_list):
    if rec_list:
        return [min(rec_list, key=lambda r: r.GoalSourceID)]
    else:
        return rec_list


class APCModelPreparation(BasicPreprocessor):
    """ This one is a quite hacky one - it doesn't depend on the src_tables"""

    def __init__(
            self,
            days_to_learn=12,
            max_delay_days=2,
            add_fields=None,
            use_rotation_goals=False,
            use_parent_banner=True,
            use_target_phrase=True,
            last_log_date=None,
            cut_fields=True,
            use_attr_type=False,
            round_date=False,
            ef_logs_mask="auto-budget-prediction/vw_apc_src_ef_(?P<TIME>%Y%m%d%H)",
            efv_logs_mask="auto-budget-prediction/vw_apc_src_efv_(?P<TIME>%Y%m%d%H)",
        ):

        self.days_to_learn = days_to_learn
        self.max_delay_days = max_delay_days
        self.efv_min_log_number = (self.days_to_learn - self.max_delay_days) * 24
        self.ef_min_log_number = (self.days_to_learn - self.max_delay_days) * 24
        self.add_fields = add_fields or []
        if 'HitDeviceType' not in self.add_fields:
            self.add_fields.append('HitDeviceType')

        self.use_rotation_goals = use_rotation_goals
        self.use_parent_banner = use_parent_banner
        self.use_target_phrase = use_target_phrase
        self.end_log_date = datetime.now() if last_log_date is None else last_log_date + timedelta(days=1)
        if round_date:
            self.end_log_date = self.end_log_date.replace(hour=0, minute=0, second=0, microsecond=0)
        self.start_log_date = self.end_log_date - timedelta(days=self.days_to_learn)
        self._ef_logs_mask = ef_logs_mask
        self._efv_logs_mask = efv_logs_mask
        self._make_cut_fields = cut_fields
        self.use_attr_type = use_attr_type
        dynamic_context_types_str = host_option('yabs-dynamic-context-types') or '7,8'
        self.dynamic_context_types = [int(ct) for ct in dynamic_context_types_str.split(',')]

        self.regenerated_fields = ['-AutoBudgetGoalID', '-GoalID', '-GoalReached', '-ClickNum', '-CurrencyID',
                                   '-Price', '-Profitability', '-GoalSourceID', '-GoalReached', '-GoalReached_temp']

        self.main_keys = ['GoalID', 'OrderID', 'Profitability', 'LogID', 'DomainID', 'BannerID', 'GroupBannerID', 'PhraseID', 'TypeID',
                          'ContextType', 'EventTime', 'SelectType', 'PageID', 'RegionID', 'DeviceType', 'GoalSourceID']


    def JoinTables(self, ef, postclick, dst_table):
        with TemporaryTableWithMeta(debug=True) as outer_join_temp, \
                TemporaryTableWithMeta(debug=True) as budget_join_temp:
            with TemporaryTableWithMeta(debug=True) as ef_table, \
                    TemporaryTableWithMeta(debug=True) as efv_table:
#                copyTablesWithMeta(efv, efv_table.name)
                mr_do_map([Cut(self.regenerated_fields)], src_tables=ef, dst_tables=[ef_table.name])
                mr_do_map(
                    [
                        Mapper('r.AppInstalled = 1'),
                        Cut(['AppInstalled'], keys=['LogID'])
                    ],
                    formatID=True,
                    src_tables=postclick,
                    dst_tables=[efv_table.name],
                    appendMode=True
                )
                # joining ef logs containing all clicks with efv log containing only events
                # with corresponding visits
                mr_do_join(
                    Join(
                        ef_table.name,
                        efv_table.name,
                        joinType="outer",
                        keys=['LogID'],
                        conflict_prefix='EFV'
                    ),
                    dst_tables=[outer_join_temp.name]
                )
            # joining order goals which can come either from AutoBudgetOrder or LocalOrderInfo
            with TemporaryTableWithMeta() as order_goals:
                mr_do_map(
                    [
                        Mapper('r.GoalSourceID = 1'),
                        Cut(['GoalID', 'Profitability', 'GoalSourceID'], keys=['OrderID']),
                        Grep('r.GoalID >= 0')
                    ],
                    src_tables=['//home/yabs/dict/AutoBudgetOrder'],
                    dst_tables=[order_goals.name]
                )
                if self.use_rotation_goals:
                    local_order_table = '//home/yabs/banana/LocalOrderInfo'
                    mr_do_map(
                        [
                            Mapper('''
                                r.GoalID = r.RotationGoalID
                                r.Profitability = 0
                                r.GoalSourceID = 2
                            '''),
                            Cut(['GoalID', 'Profitability', 'GoalSourceID'], keys=['OrderID']),
                            Grep('r.GoalID >= 0')
                        ],
                        src_tables=[local_order_table],
                        dst_tables=[order_goals.name],
                        appendMode=True
                    )
                    local_order_direct_table = '//home/bs/local-ads/LocalOrderDirect'
                    mr_do_map(
                        [
                            Mapper('''
                                   r.Profitability = 0
                                   r.GoalSourceID = 3
                            '''),
                            Cut(['GoalID', 'Profitability', 'GoalSourceID'], keys=['OrderID']),
                            Grep('r.GoalID >= 0')
                        ],
                        src_tables=[local_order_direct_table],
                        dst_tables=[order_goals.name],
                        appendMode=True
                    )
                # We are going to find out whether order goal was reached or not by reducing all rows corresponding
                # to one click. Moving all fields needed later to key to preserve them after reduce. Alternatively we
                # could be using smth like Last reduce item as these fields come from event log and does not depend
                # on visit log
                postmap_mappers = [Grep('r.GoalID >= 0')]
                if self.use_parent_banner:
                    # for dynamic banners we are using BannerID and PhraseID from parent banner
                    postmap_mappers.append(
                        Mapper('''
                            if r.ContextType in %s:
                                r.BannerID = r.ParentBannerID or r.BannerID
                        ''' % self.dynamic_context_types),
                    )
                if self.use_target_phrase:
                    postmap_mappers.append(
                        Mapper('''
                            if r.ContextType in %s:
                                r.PhraseID = r.TargetPhraseID
                        ''' % self.dynamic_context_types),
                    )

                if self._make_cut_fields:
                    add_fields = self.add_fields
                else:
                    outer_join_temp_fmt = mergeTablesFormats([outer_join_temp.name])
                    add_fields = self.add_fields + outer_join_temp_fmt.getNames()

                postmap_mappers += [
                    Mapper('''
                        goals = simplejson.loads(r.ReachedGoals) if r.ReachedGoals else []
                        r.GoalReached = int((r.GoalID in goals) or
                            (r.GoalID in [0, 3] and bool(r.AppInstalled)))
                    ''', add_fields=[('GoalReached', int)]),
                    Cut(fields=['GoalReached'] + add_fields, keys=self.main_keys)
                ]
                mr_do_map(
                    [
                        MapJoin(
                            right_table=order_goals.name,
                            joinType='inner',
                            keys=['OrderID'],
                            conflict_prefix='AutoBudget'
                        )
                    ] + postmap_mappers,
                    begin='''
                        import simplejson
                    ''',
                    src_tables=[outer_join_temp.name],
                    dst_tables=[dst_table]
                )


    def process_logs(self, srcs, dst):
        ef_logs_list = []
        try:
            ef_logs_hash = get_logs_regexp_time(
                self._ef_logs_mask,
                self.start_log_date,
                self.end_log_date
            )

            for ef_log_hash in ef_logs_hash:
                ef_logs_list.append(ef_log_hash['name'])
        except ValueError, e:
            info(e)
            exit(0)

#        efv_logs_list = []
#        try:
#            efv_logs_hash = get_logs_regexp_time(
#                self._efv_logs_mask,
#                self.start_log_date,
#                self.end_log_date
#            )
#
#            for efv_log_hash in efv_logs_hash:
#                efv_logs_list.append(efv_log_hash['name'])
#        except ValueError, e:
#            info(e)
#            exit(0)
        postclick_logs_list = []
        try:
            postclick_logs_hash = get_logs_regexp_time(
                '//home/logfeller/logs/metrika-postclicks-log/1d/(?P<TIME>%Y-%m-%d)$',
                self.start_log_date - timedelta(days=1, seconds=-1),
                self.end_log_date
            )
            for postclick_log_hash in postclick_logs_hash:
                postclick_logs_list.append(postclick_log_hash['name'])
        except ValueError, e:
            info(e)
            exit(0)

        info("Found %d/%d ef logs"
             % (len(ef_logs_list), self.ef_min_log_number))
#        info("Found %d/%d efv logs"
#             % (len(efv_logs_list), self.efv_min_log_number))
        info('Found postclick logs: %s' % postclick_logs_list)

        if len(ef_logs_list) < self.ef_min_log_number:
            info("It's not enough - stop")
            exit(0)

        info("Joining efv with ef logs")
        self.JoinTables(
            ef_logs_list,
            postclick_logs_list,
            dst
        )

        # Joining with BMCategories
        # NOTICE: BMCategoryXXXIDs in BannerToTexts doesn't have the same format
        #                  as BMCategoryXXXID in logs written by engine (for example in EFHWV logs)
        with TemporaryTableWithMeta() as tmp:
            info("Retrieving from stat/BannerToTexts mapping BannerID -> BMCategory{1,2,3}ID's")
            mr_do_map(
                [Cut(fields=["BannerID", "BMCategory1ID", "BMCategory2ID", "BMCategory3ID"])],
                src_tables=["stat/BannerToTexts"],
                dst_tables=[tmp.name]
            )
            mr_do_reduce(FirstRecordReducer(), src_tables=[tmp.name], dst_tables=[tmp.name])

            info("Joining BMCategories to learning log")
            postmap = []
            if self._make_cut_fields:
                postmap = [
                    Cut(self.main_keys +
                        ['GoalReached', 'BMCategory1ID', 'BMCategory2ID', 'BMCategory3ID'] +
                        self.add_fields)
                ]
            mr_do_join(
                Join(dst, tmp.name, keys=['BannerID'], joinType='outer', conflict_prefix='BannerToTexts'),
                formatID=True,
                postmap=postmap,
                dst_tables=[dst]
            )



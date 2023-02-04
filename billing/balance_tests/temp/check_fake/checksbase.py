#!/usr/bin/python
# -- coding: utf-8 --

import functools
from multiprocessing.dummy import Pool
import pickle
import datetime
import os

from nose.plugins import builtin, base

date_format = "%d.%m.%Y"


class CollectNames(base.Plugin):
    def configure(self, options, conf):
        # base.Plugin.configure(self, options, conf)
        self.enabled = True

    def loadTestsFromNames(self, names, module=None):
        for n in names:
            if len(n.split(':')) > 1:
                TestBase._names.append(n)


builtin.plugins.append(CollectNames)


def prepare_wrapper(fname):
    def prepare_wrapper(f1):
        @functools.wraps(f1)
        def wrapper(*args, **kwargs):
            return f1(*args, **kwargs)

        wrapper.prepare_for = fname
        return wrapper

    return prepare_wrapper


class TestBase(object):
    _names = []
    prepare = staticmethod(prepare_wrapper)

    @classmethod
    def setup_class(cls):
        ##### есть 2 файлика: tod.dat и tom.dat (последний хранит в себе дату (со временем на всякий случай, на когда были созданы данные в нём)
        #####    Алгоритм первым делом заходит в tom.dat - если дата в нём:
        #####    - сегодня: то перекладываем данные в tod.dat, генерим новые и складываем в tom.dat с датой "завтра".
        #####    - завтра: выходим ничего не делая (значит, что это не уже первый запуск в этих сутках).
        #####    Далее берём данные из tod.dat и запускаем сверку для них
        base_path = os.path.dirname(__file__)
        tom_name = os.path.join(base_path, 'data', str(cls.__name__) + '_tom.pkl')
        tod_name = os.path.join(base_path, 'data', str(cls.__name__) + '_tod.pkl')
        print tom_name
        print tod_name
        try:
            tom = open(tom_name, 'rb')
            tom_data = pickle.load(tom)
            print 'tom_data:'
            print tom_data
            print '!!!!!!'
            file_date = datetime.datetime.now().strftime(date_format)
            if tom_data['date'] == str(file_date):
                data_for_check = open(tod_name, 'wb')
                pickle.dump(tom_data, data_for_check)
                data_for_check.close()
                ####генерим новые данные
                cls.do_all_prepare(parallel=0)
                ####кладем их в tom.pkl  ????
            tom.close()
        except IOError as e:
            print 'No file found'
        #####        берём данные из tod.dat и запускаем сверку для них
        cls.run_check()

    @classmethod
    def do_all_prepare(cls, parallel=0):
        all_prepare = [p for p in cls.__dict__.itervalues() if hasattr(p, 'prepare_for')]
        all_prepare = [p for p in all_prepare if
                       "%s:%s.%s" % (cls.__module__, cls.__name__, p.prepare_for) in cls._names or not cls._names]
        print all_prepare
        # print "%s:%s.%s"%(cls.__module__, cls.__name__)
        if parallel:
            pool = Pool(parallel)
            pool.map(lambda x: x(), all_prepare)
            pool.close()
            pool.join()
        else:
            for p in all_prepare:
                p()

    @classmethod
    def run_check(cls):
        pass

# -*- coding: utf-8 -*-

##from temp.MTestlib import mtl
import pprint

cnt = 10


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def rnd_transfer():
    orders_list = [x for x in range(cnt)]
    # dst_list = [order_list for x in order_]
    # for x in int(random.random() * cnt):


rnd_transfer()

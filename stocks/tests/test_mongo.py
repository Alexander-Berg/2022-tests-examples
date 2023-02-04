# -*- coding: utf-8 -*-
""" Тест подключения к mongodb. """

from pymongo import MongoClient

# from pprint import pprint
# import threading

# MongoDB
connectionstring = "mongodb://admin:admin@" \
                   "mongod0.wdb-dev.yandex.net," \
                   "mongod1.wdb-dev.yandex.net," \
                   "mongod2.wdb-dev.yandex.net/"
# connectionstring = "mongodb://stocksuser:zIA4zbDJbp39pLOUCCSF@n1.pmongo.yandex.net,i1.pmongo.yandex.net," \
#                    "m1.pmongo.yandex.net,s1.pmongo.yandex.net/stocksdb"

# offset = 0
# offset_lock = threading.Lock()
# print_lock = threading.Lock()

client = MongoClient(connectionstring)
db = client.stocksdb
stocks = db.stocks
print(stocks.find({'id': 10000}).count())

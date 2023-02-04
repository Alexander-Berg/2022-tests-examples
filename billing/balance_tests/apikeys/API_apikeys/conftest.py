# coding=utf-8
__author__ = 'vertail'


import pymongo
import pytest



@pytest.fixture(scope='session', autouse=True)
def db_connection():
    client = pymongo.MongoClient('mongo.apikeys-test.paysys.yandex.net:27217')
    client.apikeys.authenticate('bo', 'balalancing')
    return client.apikeys
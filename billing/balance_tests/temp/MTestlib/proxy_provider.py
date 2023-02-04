# -*- coding: utf-8 -*-

import xmlrpclib
import pprint


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def GetServiceProxy(host='greed-ts1f', test=0):
    if host.__class__ is tuple:
        login = host[1]
        host = host[0]

    if host != 'greed-dev4f':
        BALANCE_MEDIUM_PORT = 8002
        BALANCE_TEST_XMLRPC_HTTP_PORT = 30702
        if test:
            XMLRPC_url = "http://xmlrpc.balance.{0}.yandex.ru:{1}/xmlrpc".format(host, BALANCE_TEST_XMLRPC_HTTP_PORT)
        else:
            XMLRPC_url = "http://xmlrpc.balance.{0}.yandex.ru:{1}/xmlrpc".format(host, BALANCE_MEDIUM_PORT)
    else:
        # if test: XMLRPC_url = 'http://{0}-xmlrpc-test.greed-dev4f.yandex.ru'.format(login)
        # else: XMLRPC_url = 'http://{0}-xmlrpc-medium.greed-dev4f.yandex.ru'.format(login)
        BALANCE_MEDIUM_PORT = 31196
        BALANCE_TEST_XMLRPC_HTTP_PORT = 31191
        if test:
            XMLRPC_url = "http://xmlrpc.balance.{0}.yandex.ru:{1}/xmlrpc".format(host, BALANCE_TEST_XMLRPC_HTTP_PORT)
        else:
            XMLRPC_url = "http://xmlrpc.balance.{0}.yandex.ru:{1}/xmlrpc".format(host, BALANCE_MEDIUM_PORT)

    return xmlrpclib.ServerProxy(XMLRPC_url, allow_none=1, use_datetime=1)


if __name__ == '__main__':
    main()

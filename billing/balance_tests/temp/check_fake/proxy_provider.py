# -*- coding: utf-8 -*-

# -------------------------------------------------------------------------------
# Name:        модуль1
# Purpose:
#
# Author:      torvald
#
# Created:     24.12.2014
# Copyright:   (c) torvald 2014
# Licence:     <your licence>
# -------------------------------------------------------------------------------

import xmlrpclib
import pprint


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def GetServiceProxy(host='greed-ts1f', test=0):
    if test:
        XMLRPC_url = "http://xmlrpc.balance.%s.yandex.ru:30702/xmlrpc" % host
    else:
        XMLRPC_url = "http://%s.yandex.ru:8002/xmlrpc" % host

    return xmlrpclib.ServerProxy(XMLRPC_url, allow_none=1, use_datetime=1)


if __name__ == '__main__':
    main()

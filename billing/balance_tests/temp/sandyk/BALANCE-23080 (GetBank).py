# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import pprint
import xmlrpclib

tm = xmlrpclib.ServerProxy("http://greed-tm1f.yandex.ru:8002/xmlrpc", allow_none=1, use_datetime=1)

# t = tm.Balance.GetBank({'Bik':'041403759'})
t = tm.Balance.GetBank({'Swift':'CGISHKH1XXX','Bik':'041403759'})
pprint.pprint(t)
for key in t.keys():
    print key,(t[key])


# coding: utf-8
import xmlrpclib
import pprint
import datetime


##def Print(obj):
##    print pprint.pformat(obj).decode('unicode_escape')

##def q(obj):      
start_dt = end_dt = datetime.datetime(2015,3,10)
proxy = xmlrpclib.ServerProxy("http://greed-tm1f.yandex.ru:8002/xmlrpc")

res = proxy.Balance.GetInternalPagesTagsStat(start_dt, end_dt)
##res = proxy.Balance.GetPagesTagsStat(start_dt, end_dt)
print 'Results: %s' % (res)
##Print(res)
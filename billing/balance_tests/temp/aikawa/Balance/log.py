# import xmlrpclib
#
#
# rpc = xmlrpclib.ServerProxy("http://gavrilovp.greed-dev4f.yandex.ru:39891/xmlrpc",
#                             allow_none=1, use_datetime=1)
#
# print rpc.ExportObject("OEBS", "Invoice", 51886255)
#
#
# from balance import balance_steps as steps
#
# steps.CommonSteps.export("OEBS", "Act", 51936963)
# steps.CommonSteps.export("OEBS", "Invoice", 51900019)


# print u'\u0411\u0435\u0437\u041d\u0414\u0421'
import datetime

dt = datetime.datetime(2016, 01, 31, 11, 0, 0)
dt_after = dt + datetime.timedelta(days=90)
print dt_after
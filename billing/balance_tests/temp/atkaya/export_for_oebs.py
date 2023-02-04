# coding: utf-8
import xmlrpclib

EXPORT_TYPE = 'OEBS'

CLASSNAME_PERSON = 'Person'
CLASSNAME_CLIENT = 'Client'
CLASSNAME_CONTRACT = 'Contract'
CLASSNAME_CONTRACT_COLLATERAL = 'ContractCollateral'
CLASSNAME_INVOICE = 'Invoice'
CLASSNAME_ACT = 'Act'
CLASSNAME_TRANSACTION = 'ThirdPartyTransaction'

testproxy = xmlrpclib.ServerProxy("http://xmlrpc.balance.greed-tm.paysys.yandex.ru:30702/xmlrpc", allow_none=1, use_datetime=1)

print testproxy.TestBalance.ExportObject(EXPORT_TYPE, CLASSNAME_CONTRACT, 554114)
print testproxy.TestBalance.ExportObject(EXPORT_TYPE, CLASSNAME_CONTRACT_COLLATERAL, 804617)

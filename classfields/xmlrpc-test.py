#!/usr/bin/env python
# -*- coding: utf-8 -*-
import xmlrpclib

uid = '485576718'
serviceId = 82
orderId = 1534
clientId = '32511881'
paysysId = 1201003
personId = 6640230
contractId = 1002582

server = xmlrpclib.ServerProxy('http://greed-ts.paysys.yandex.ru:8002/xmlrpc/', allow_none=1, use_datetime=1)
rpc = server.Balance2

#print(rpc.GetClientByIdBatch([uid]))
#print xmlrpclib.dumps(('4010655430', {'person_id': '6505434', 'client_id': '81189741', 'type': 'ur', 'name': 'ПРОСТОР-РИЭЛТИ', 'longname': 'ООО "ПРОСТОР-РИЭЛТИ"', 'phone': '+74957776655', 'email': '28april@test.com', 'legaladdress': 'Санкт-Петербург, ул. Виноградова д.10', 'inn': '781714171469', 'kpp': '15', 'account': '666666666666666666', 'signer-person-name': 'Дрыгпрыг Великодушный', 'postcode': '333444', 'postaddress': 'Москва, ул. Новая, д. 75' }), methodname='CreatePerson')
#res = rpc.CreatePerson('4010655430', {'person_id': '6505434', 'client_id': '81189741', 'type': 'ur', 'name': 'ПРОСТОР-РИЭЛТИ', 'longname': 'ООО "ПРОСТОР-РИЭЛТИ"', 'phone': '+74957776655', 'email': '28april@test.com', 'legaladdress': 'Санкт-Петербург, ул. Виноградова д.10', 'inn': '781714171469', 'kpp': '15', 'account': '666666666666666666', 'signer-person-name': 'Дрыгпрыг Великодушный', 'postcode': '333444', 'postaddress': 'Москва, ул. Новая, д. 75' })
#print(res)
res = rpc.GetContractCreditsDetailed({'ContractID': 278656}, {'UnpaidInvoices': 1})
print(res)

# print(rpc.ListClientPassports(0,  60653856))

# request = rpc.CreateRequest2(uid, clientId, [{'ServiceID': serviceId, 'ServiceOrderID': orderId, 'Qty': '10'}])
# requestId = request['RequestID']
# res = rpc.CreateInvoice2(uid, {'RequestID': requestId, 'PaysysID': paysysId, 'PersonID': personId, 'ContractID': contractId, 'Credit': 1})
# print(res)

#print xmlrpclib.dumps(('485576718', 32511881, [{'ServiceID': 82, 'ServiceOrderID': 1524, 'Qty': '10'}]), methodname='CreateRequest2')
#print xmlrpclib.dumps(('485576718', {'RequestID': 695058696, 'PaysysID': 1201003, 'PersonID': 4082869, 'ContractID': 278656, 'Credit': 1}), methodname='CreateInvoice2')
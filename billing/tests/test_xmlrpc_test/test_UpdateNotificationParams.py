from balance import constants

from tests import object_builder as ob

CODE_SUCCESS = 'SUCCESS'


def test_change_protocol(session, test_xmlrpc_srv):
    snp = ob.ServiceNotifyParamsBuilder.construct(session, protocol='xml-rpc')
    session.flush()
    response = test_xmlrpc_srv.UpdateNotificationParams(snp.service_id, {'protocol': 'json-rest'})
    assert response == [0, CODE_SUCCESS]
    assert snp.protocol == 'json-rest'


def test_change_url(session, test_xmlrpc_srv):
    snp = ob.ServiceNotifyParamsBuilder.construct(session, test_url='https://url.before.ru', url='https://url.prod.ru')
    session.flush()
    response = test_xmlrpc_srv.UpdateNotificationParams(snp.service_id, {'url': 'https://url.after.ru'})
    assert response == [0, CODE_SUCCESS]
    assert snp.test_url == 'https://url.after.ru'
    assert snp.url == 'https://url.prod.ru'

from balance import constants

from tests import object_builder as ob


def test_get_notifications_params(session, test_xmlrpc_srv):
    snp = ob.ServiceNotifyParamsBuilder.construct(session)
    assert test_xmlrpc_srv.GetNotificationParams(snp.service_id) == {'version': snp.version,
                                                                       'protocol': snp.protocol,
                                                                       'test_url': snp.test_url}

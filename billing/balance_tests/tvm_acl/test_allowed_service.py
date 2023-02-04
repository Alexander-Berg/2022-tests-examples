# coding=utf-8

from balance.mapper import TVMACLAllowedService

from tests import object_builder as ob


def test_no_app(session):
    assert TVMACLAllowedService.allowed_services(
        session,
        tvm_id=ob.generate_int(5),
        env_type='test'
    ) == []


def test_no_allowed_services(session):
    tvm_id = ob.generate_int(5)
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    assert TVMACLAllowedService.allowed_services(
        session,
        tvm_id=tvm_id,
        env_type='test'
    ) == []


def test_has_allowed_services(session):
    tvm_id = ob.generate_int(5)
    service_id = ob.ServiceBuilder.construct(session).id
    # В ответе должен быть только один сервис
    ob.ServiceBuilder.construct(session)
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLAllowedServiceBuilder.construct(session, tvm_id=tvm_id, service_id=service_id)
    assert TVMACLAllowedService.allowed_services(
        session,
        tvm_id=tvm_id,
        env_type='test'
    ) == [service_id]


def test_wrong_env(session):
    tvm_id = ob.generate_int(5)
    service_id = ob.ServiceBuilder.construct(session).id
    # В ответе должен быть только один сервис
    ob.ServiceBuilder.construct(session)
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLAllowedServiceBuilder.construct(session, tvm_id=tvm_id, service_id=service_id)
    assert TVMACLAllowedService.allowed_services(
        session,
        tvm_id=tvm_id,
        env_type='prod'
    ) == []


def test_wrong_tvm_id(session):
    tvm_id = ob.generate_int(5)
    service_id = ob.ServiceBuilder.construct(session).id
    # В ответе должен быть только один сервис
    ob.ServiceBuilder.construct(session)
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLAllowedServiceBuilder.construct(session, tvm_id=tvm_id, service_id=service_id)
    assert TVMACLAllowedService.allowed_services(
        session,
        tvm_id=tvm_id + 1,
        env_type='test'
    ) == []

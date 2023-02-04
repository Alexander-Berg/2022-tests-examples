
from balance.mapper import TVMACLPermission

from tests import object_builder as ob


def test_no_app(session):
    assert not TVMACLPermission.is_allowed(
        session, 'xmlrpc', 'Method',
        tvm_id=ob.generate_int(5),
        env_type='test'
    )


def test_no_permissions(session):
    tvm_id = ob.generate_int(5)
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    assert not TVMACLPermission.is_allowed(
        session, 'xmlrpc', 'Method',
        tvm_id=tvm_id,
        env_type='test'
    )


def test_has_permission_for_different_method_name(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id, endpoint=endpoint, method_name='MethodOne'
    )
    assert not TVMACLPermission.is_allowed(
        session, endpoint, 'MethodTwo',
        tvm_id=tvm_id,
        env_type='test'
    )


def test_has_permission_for_different_endpoint(session):
    tvm_id = ob.generate_int(5)
    method_name = 'Method'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id, endpoint='xmlrpc_one', method_name=method_name
    )
    assert not TVMACLPermission.is_allowed(
        session, 'xmlrpc_two', method_name,
        tvm_id=tvm_id,
        env_type='test'
    )


def test_has_permission_for_different_env(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id, env='prod')
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id, endpoint=endpoint, method_name=method_name
    )
    assert not TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id,
        env_type='test'
    )


def test_wrong_tvm_id_plain(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id, endpoint=endpoint, method_name=method_name
    )
    assert not TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id + 1,
        env_type='test'
    )


def test_has_plain_permission(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id, endpoint=endpoint, method_name=method_name
    )
    assert TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id,
        env_type='test'
    )


def test_has_group_permission(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    group_name = 'MethodGroup'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLGroupBuilder.construct(session, name=group_name)
    ob.TVMACLGroupMethodBuilder.construct(session, group_name=group_name, endpoint=endpoint, 
                                          method_name=method_name)
    ob.TVMACLGroupPermissionBuilder.construct(session, group_name=group_name, tvm_id=tvm_id)
    assert TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id,
        env_type='test'
    )


def test_group_has_different_permission(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    group_name = 'MethodGroup'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLGroupBuilder.construct(session, name=group_name)
    ob.TVMACLGroupMethodBuilder.construct(session, group_name=group_name, endpoint=endpoint,
                                          method_name='MethodOne')
    ob.TVMACLGroupPermissionBuilder.construct(session, group_name=group_name, tvm_id=tvm_id)
    assert not TVMACLPermission.is_allowed(
        session, endpoint, 'MethodTwo',
        tvm_id=tvm_id,
        env_type='test'
    )


def test_wrong_group(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    correct_group_name = 'MethodGroup'
    incorrect_group_name = 'OtherGroup'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLGroupBuilder.construct(session, name=correct_group_name)
    ob.TVMACLGroupBuilder.construct(session, name=incorrect_group_name)
    ob.TVMACLGroupMethodBuilder.construct(session, group_name=correct_group_name, endpoint=endpoint,
                                          method_name=method_name)
    ob.TVMACLGroupPermissionBuilder.construct(session, group_name=incorrect_group_name, tvm_id=tvm_id)
    assert not TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id,
        env_type='test'
    )


def test_wrong_tvm_id_group(session):
    tvm_id = ob.generate_int(5)
    endpoint = 'xmlrpc'
    method_name = 'Method'
    group_name = 'MethodGroup'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLGroupBuilder.construct(session, name=group_name)
    ob.TVMACLGroupMethodBuilder.construct(session, group_name=group_name, endpoint=endpoint,
                                          method_name=method_name)
    ob.TVMACLGroupPermissionBuilder.construct(session, group_name=group_name, tvm_id=tvm_id)
    assert not TVMACLPermission.is_allowed(
        session, endpoint, method_name,
        tvm_id=tvm_id + 1,
        env_type='test'
    )

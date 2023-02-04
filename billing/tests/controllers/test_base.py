import pytest
import marshmallow


from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.controllers import KeyServiceConfigSchema, UserSchema


@pytest.fixture()
def custom_field_config_domain():
    yield mapper.CustomFieldConfig(
        name='http_referer_list',
        _type='list',
        _params=mapper.keys.CustomFieldConfigParams(member_type='str'),
        _validator=mapper.keys.CustomFieldConfigValidator(name='domain', params={'auto_punycode': True}),
        value=None
    )


@pytest.fixture()
def custom_field_config_ip_or_net():
    yield mapper.CustomFieldConfig(
        name='http_ip_list',
        _type='list',
        _params=mapper.keys.CustomFieldConfigParams(member_type="str"),
        _validator=mapper.keys.CustomFieldConfigValidator(name='ip_or_net', params={'ipv4': True, 'ipv6': True}),
        value=None
    )


@pytest.fixture()
def custom_field_config_regex():
    yield mapper.CustomFieldConfig(
        name='three_digits',
        _type='str',
        _validator=mapper.keys.CustomFieldConfigValidator(name='reg_exp', params={'reg_exp': '\\d{3}'}),
        value=None
    )


def test_custom_field_simple_domain_validation(custom_field_config_domain):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain]), partial=True)
    domain = 'ya.ru'
    data = schema.load_data({'custom_params': {'http_referer_list': [domain]}})
    assert data['custom_params']['http_referer_list'][0] == domain


def test_custom_field_idn_domain_validation(custom_field_config_domain):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain]), partial=True)
    domain = 'яндекс.рф'
    data = schema.load_data({'custom_params': {'http_referer_list': [domain]}})
    assert data['custom_params']['http_referer_list'][0] == domain


def test_custom_field_wrong_domain_validation(custom_field_config_domain):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain]), partial=True)
    domain = 'not a domain'
    with pytest.raises(marshmallow.exceptions.ValidationError):
        schema.load_data({'custom_params': {'http_referer_list': [domain]}})


def test_custom_field_simple_ip_validation(custom_field_config_ip_or_net):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_ip_or_net]), partial=True)
    ip_list = ['127.0.0.1', '10.0.0.0/24', 'fe80::b5ec:478a:c45b:2c67', 'fe80::c5fd:a659:e5f3:bfe0/10']
    data = schema.load_data({'custom_params': {'http_ip_list': ip_list}})
    assert set(data['custom_params']['http_ip_list']) == set(ip_list)


def test_custom_field_wrong_ip_validation(custom_field_config_domain):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain]), partial=True)
    ip = '256.0.0.1'
    with pytest.raises(marshmallow.exceptions.ValidationError):
        schema.load_data({'custom_params': {'http_ip_list': [ip]}})


def test_custom_field_combine_validation(custom_field_config_domain, custom_field_config_ip_or_net):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain, custom_field_config_ip_or_net]), partial=True)
    ip_list = ['127.0.0.1', '10.0.0.0/24', 'fe80::b5ec:478a:c45b:2c67', 'fe80::c5fd:a659:e5f3:bfe0/10']
    domain_list = ['яндекс.рф', 'yandex.com.tr']
    data = schema.load_data({'custom_params': {'http_ip_list': ip_list}})
    assert set(data['custom_params']['http_ip_list']) == set(ip_list)

    data = schema.load_data({'custom_params': {'http_ip_list': ip_list, 'http_referer_list': domain_list}})
    assert set(data['custom_params']['http_ip_list']) == set(ip_list)
    assert set(data['custom_params']['http_referer_list']) == set(domain_list)


def test_custom_field_combine_wrong_validation(custom_field_config_domain, custom_field_config_ip_or_net):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_domain, custom_field_config_ip_or_net]), partial=True)
    ip_list = ['127.0.0.1', '10.0.0.0/24', 'ie80::b5ec:478a:c45b:2c67', 'fe80::c5fd:a659:e5f3:bfe0/10']
    domain_list = ['яндекс.рф', 'not a domain']

    with pytest.raises(marshmallow.exceptions.ValidationError) as validation_error:
        schema.load_data({'custom_params': {'http_ip_list': ip_list, 'http_referer_list': domain_list}})

    assert validation_error.value.messages == {
        'custom_params': {
            'http_referer_list': {1: ['Invalid value.']},
            'http_ip_list': {2: ['Invalid value.']}
        }
    }


def test_custom_field_simple_regex_validation(custom_field_config_regex):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_regex]), partial=True)
    value = '123'
    data = schema.load_data({'custom_params': {'three_digits': value}})
    assert data['custom_params']['three_digits'] == value


def test_custom_field_wrong_regex_validation(custom_field_config_regex):
    schema = KeyServiceConfigSchema(
        context=dict(custom_fields_config_list=[custom_field_config_regex]), partial=True)
    value = 'abc'
    with pytest.raises(marshmallow.exceptions.ValidationError):
        schema.load_data({'custom_params': {'three_digits': value}})


def test_user_roles_are_serialized_correctly(mongomock, user_manager):
    assert user_manager.roles == ['manager_of_simple_service']
    assert user_manager.roles_ref == [mapper.Role.getone(pk='manager_of_simple_service')]
    assert UserSchema().dump_data(user_manager) == {
        'balance_client_id': None,
        'email': None,
        'login': None,
        'n_project_slots': 1,
        'region_id': None,
        'currency': None,
        'name': None,
        'roles': [{'name': 'manager_of_simple_service'}],
        'uid': 1120000000000001
    }

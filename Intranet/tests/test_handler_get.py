# encoding: utf-8

from __future__ import unicode_literals

import itertools
from urllib import quote

import mock
import pytest
from tornado import gen


@pytest.fixture
def create_handler():
    from intranet.webauth.lib import auth_request_handler

    class MockHandler(auth_request_handler.AuthRequestHandler):
        class MockRequest(object):
            def __init__(self, query_arguments):
                self.test_result = None
                self.cookies = {}
                self.host = 'xxx'
                self.query_arguments = query_arguments
                self.headers = {'X-Forwarded-For': '0.0.0.0'}

        def __init__(self, query_arguments):
            self.request = MockHandler.MockRequest(query_arguments)

        def decline_request(self, reasons, login='', uid=''):
            self.test_result = False
            self.return_code = 403
            self.answer_text = ''
            for reason in reasons:
                self.answer_text += reason + "\n"

        def accept_request(self, login, uid):
            self.test_result = True
            self.return_code = 200
            self.answer_text = 'Auth completed\n'

        def get_query_argument(self, key, default):
            return self.request.query.get(key, default)

    def handler(query_arguments=None):
        if query_arguments is None:
            query_arguments = {}
        mock_handler = MockHandler(query_arguments)
        return mock_handler

    return handler


@pytest.fixture
def mock_validation_steps(monkeypatch):
    validation_steps = []

    def wrapper(statuses=None, logins=None):
        if statuses is None:
            statuses = {}
        if logins is None:
            logins = {}

        class MockStep(object):
            def __init__(self, *args):
                pass

        class MockCertificateStep(MockStep):
            @gen.coroutine
            def check(self):
                status = statuses.get('cert', True)
                login = logins.get('cert', 'dummy')
                validation_steps.append('cert')
                raise gen.Return((status, (login, 'testuid') if status else 'error'))

        class MockTokenStep(MockStep):
            @gen.coroutine
            def check(self):
                status = statuses.get('token', True)
                login = logins.get('token', 'dummy')
                validation_steps.append('token')
                raise gen.Return((status, (login, 'testuid') if status else 'error'))

        class MockCookiesStep(MockStep):
            @gen.coroutine
            def check(self):
                status = statuses.get('cookies', True)
                login = logins.get('cookies', 'dummy')
                validation_steps.append('cookies')
                raise gen.Return((status, (login, 'testuid') if status else 'error'))

        monkeypatch.setattr('intranet.webauth.lib.step.LOGIN_STEPS', {
            'cert': MockCertificateStep,
            'cookies': MockCookiesStep,
            'token': MockTokenStep
        })

        return validation_steps

    return wrapper


@gen.coroutine
def mocked_check_role_cache(login, idm_role, fields_data):
    if login == 'dummy' and idm_role == 'tetsrole' and fields_data is None:
        raise gen.Return(True)
    else:
        raise gen.Return(False)


@pytest.mark.gen_test
def test_empty_query(create_handler, mock_validation_steps):
    handler = create_handler()
    validation_steps = mock_validation_steps()
    with mock.patch('intranet.webauth.lib.role_cache.check_role_cache') as check_role_cache:
        check_role_cache.side_effect = mocked_check_role_cache
        yield handler.get()
    assert validation_steps == ['cert', 'token', 'cookies']
    assert handler.test_result is True


@pytest.mark.gen_test
def test_all_required_success(create_handler, mock_validation_steps):
    handler = create_handler({'required': ['cert,cookies,token']})
    validation_steps = mock_validation_steps()
    yield handler.get()
    assert validation_steps == ['cert', 'cookies', 'token']
    assert handler.test_result is True


@pytest.mark.parametrize('step', ['cert', 'token', 'cookies'])
@pytest.mark.parametrize('critical', [True, False])
@pytest.mark.gen_test
def test_all_required_one_fail(create_handler, mock_validation_steps, step, critical):
    handler = create_handler({'required': ['%s,cert,token,token,cookies' % step]})
    validation_steps = mock_validation_steps({step: False})
    yield handler.get()
    assert validation_steps == [step, 'cert', 'token', 'token', 'cookies']
    assert handler.test_result is False


@pytest.mark.gen_test
def test_all_optional_no_success(create_handler, mock_validation_steps):
    handler = create_handler({'optional': ['token,cert,cookies']})
    validation_steps = mock_validation_steps({'cert': None, 'cookies': None, 'token': None})
    yield handler.get()
    assert validation_steps == ['token', 'cert', 'cookies']
    assert handler.test_result is False


@pytest.mark.parametrize('step', ['cert', 'token', 'cookies'])
@pytest.mark.parametrize('critical', [True, False])
@pytest.mark.gen_test
def test_all_optional_one_fail(create_handler, mock_validation_steps, step, critical):
    handler = create_handler({'optional': ['cookies,cert,token']})
    validation_steps = mock_validation_steps({step: False if critical else None})
    yield handler.get()
    if critical:
        assert validation_steps == ['cookies', 'cert', 'token']
        assert handler.test_result is False
    else:
        assert validation_steps == ['cookies', 'cert', 'token']
        assert handler.test_result is True


@pytest.mark.parametrize('step', ['cert', 'token', 'cookies'])
@pytest.mark.gen_test
def test_all_optional_one_success(create_handler, mock_validation_steps, step):
    handler = create_handler({'optional': ['cert,token,cookies']})
    steps_results = {'cert': None, 'token': None, 'cookies': None}
    steps_results[step] = True
    validation_steps = mock_validation_steps(steps_results)
    yield handler.get()
    assert validation_steps == ['cert', 'token', 'cookies']
    assert handler.test_result is True


@pytest.mark.parametrize('good,bad', [('cert', 'token'),
                                      ('token', 'cert'),
                                      ('cert', 'cookies'),
                                      ('cookies', 'cert'),
                                      ('token', 'cookies'),
                                      ('cookies', 'token')])
@pytest.mark.gen_test
def test_all_optional_one_success_one_fail(create_handler, mock_validation_steps, good, bad):
    handler = create_handler({'optional': ['cert,token,cookies']})
    steps_results = {'cert': None, 'token': None, 'cookies': None}
    steps_results[good] = True
    steps_results[bad] = False
    validation_steps = mock_validation_steps({good: True, bad: False})
    yield handler.get()
    assert validation_steps == ['cert', 'token', 'cookies']
    assert handler.test_result is False


# getting the list of all possible combinations of parameters
parameters = ['cert', 'token', 'cookies']
queries = list(itertools.chain(*(list(itertools.combinations(parameters, i)) for i in range(1, 2))))


@pytest.mark.parametrize('required', queries)
@pytest.mark.parametrize('optional', queries)
@pytest.mark.gen_test
def test_good_required_good_optional(create_handler, mock_validation_steps, required, optional):
    handler = create_handler({'required': [','.join(required)], 'optional': [','.join(optional)]})
    validation_steps = mock_validation_steps()
    yield handler.get()
    assert tuple(validation_steps) == required + optional
    assert handler.test_result is True


@pytest.mark.parametrize('optional', parameters)
@pytest.mark.parametrize('critical', [True, False])
@pytest.mark.gen_test
def test_two_good_required_one_bad_optional(create_handler, mock_validation_steps, optional, critical):
    required = [x for x in parameters if x != optional]
    handler = create_handler({'required': [','.join(required)], 'optional': [optional]})
    validation_steps = mock_validation_steps({optional: False if critical else None})
    yield handler.get()
    assert validation_steps == required + [optional]
    assert handler.test_result is False


@pytest.mark.parametrize('step', ['cert', 'cookies', 'token'])
@pytest.mark.gen_test
def test_all_required_unequal_logins(create_handler, mock_validation_steps, step):
    handler = create_handler({'required': ['cert,cookies,token']})
    mock_validation_steps = mock_validation_steps(logins={step: 'another'})
    yield handler.get()
    assert mock_validation_steps == ['cert', 'cookies', 'token']
    assert handler.test_result is False


@pytest.mark.parametrize('step', ['cert', 'cookies', 'token'])
@pytest.mark.gen_test
def test_all_optional_unequal_logins(create_handler, mock_validation_steps, step):
    handler = create_handler({'optional': ['cert,cookies,token']})
    mock_validation_steps = mock_validation_steps(logins={step: 'another'})
    yield handler.get()
    assert mock_validation_steps == ['cert', 'cookies', 'token']
    assert handler.test_result is False


@pytest.mark.parametrize('optional', parameters)
@pytest.mark.gen_test
def test_two_good_required_one_bad_optional_with_unequal_login(create_handler, mock_validation_steps, optional):
    required = [x for x in parameters if x != optional]
    handler = create_handler({'required': [','.join(required)], 'optional': [optional]})
    mock_validation_steps = mock_validation_steps(logins={optional: 'another'})
    yield handler.get()
    assert mock_validation_steps == required + [optional]
    assert handler.test_result is False


@gen.coroutine
def mocked_get(key):
    assert key == 'role_cache_version'
    raise gen.Return('1')


@gen.coroutine
def mocked_sismember(set_name, key):
    assert set_name == 'roles/1'
    if key == '{}/dummy/{}'.format(quote('/roles/somerole/', safe=''), quote('[]', safe='')):
        raise gen.Return(True)
    else:
        raise gen.Return(False)


@pytest.mark.parametrize('left_bracket', [False, True])
@pytest.mark.parametrize('right_bracket', [False, True])
@pytest.mark.parametrize('idm_role,is_ok', [(None, True), ('roles/strangerole', False), ('roles/somerole', True)])
@pytest.mark.gen_test
def test_with_idm_role(create_handler, mock_validation_steps, idm_role, is_ok, left_bracket, right_bracket):
    if idm_role is not None:
        if left_bracket:
            idm_role = '/' + idm_role
        if right_bracket:
            idm_role += '/'
    params = {'required': ['token']}
    if idm_role:
        params['idm_role'] = [idm_role]
    handler = create_handler(params)
    validation_steps = mock_validation_steps()
    with mock.patch('intranet.webauth.lib.role_cache._get') as _get, mock.patch('intranet.webauth.lib.role_cache._sismember') as _sismember:
        _get.side_effect = mocked_get
        _sismember.side_effect = mocked_sismember
        yield handler.get()
    assert tuple(validation_steps) == ('token',)
    assert handler.test_result == is_ok

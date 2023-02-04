# -*- coding: utf-8 -*-
import pytest
import mock
import hamcrest as hm

from balance import exc, constants as cst
from balance.corba_buffers import StateBuffer, RequestBuffer
from muzzle.security.blackbox import SecureMode
from tests import object_builder as ob


def patch_mock_bb(mock_bb, oper_id, kwargs=None):
    params = {
        'status': 'VALID',
        'uid': oper_id,
        'error': 'OK',
        'fields': {'display_name': 'Production'},
    }
    if kwargs:
        params.update(kwargs)
    mock_bb.return_value = params


def get_state_obj(**kwargs):
    params = {
        'prot_remote_ip': '95.108.172.0',
        'prot_method': 'get',
        'prot_host': 'balance.yandex.ru',
        'prot_path': 'paypreview.xml',
        'prot_secure': 'yes',
    }
    params.update(kwargs)
    return StateBuffer(params=params)


def get_request_params(in_params=None, in_headers=None, in_cookies=None):
    in_cookies = in_cookies if in_cookies is not None else [
        ('Session_id', str(ob.get_big_number())),
        ('sessionid2', str(ob.get_big_number())),
        ('yandexuid', str(ob.get_big_number())),
    ]
    return RequestBuffer(
        params=[
            in_params or [],  # in_params
            in_headers or [],  # in_headers
            in_cookies,  # in_cookies
        ],
    )


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@mock.patch('butils.check_auth.bbox_client')
class TestAuth(object):
    secure_mode = SecureMode.UNKNOWN
    retpath = u'https%3A%2F%2Fbalance.yandex.rupaypreview.xml'
    retpath_types = {
        'logout': u'https://passport.yandex.ru/passport?mode=logout&yu={yu}&retpath={retpath}',
        'auth': u'https://passport.yandex.ru/auth?from=balance&retpath={retpath}',
        'update': u'https://passport.yandex.ru/auth/update?retpath={retpath}',
    }

    def _check_no_auth(self, state_obj, req_obj, res, retpath_type, yandexuid=''):
        retpath = self.retpath_types[retpath_type].format(retpath=self.retpath, yu=yandexuid)

        assert res.tag == 'no-auth'
        assert state_obj.changes == {'skip': u'1'}
        assert req_obj.out_status == 302
        hm.assert_that(
            req_obj.out_headers,
            hm.contains(
                hm.contains(u'X-Frame-Options', u'SAMEORIGIN'),
                hm.contains(u'Location', retpath),
            ),
        )

    def test_no_secure(self, mock_bb, session, muzzle_logic):
        with pytest.raises(AssertionError) as exc_info:
            muzzle_logic.check_auth_bb(
                get_state_obj(prot_secure='no'),
                get_request_params(
                    in_cookies=[('Session_id', str(ob.get_big_number()))],
                ),
                self.secure_mode,
            )
        assert exc_info.value.message == 'HTTP is prohibited'

    def test_valid_session_id2(self, mock_bb, session, muzzle_logic):
        patch_mock_bb(mock_bb, session.oper_id)
        res = muzzle_logic.check_auth_bb(
            get_state_obj(),
            get_request_params(
                in_cookies=[('sessionid2', str(ob.get_big_number()))],
            ),
            self.secure_mode,
        )
        assert res.findtext('passport_id') == str(session.oper_id)

    def test_only_w_session_id(self, mock_bb, session, muzzle_logic):
        patch_mock_bb(mock_bb, session.oper_id)

        state_obj = get_state_obj()
        req_obj = get_request_params(
            # Session_id можно только с no_secure, а no_secure запрещен
            in_cookies=[('Session_id', str(ob.get_big_number()))],
        )

        res = muzzle_logic.check_auth_bb(state_obj, req_obj, self.secure_mode)
        self._check_no_auth(state_obj, req_obj, res, 'logout')

    def test_wo_session_cookie(self, mock_bb, session, muzzle_logic):
        patch_mock_bb(mock_bb, session.oper_id)

        state_obj = get_state_obj()
        req_obj = get_request_params(in_cookies=[])

        res = muzzle_logic.check_auth_bb(state_obj, req_obj, self.secure_mode)
        self._check_no_auth(state_obj, req_obj, res, 'auth')

    @pytest.mark.parametrize(
        'mock_params',
        [
            {'status': 'EXPIRED'},
            {'status': 'INVALID', 'error': 'Session_id Invalid format'}
        ],
    )
    def test_expired(self, mock_bb, session, muzzle_logic, mock_params):
        patch_mock_bb(mock_bb, session.oper_id, mock_params)

        state_obj = get_state_obj()
        req_obj = get_request_params()

        res = muzzle_logic.check_auth_bb(state_obj, req_obj, self.secure_mode)
        yandexuid, = [c[1] for c in req_obj.in_cookies if c[0] == 'yandexuid']
        self._check_no_auth(state_obj, req_obj, res, 'logout', yandexuid)

    def test_need_reset(self, mock_bb, session, muzzle_logic):
        patch_mock_bb(mock_bb, session.oper_id, {'status': 'NEED_RESET'})

        state_obj = get_state_obj()
        req_obj = get_request_params()

        res = muzzle_logic.check_auth_bb(state_obj, req_obj, self.secure_mode)
        self._check_no_auth(state_obj, req_obj, res, 'update')

    @pytest.mark.parametrize(
        'mock_params',
        [
            {'status': 'INVALID'},
            {'error': 'Some error'},
            {'uid': None},
        ],
    )
    def test_invalid_resp_from_bb(self, mock_bb, session, muzzle_logic, mock_params):
        patch_mock_bb(mock_bb, session.oper_id, mock_params)

        state_obj = get_state_obj()
        req_obj = get_request_params()

        res = muzzle_logic.check_auth_bb(state_obj, req_obj, self.secure_mode)
        self._check_no_auth(state_obj, req_obj, res, 'auth')

    def test_intranet(self, mock_bb, session, muzzle_logic):
        patch_mock_bb(mock_bb, session.oper_id)
        res = muzzle_logic.check_auth_bb(
            get_state_obj(prot_host='balance.yandex-team.ru'),
            get_request_params(),
            self.secure_mode,
        )
        assert res.findtext('passport_id') == str(session.oper_id)


@mock.patch('muzzle.security.blackbox.check_passport_auth')
@mock.patch('butils.check_auth.bbox_client')
class TestDoSauth(object):
    regions_cfg_name = 'SAUTH_REQUIRED_REGIONS'
    cfg_name = 'SAUTH_BALANCE_MODE'

    default_balance_cfg = {'regions': [], 'users': []}
    default_regions_cfg = 1

    @pytest.fixture(autouse=True)
    def set_default_config(self, session):
        session.config.__dict__[self.cfg_name] = self.default_balance_cfg
        session.config.__dict__[self.regions_cfg_name] = self.default_regions_cfg

    def test_intranet(self, mock_bb, mock_passport, session, muzzle_logic):
        session.config.__dict__[self.cfg_name] = self.default_balance_cfg
        patch_mock_bb(mock_bb, session.oper_id)
        mock_passport.return_value = False

        res = muzzle_logic.check_sauth(
            get_state_obj(prot_host='balance.yandex-team.ru'),
            get_request_params(),
            session.oper_id,
        )
        assert res.tag == 'ok'

    @pytest.mark.parametrize(
        'cfg_value, skip',
        [
            pytest.param([cst.RegionId.ARMENIA], False, id='same region'),
            pytest.param([cst.RegionId.BELARUS], True, id='other region'),
            pytest.param(1, False, id='check all regions'),
            pytest.param(0, True, id='skip checking for everybody'),
        ],
    )
    def test_skip_by_regions(self, mock_bb, mock_passport, session, muzzle_logic, client, cfg_value, skip):
        session.config.__dict__[self.regions_cfg_name] = cfg_value
        client.region_id = cst.RegionId.ARMENIA
        session.passport.client = client
        session.flush()

        patch_mock_bb(mock_bb, session.oper_id)
        mock_passport.return_value = False

        if skip:
            res = muzzle_logic.check_sauth(
                get_state_obj(),
                get_request_params(),
                session.oper_id
            )
            assert res.tag == 'ok'

        else:
            with pytest.raises(exc.AUTHENTICATION_FAILED) as exc_info:
                muzzle_logic.check_sauth(get_state_obj(), get_request_params(), session.oper_id)
            msg = 'Authentication failed, details: no passport secure authentication, domain: yandex.ru, need_reset: 0'
            assert exc_info.value.msg == msg

    @pytest.mark.parametrize(
        'in_config, auth_type',
        [
            pytest.param(True, 'balance', id='balance'),
            pytest.param(False, 'passport', id='passport'),
        ],
    )
    def test_user_list(self, mock_bb, mock_passport, session, muzzle_logic,  in_config, auth_type):
        session.config.__dict__[self.cfg_name] = {'users': [session.oper_id if in_config else 1234]}
        patch_mock_bb(mock_bb, session.oper_id)
        mock_passport.return_value = False

        with pytest.raises(exc.AUTHENTICATION_FAILED) as exc_info:
            muzzle_logic.check_sauth(get_state_obj(), get_request_params(), session.oper_id)
        msg = 'Authentication failed, details: no %s secure authentication, domain: yandex.ru, need_reset: 0' % auth_type
        assert exc_info.value.msg == msg

    @pytest.mark.parametrize(
        'cfg_value, client_region_id, auth_type',
        [
            pytest.param([cst.RegionId.ARMENIA], cst.RegionId.ARMENIA, 'balance', id='client\'s region is in config'),
            pytest.param([cst.RegionId.ARMENIA], cst.RegionId.BELARUS, 'passport', id='client\'s region is not in config'),
            pytest.param([], cst.RegionId.ARMENIA, 'passport', id='config is empty'),
            pytest.param([cst.RegionId.ARMENIA], None, 'passport', id='client_region_id=None'),
        ],
    )
    def test_region_list(self, mock_bb, mock_passport, session, muzzle_logic, client, cfg_value, client_region_id, auth_type):
        session.config.__dict__[self.cfg_name] = {'regions': cfg_value}
        client.region_id = client_region_id
        session.passport.client = client
        session.flush()
        patch_mock_bb(mock_bb, session.oper_id)
        mock_passport.return_value = False

        with pytest.raises(exc.AUTHENTICATION_FAILED) as exc_info:
            muzzle_logic.check_sauth(get_state_obj(), get_request_params(), session.oper_id)
        msg = 'Authentication failed, details: no %s secure authentication, domain: yandex.ru, need_reset: 0' % auth_type
        assert exc_info.value.msg == msg

    @pytest.mark.parametrize(
        'config_val, auth_type',
        [
            pytest.param(0, 'passport', id='passport'),
            pytest.param(1, 'balance', id='balance'),
        ],
    )
    def test_go_balance_sauth(self, mock_bb, mock_passport, session, muzzle_logic, config_val, auth_type):
        session.config.__dict__[self.cfg_name] = config_val
        patch_mock_bb(mock_bb, session.oper_id)
        mock_passport.return_value = False

        with pytest.raises(exc.AUTHENTICATION_FAILED) as exc_info:
            muzzle_logic.check_sauth(get_state_obj(), get_request_params(), session.oper_id)

        msg = 'Authentication failed, details: no %s secure authentication, domain: yandex.ru, need_reset: 0' % auth_type
        assert exc_info.value.msg == msg

    @pytest.mark.parametrize(
        'balance_check',
        [
            pytest.param(True, id='balance'),
            pytest.param(False, id='passport'),
        ],
    )
    @pytest.mark.parametrize(
        'reverse',
        [False, True],
    )
    @mock.patch('muzzle.security.blackbox.check_balance_auth')
    def test_auth_ok(
            self,
            mock_balance,
            mock_bb,
            mock_passport,
            session,
            muzzle_logic,
            balance_check,
            reverse,
    ):
        session.config.__dict__[self.cfg_name] = 1 if balance_check else self.default_balance_cfg
        patch_mock_bb(mock_bb, session.oper_id)

        balance_res, passport_res = balance_check, not balance_check
        if reverse:
            balance_res, passport_res = passport_res, balance_res

        mock_passport.return_value = passport_res
        mock_balance.return_value = balance_res

        if reverse:
            with pytest.raises(exc.AUTHENTICATION_FAILED) as exc_info:
                muzzle_logic.check_sauth(get_state_obj(), get_request_params(), session.oper_id)

            auth_type = 'balance' if balance_check else 'passport'
            msg = 'Authentication failed, details: no %s secure authentication, domain: yandex.ru, need_reset: 0' % auth_type
            assert exc_info.value.msg == msg

        else:
            res = muzzle_logic.check_sauth(
                get_state_obj(),
                get_request_params(),
                session.oper_id
            )
            assert res.tag == 'ok'

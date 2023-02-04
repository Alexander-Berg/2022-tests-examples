# -*- encoding: utf-8 -*-
import json
from copy import deepcopy
from urllib import urlencode

import responses
import typing

from tests import ws_responses
from yb_darkspirit import interface
from yb_darkspirit.interface import CashRegister


class WhiteSpiritMocks(object):
    """
    Contains actual information about all WS active mocks. Useful since :class:`interface.CashRegister` relies on WS mocks.
    Also may be useful to keep consistency between environment state set via WS mocks and state in DB.
    """
    cr_wrappers = None  # type: typing.Dict[str, CashRegister]

    def __init__(self, ws_url, rsps, session, cashmashines_response_kwargs=None):
        """
        :type ws_url: str
        :type rsps: responses.RequestsMock
        :type session: sqlalchemy.Session
        :type cashmashines_response_kwargs: dict
        """
        self.url = ws_url
        self.responses = rsps
        self.mocks = {}

        if cashmashines_response_kwargs:
            self._mock_cashmachines(**cashmashines_response_kwargs)
        self.init_cr_wrappers(session)

    def init_cr_wrappers(self, session):
        self.cr_wrappers = {
            cr_wrapper.long_serial_number: cr_wrapper
            for cr_wrapper in interface.CashRegister.from_whitespirit_url(session, self.url)
        }
        for cr_wrapper in self.cr_wrappers.values():
            cr_wrapper.sync()

    def mock(self, method, uri, reset=False, strict=False, **kwargs):
        response_mock = responses.Response(method=method, url=uri, **kwargs)
        self._add_mock(method, uri, response_mock, reset=reset, strict=strict)
        return response_mock

    def _add_mock(self, method, uri, response_mock, reset=True, strict=False):
        if (method, uri) in self.mocks:
            assert not strict, 'Mock is already set for (method, url) = {}'.format((method, uri))
            if reset:
                self.responses.remove(self.mocks[method, uri])
        self.responses.add(response_mock)
        self.mocks[method, uri] = response_mock
        return response_mock

    def _mock_cashmachines(self, **kwargs):
        resp = self.mock(responses.GET, self.url + "/v1/cashmachines", strict=True, **kwargs)
        return resp

    def _mock_cashmachine_method(self, cr_long_sn, do_sync=True, **kwargs):
        kwargs.setdefault('content_type', "application/json")
        if 'json' not in kwargs:
            kwargs.setdefault('body', '{}')
        resp = self.mock(**kwargs)
        if do_sync:
            self._sync_cr_wrapper(cr_long_sn)
        return resp

    def _sync_cr_wrapper(self, cr_long_sn):
        cr_wrapper = self.cr_wrappers.get(cr_long_sn)
        if cr_wrapper:
            # Invalidate local cache since we changed WS response which is ground-truth for data in CashRegister object
            cr_wrapper.expire_device_info()
            cr_wrapper.sync()

    def cashmachines_status_mock(self, cr_long_sn):
        uri = self.url + "/v1/cashmachines/{}/status".format(cr_long_sn)
        return responses.GET, uri

    def cashmachines_status(self, cr_long_sn, **kwargs):
        method, uri = self.cashmachines_status_mock(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=method, uri=uri, **kwargs)

    def cashmachines_document(self, cr_long_sn, document_id, flags=('with_fullform', 'with_rawform'), **kwargs):
        flags_args = urlencode({flag: True for flag in flags})
        uri = self.url + "/v1/cashmachines/{}/document/{}?{}".format(cr_long_sn, document_id, flags_args)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.GET, uri=uri, **kwargs)

    def cashmachines_configure(self, cr_long_sn, **kwargs):
        def request_callback(request):
            payload = json.loads(request.body)
            cr_status = json.loads(
                self.mocks[self.cashmachines_status_mock(cr_long_sn)].body
            )
            # TODO: use data from POST body of `/configure` request to WS
            cr_status['registration_info'].update(dict(
                # TODO: Check `register_sn` if obsolete
                register_sn="",
                **payload.get('reg_info', {})
            ))
            self.cashmachines_status(
                cr_long_sn=cr_long_sn,
                json=cr_status,
                content_type="application/json",
            )
            return 200, [], '{}'

        uri = self.url + "/v1/cashmachines/{}/configure".format(cr_long_sn)

        mock = responses.CallbackResponse(
            url=uri,
            method=responses.POST,
            callback=request_callback,
            content_type='application/json',
            **kwargs
        )

        self._add_mock(method=responses.POST, uri=uri, response_mock=mock)
        self._sync_cr_wrapper(cr_long_sn)
        return mock

    def cashmachines_register(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/register".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_close_shift(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/close_shift".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_close_fiscal_mode(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/close_fiscal_mode?mysecret=xxx_oke_{}".format(
            cr_long_sn, cr_long_sn[-4:]
        )
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_ident(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/ident".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_ssh_ping(self, cr_long_sn, use_password=False, do_sync=False, **kwargs):
        flags_args = urlencode({"use_password": use_password})
        uri = self.url + "/v1/cashmachines/{}/ssh_ping?{}".format(cr_long_sn, flags_args)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.GET, uri=uri, do_sync=do_sync, **kwargs)

    def cashmachines_space_info(self, cr_long_sn, use_password=False, **kwargs):
        flags_args = urlencode({"use_password": use_password})
        uri = self.url + "/v1/cashmachines/{}/space_info?{}".format(cr_long_sn, flags_args)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.GET, uri=uri, **kwargs)

    def cashmachines_get_password(self, cr_long_sn, use_password=False, **kwargs):
        flags_args = urlencode({"use_password": use_password})
        uri = self.url + "/v1/cashmachines/{}/get_admin_password?{}".format(cr_long_sn, flags_args)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.GET, uri=uri, **kwargs)

    def cashmachines_setup_ssh_connection(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/setup_ssh_connection".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_upgrade(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{cr_long_sn}/upgrade".format(cr_long_sn=cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_clear_device_data(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/clear_device_data".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_reboot(self, cr_long_sn, cold=False, **kwargs):
        flags_args = urlencode({"cold": cold})
        uri = self.url + "/v1/cashmachines/{}/reboot?{}".format(cr_long_sn, flags_args)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_set_datetime(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/set_datetime".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def cashmachines_set_datetime_json(self, cr_long_sn, **kwargs):
        uri = self.url + "/v1/cashmachines/{}/set_datetime_json".format(cr_long_sn)
        return self._mock_cashmachine_method(cr_long_sn=cr_long_sn, method=responses.POST, uri=uri, **kwargs)

    def uploads(self, **kwargs):
        uri = self.url + '/v1/uploads'
        return self.mock(method=responses.GET, uri=uri, **kwargs)

    def post_upload(self, **kwargs):
        uri = self.url + '/upload'
        kw = {'body': json.dumps(ws_responses.WS_POST_UPLOAD)}
        kw.update(kwargs)
        return self.mock(method=responses.POST, uri=uri, **kw)


def set_cr_state(ws_mocks, cr_long_sn, cr_state, fn_state):
    # Берем текущий статус кассы из мока
    cr_status = json.loads(
        ws_mocks.mocks[ws_mocks.cashmachines_status_mock(cr_long_sn)].body
    )
    cr_status.update({
        "state": cr_state,
        "fn_state": fn_state,
    })
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_long_sn,
        json=cr_status,
        reset=True,
        content_type="application/json",
    )


def mock_cr_configure(ws_mocks, cr_long_sn):
    ws_mocks.cashmachines_configure(cr_long_sn=cr_long_sn)
    ws_mocks.cashmachines_reboot(cr_long_sn=cr_long_sn)
    ws_mocks.cashmachines_set_datetime(cr_long_sn=cr_long_sn)


def add_document_to_cr(document_dict, ws_mocks, cr_long_sn):
    cr_wrapper = ws_mocks.cr_wrappers[cr_long_sn]
    # Берем текущий статус кассы из мока
    cr_status = json.loads(
        ws_mocks.mocks[ws_mocks.cashmachines_status_mock(cr_long_sn)].body
    )
    # Обновляем инфу о последнем документе
    cr_status['fn_last_document_info'] = {
        "id": document_dict['id'],
        "dt": document_dict['dt'],
    }
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_long_sn,
        json=cr_status,
        content_type="application/json",
    )
    ws_mocks.cashmachines_document(
        cr_long_sn=cr_long_sn,
        document_id=document_dict['id'],
        json=document_dict,
        content_type="application/json",
    )


def make_document(document_dict, current_last_document_number):
    document_dict = deepcopy(document_dict)
    document_dict['id'] = current_last_document_number + 1
    document_dict["raw_data"] = json.dumps(document_dict)
    return document_dict

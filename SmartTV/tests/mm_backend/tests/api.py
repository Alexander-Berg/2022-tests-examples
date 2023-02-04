import logging
from copy import deepcopy
from typing import List, Dict, Tuple, Optional

import mock
import pytest
from alice.megamind.protos.scenarios.request_pb2 import TScenarioRunRequest
from alice.megamind.protos.scenarios.response_pb2 import TScenarioRunResponse
from django.test import Client
from google.protobuf import json_format

from smarttv.alice.mm_backend.channels.vh_content_scheme import switch_channel_uri as vh_content_uri
from smarttv.alice.mm_backend.common.constants import alice_tv_app_id
from smarttv.alice.mm_backend.common.livetv_url_scheme import switch_channel_uri, switch_input_uri
from smarttv.alice.mm_backend.inputs.constants import specifying_slot_name
from smarttv.alice.mm_backend.tests.mock import VhApiEmptyResponseMock, VhApiMock

logger = logging.getLogger(__name__)


class MmReqData:
    def __init__(self, slots: List[Tuple[str, str]], ds_data: Dict, app_id: Optional):
        self.slots = slots
        self.ds_data = ds_data
        self.app_id = app_id


client = Client(HTTP_ACCEPT='application/protobuf')

russia_1 = {
    'id': '2',
    'number': 2,
    'name': '2 Россия-1'
}

first_tv = {
    'id': '1',
    'number': 1,
    'name': '1 Первый канал'
}

hobbies_world = {
    'id': '50',
    'number': 50,
    'name': '50 Мир Увлечений'
}


def make_input(name, id, device=None):
    return {
        'id': str(id),
        'name': name,
        'custom_name': None,
        'device_name': device
    }


# alias
m_i = make_input

mm_request_template = {
    'base_request': {
        'request_id': '36b60f00-c135-4227-8cf8-386d7d989237',
        'dialog_id': None,
        'server_time_ms': 1566217778,
        'random_seed': 42,
        'client_info': {
            'app_id': alice_tv_app_id,
            'app_version': '4.0',
            'os_version': '9',
            'platform': 'android',
            'uuid': 'a1fdd3f3-d696-4ff0-bc05-ac3379a56174',
            'device_id': 'a1fdd3f3-d696-4ff0-bc05-ac3379a56174',
            'lang': 'ru-RU',
            'client_time': '20190819T152916',
            'timezone': 'Europe/Moscow',
            'timestamp': '1566217756',
            'device_model': 'Pixel 2 XL',
            'device_manufacturer': 'google'
        },
        'location': {
            'lat': 55.7364953,
            'lon': 37.6404265,
            'recency': 23450,
            'accuracy': 24.21999931
        },
        'interfaces': {
            'has_screen': True
        },
        'device_state': {
            'sound_level': 0,
            'sound_muted': False,
            'is_tv_plugged_in': False,
            'music': None,
            'video': None,
            'alarms_state': '',
            'device_config': None,
            'tv_set': {
                'inputs': [],
                "channels": []
            }
        },
        'experiments': {
        },
        'state': {}
    },
    'input': {
        'semantic_frames': [
            {
                'name': 'alice.switch_tv_input',
                'slots': [
                    {
                        'name': 'source_id',
                        'value': 'hdmi'
                    }
                ]
            }
        ],
        'text': {}
    },
    'data_sources': {}
}


class MmBackend:
    endpoint = None
    tv_set_section = None
    intent_name = None

    def get_input_frames(self, slots: List[Tuple[str, str]]):
        return [
            {
                'name': self.intent_name,
                'slots': [{'name': s[0], 'value': s[1]} for s in slots]
            }
        ]

    def patch_device_state(self, mm_js, data):
        mm_js['base_request']['device_state']['tv_set'][self.tv_set_section].append(data)

    def checks(self, response, directives_count, uri=None):
        directives = response['response_body']['layout'].get('directives')
        if directives_count == 0:
            assert directives is None
        elif directives_count > 0:
            assert isinstance(directives, list), 'match was unsuccessful'
            assert len(directives) == directives_count
            assert 'open_uri_directive' in directives[0]
            assert directives[0]['open_uri_directive']['uri'] == uri

    def make_request(self, test_data: MmReqData):
        mm_req = deepcopy(mm_request_template)
        mm_req['base_request']['client_info']['app_id'] = test_data.app_id or alice_tv_app_id
        mm_req['input']['semantic_frames'] = self.get_input_frames(test_data.slots)
        self.patch_device_state(mm_req, test_data.ds_data)
        return mm_req

    def post_request(self, json_req):
        msg = json_format.ParseDict(json_req, TScenarioRunRequest(), False).SerializeToString()
        response = client.post(path=self.endpoint, data=msg, content_type='application/protobuf')
        assert response.status_code == 200
        return json_format.MessageToDict(TScenarioRunResponse.FromString(response.content))

    def get_response(self, test_data):
        return self.post_request(self.make_request(test_data))

    def check_is_irrelevant(self, response):
        assert response['features']['is_irrelevant']


class TestChannels(MmBackend):
    endpoint = '/alice/channels/run/'
    authority = 'realtek.media.tv'
    input_type = 'channel'
    tv_set_section = 'channels'
    intent_name = 'alice.switch_tv_channel'

    def make_test_data(self, user_req, channels, app_id=None):
        return MmReqData(
            slots=[('channel', user_req)],
            ds_data={
                'authority': self.authority,
                'input_type': self.input_type,
                'list': channels
            },
            app_id=app_id
        )

    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiEmptyResponseMock)
    @pytest.mark.parametrize('device_channels', [[first_tv], [first_tv, russia_1]])
    def test_found(self, device_channels):
        response = self.get_response(self.make_test_data('первый', device_channels))
        uri = switch_channel_uri(self.authority, self.input_type, first_tv['id'])
        self.checks(response, 1, uri)

    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiEmptyResponseMock)
    @pytest.mark.parametrize('user_req', ['нтв', 'спас', 'твц'])
    def test_not_found(self, user_req):
        response = self.get_response(self.make_test_data(user_req, [first_tv]))
        self.checks(response, 0)

    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiEmptyResponseMock)
    def test_empty_channels_set(self):
        response = self.get_response(self.make_test_data('нтв', []))
        assert response['features']['is_irrelevant']

    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiMock)
    def test_efir_channels(self):
        channel = VhApiMock.channels[0]
        user_req, id = channel

        response = self.get_response(self.make_test_data(user_req.lower(), [first_tv, russia_1]))
        uri = vh_content_uri(id)
        self.checks(response, 1, uri)

    @pytest.mark.skip('use fuzzy search, synonyms and morphology in match')
    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiEmptyResponseMock)
    def test_search_morphology(self):
        response = self.get_response(self.make_test_data('увлечения', [hobbies_world]))
        uri = switch_channel_uri(self.authority, self.input_type, hobbies_world['id'])
        self.checks(response, 1, uri)

    @mock.patch('smarttv.alice.mm_backend.channels.channel.vh_api', VhApiEmptyResponseMock)
    def test_incompatible_app_id(self):
        response = self.get_response(self.make_test_data('первый', [first_tv], app_id='ru.yandex.quasar'))
        self.check_is_irrelevant(response)


class TestInputs(MmBackend):
    endpoint = '/alice/inputs/run/'
    authority = 'realtek.media.tv'
    input_type = 'passthrough'
    input_service = 'com.realtek.tv.hdmitvinput/HDMITvInputService'
    tv_set_section = 'inputs'
    intent_name = 'alice.switch_tv_input'

    def make_ds_data(self, inputs, authority=None):
        return {
            'authority': authority or self.authority,
            'input_type': self.input_type,
            'input_service': self.input_service,
            'list': inputs
        }

    def make_test_data(self, user_req, inputs, extra_slot: Tuple[str, str] = None):
        slots = [('source_id', user_req)]
        if extra_slot is not None:
            slots.append(extra_slot)

        return MmReqData(
            slots=slots,
            ds_data=self.make_ds_data(inputs),
            app_id=None
        )

    def uri(self, id, authority=None, input_type=None, input_service=None):
        return switch_input_uri(
            authority or self.authority,
            input_type or self.input_type,
            input_service or self.input_service,
            id
        )

    @pytest.mark.parametrize('inputs', [[m_i('HDMI', 1), m_i('VGA', 2)], [m_i('HDMI1', 1), m_i('USB', 2)]])
    def test_by_name_ok(self, inputs):
        response = self.get_response(self.make_test_data('hdmi', inputs))
        self.checks(response, 1, self.uri(1))

    def test_by_name_ok_2_input_sections(self):
        vga_id = 3
        test_authority = 'test_authority'
        inputs1 = [m_i('HDMI1', 1), m_i('HDMI2', 2)]
        inputs2 = [m_i('VGA', vga_id), m_i('USB', 4)]

        mm_req = self.make_request(self.make_test_data('vga', inputs1))
        self.patch_device_state(mm_req, self.make_ds_data(inputs2, test_authority))
        response = self.post_request(mm_req)
        self.checks(response, 1, self.uri(vga_id, authority=test_authority))

    def test_by_name_failed(self):
        response = self.get_response(self.make_test_data('usb', [m_i('HDMI1', 1), m_i('VGA', 2)]))
        self.checks(response, 0)

    def test_by_device_name_ok(self):
        response = self.get_response(self.make_test_data('mibox', [m_i('VGA', 1), m_i('HDMI', 2, 'MIBOX3')]))
        self.checks(response, 1, self.uri(2))

    def test_by_device_name_failed(self):
        response = self.get_response(self.make_test_data('xbox', [m_i('VGA', 1), m_i('HDMI', 2, 'MIBOX3')]))
        self.checks(response, 0)

    def test_empty_input_set(self):
        response = self.get_response(self.make_test_data('vga', []))
        assert response['features']['is_irrelevant']

    def test_2_steps(self):
        response = self.get_response(self.make_test_data('hdmi', [m_i('HDMI1', 1), m_i('HDMI2', 2)]))
        frame = response['response_body']['semantic_frame']
        slots = frame['slots']
        assert len(slots) == 1
        assert slots[0]['name'] == specifying_slot_name

        specifying_slot_values = response['response_body']['entities'][0]['items'].keys()
        assert len(specifying_slot_values) == 2
        assert '1' in specifying_slot_values

        response = self.get_response(
            self.make_test_data(
                'hdmi',
                [m_i('HDMI1', 1), m_i('HDMI2', 2)],
                extra_slot=(specifying_slot_name, '1')
            )
        )
        self.checks(response, 1, self.uri(1))

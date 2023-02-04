from mock import Mock

from intranet.vconf.src.ext_api.cucm import CUCMClient

cucm_response = {
    'SelectCmDeviceResult': {
        'TotalDevicesFound': 2,
        'CmNodes': {
            'item': [
                {
                    'ReturnCode': 'Ok',
                    'CmDevices': {
                        'item': [
                            {
                                'Model': 123,
                                'LinesStatus': {
                                    'item': [
                                        {
                                            'DirectoryNumber': '18230',
                                            'Status': 'Registered',
                                        },
                                    ],
                                },
                                'IPAddress': {
                                    'item': [
                                        {
                                            'IP': '172.27.76.67',
                                            'IPAddrType': 'ipv4',
                                            'Attribute': 'Unknown',
                                        },
                                        {
                                            'IP': '2a02:06b8:0000:3203:0277:8dff:fe90:a2bd',
                                            'IPAddrType': 'ipv6',
                                            'Attribute': 'Unknown',
                                        },
                                    ],
                                },
                            },
                            {
                                'Model': 567,
                                'LinesStatus': {
                                    'item': [
                                        {
                                            'DirectoryNumber': '18231',
                                            'Status': 'Registered',
                                        },
                                    ],
                                },
                                'IPAddress': {
                                    'item': [
                                        {
                                            'IP': '172.29.8.69',
                                            'IPAddrType': 'ipv4',
                                            'Attribute': 'Unknown',
                                        },
                                        {
                                            'IP': '2a02:06b8:0000:6003:6e31:0eff:fe26:ccf4',
                                            'IPAddrType': 'ipv6',
                                            'Attribute': 'Unknown',
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                },
                {
                    'ReturnCode': 'NotFound',
                    'Name': 'cipt-cucm3.yndx.net',
                    'NoChange': False,
                    'CmDevices': {
                        'item': [],
                    },
                },
            ],
        },
    },
}


class MockedCUCMClient:

    def __init__(self, *args, **kwargs):
        service_mock = Mock()
        service_mock.selectCmDeviceExt = Mock(return_value=cucm_response)
        self.service = service_mock


def test_get_codec_number_to_ipv6_map():
    expected_result = {
        '18230': {
            'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a2bd]',
            'model': 123,
        },
        '18231': {
            'ip': '[2a02:6b8:0:6003:6e31:eff:fe26:ccf4]',
            'model': 567,
        },
    }
    actual_result = CUCMClient.get_codec_number_to_ipv6_and_model_map(MockedCUCMClient(), ['18230', '18231', '18232'])
    assert actual_result == expected_result

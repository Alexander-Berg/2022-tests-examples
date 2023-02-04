import json
import pytest

from intranet.crt.constants import TAG_FILTER_TYPE
from intranet.crt.tags.models import TagFilter
from __tests__.utils.common import (
    create_certificate,
    create_certificate_tag,
    create_tag_filter,
    make_tag_role_spec,
    make_tagfilter_role_spec,
)


pytestmark = pytest.mark.django_db


def test_idm_hooks_affects_filter_user_set(crt_client, users, idm_tag_filters):
    vpn_filter = idm_tag_filters['Office.VPN']
    bubblegum, jake = users['bubblegum'], users['jake']

    assert vpn_filter.users.count() == 0

    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tagfilter_role_spec(bubblegum.username, vpn_filter.name)
    )
    assert vpn_filter.users.count() == 1
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tagfilter_role_spec(jake.username, vpn_filter.name)
    )
    assert vpn_filter.users.count() == 2
    crt_client.post(
        '/idm/tags/remove-role/',
        data=make_tagfilter_role_spec(jake.username, vpn_filter.name)
    )
    assert vpn_filter.users.get() == bubblegum


def test_idm_hooks_affects_user_certificate(crt_client, users, certificate_types, tags):
    finn, jake = users['finn'], users['jake']
    pc_tag = tags['pc_tag']

    finn_cert_1 = create_certificate(finn, certificate_types['pc'], serial_number='F1')
    finn_cert_2 = create_certificate(finn, certificate_types['pc'], serial_number='F2')
    jake_cert_1 = create_certificate(jake, certificate_types['pc'], serial_number='D1')
    jake_cert_2 = create_certificate(jake, certificate_types['pc'], serial_number='D2')

    for cert in [finn_cert_1, finn_cert_2, jake_cert_1, jake_cert_1]:
        assert cert.tags.count() == 0

    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(finn.username, pc_tag.name, 'F1'),
    )
    assert finn_cert_1.tags.count() == 1
    assert finn_cert_2.tags.count() == 0
    assert jake_cert_1.tags.count() == 0
    assert jake_cert_2.tags.count() == 0

    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(jake.username, pc_tag.name, 'D1'),
    )
    assert finn_cert_1.tags.count() == 1
    assert finn_cert_2.tags.count() == 0
    assert jake_cert_1.tags.count() == 1
    assert jake_cert_2.tags.count() == 0
    crt_client.post(
        '/idm/tags/remove-role/',
        data=make_tag_role_spec(jake.username, pc_tag.name, 'D1'),
    )
    assert finn_cert_1.tags.count() == 1
    assert finn_cert_2.tags.count() == 0
    assert jake_cert_1.tags.count() == 0
    assert jake_cert_2.tags.count() == 0


def test_idm_tags_hooks_errors(crt_client, users, certificate_types, tags):
    finn = users['finn']
    pc_tag = tags['pc_tag']
    cert = create_certificate(finn, certificate_types['pc'], serial_number='F1')
    assert cert.tags.count() == 0

    for path in ['/idm/tags/add-role/', '/idm/tags/remove-role/']:
        # Отсутствует serial_number
        for fields in [{'serial_number': ''}, {'serial_number': None}, {}]:
            data = make_tag_role_spec(finn.username, pc_tag.name, '')
            data['fields'] = json.dumps(fields)
            response = crt_client.post(path, data=data).json()

            assert response['code'] == 500
            assert response['error'] == 'Serial number is not specified'

        # Несуществующий serial_number
        response = crt_client.post(path, data=make_tag_role_spec(finn.username, pc_tag.name, 'F234')).json()
        assert response['code'] == 500
        assert response['error'] == 'User\'s \'finn\' certificate with serial number \'F234\' does not exists'

    # Правильный запрос
    assert cert.tags.count() == 0
    response = crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(finn.username, pc_tag.name, 'F1'),
    ).json()
    assert response['code'] == 0
    assert response['data'] == {'serial_number': 'F1'}
    assert cert.tags.count() == 1

    # Роль уже есть
    response = crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(finn.username, pc_tag.name, 'F1'),
    ).json()
    assert response['code'] == 0
    assert response['data'] == {'serial_number': 'F1'}
    assert cert.tags.count() == 1

    # Правильный запрос на удаление
    response = crt_client.post(
        '/idm/tags/remove-role/',
        data=make_tag_role_spec(finn.username, pc_tag.name, 'F1'),
    ).json()
    assert response['code'] == 0

    # Роль уже удалена
    response = crt_client.post(
        '/idm/tags/remove-role/',
        data=make_tag_role_spec(finn.username, pc_tag.name, 'F1'),
    ).json()
    assert response['code'] == 0


def test_idm_info(crt_client, idm_tag_filters):
    response = crt_client.get('/idm/tags/info/')
    assert response.json() == {
        'code': 0,
        'roles': {
            'name': 'Тип',
            'slug': 'type',
            'values': {
                'tag': {
                    'name': 'Тег по серийному номеру',
                    'roles': {
                        'fields': [
                            {
                                'name': 'Серийный номер',
                                'required': True,
                                'slug': 'serial_number',
                            }
                        ],
                        'name': 'Тег',
                        'slug': 'tag',
                        'values': {},
                    }
                },
                'tagfilter': {
                    'name': 'Тегфильтр',
                    'roles': {
                        'name': 'Тегфильтр',
                        'slug': 'tagfilter',
                        'values': {
                            'Office.8021X.Staff': {
                                'name': 'Office.8021X.Staff',
                                'unique_id': 'tf_Office.8021X.Staff',
                            },
                            'Office.VPN': {
                                'name': 'Office.VPN',
                                'unique_id': 'tf_Office.VPN',
                            },
                            'Office.WiFi.Yandex': {
                                'name': 'Office.WiFi.Yandex',
                                'unique_id': 'tf_Office.WiFi.Yandex',
                            },
                        }
                    }
                }
            }
        }
    }

    TagFilter.objects.filter(name='Office.8021X.Staff').delete()
    create_tag_filter('Office.8021X.Developers', type=TAG_FILTER_TYPE.IDM_SYSTEM)
    create_certificate_tag('Office.8021X.Fin')

    response = crt_client.get('/idm/tags/info/')
    assert response.json() == {
        'code': 0,
        'roles': {
            'name': 'Тип',
            'slug': 'type',
            'values': {
                'tag': {
                    'name': 'Тег по серийному номеру',
                    'roles': {
                        'fields': [
                            {
                                'name': 'Серийный номер',
                                'required': True,
                                'slug': 'serial_number',
                            }
                        ],
                        'name': 'Тег',
                        'slug': 'tag',
                        'values': {
                            'Office.8021X.Fin': {
                                'name': 'Office.8021X.Fin',
                                'unique_id': 'tag_Office.8021X.Fin',
                            },
                        },
                    }
                },
                'tagfilter': {
                    'name': 'Тегфильтр',
                    'roles': {
                        'name': 'Тегфильтр',
                        'slug': 'tagfilter',
                        'values': {
                            'Office.VPN': {
                                'name': 'Office.VPN',
                                'unique_id': 'tf_Office.VPN',
                            },
                            'Office.WiFi.Yandex': {
                                'name': 'Office.WiFi.Yandex',
                                'unique_id': 'tf_Office.WiFi.Yandex',
                            },
                            'Office.8021X.Developers': {
                                'name': 'Office.8021X.Developers',
                                'unique_id': 'tf_Office.8021X.Developers',
                            }
                        }
                    }
                }
            }
        }
    }


def test_idm_get_all_roles(crt_client, users, idm_tag_filters, tags, certificate_types):
    vpn_filter, wifi_filter = idm_tag_filters['Office.VPN'], idm_tag_filters['Office.WiFi.Yandex']
    pc_tag, mobile_tag = tags['pc_tag'], tags['mobile_tag']
    bubblegum, jake = users['bubblegum'], users['jake']

    create_certificate(bubblegum, certificate_types['pc'], serial_number='B1')
    create_certificate(bubblegum, certificate_types['pc'], serial_number='B2')
    create_certificate(jake, certificate_types['pc'], serial_number='D1')
    create_certificate(jake, certificate_types['pc'], serial_number='D2')

    response = crt_client.get('/idm/tags/get-all-roles/')
    assert response.json() == {
        'code': 0,
        'users': []
    }

    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tagfilter_role_spec(bubblegum.username, vpn_filter.name)
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tagfilter_role_spec(jake.username, vpn_filter.name)
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tagfilter_role_spec(jake.username, wifi_filter.name)
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(bubblegum.username, pc_tag.name, 'B1'),
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(bubblegum.username, mobile_tag.name, 'B1'),
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(bubblegum.username, pc_tag.name, 'B2'),
    )
    crt_client.post(
        '/idm/tags/add-role/',
        data=make_tag_role_spec(jake.username, pc_tag.name, 'D1'),
    )
    response = crt_client.get('/idm/tags/get-all-roles/')
    assert response.json() == {
        'code': 0,
        'users': [
            {
                'login': 'bubblegum',
                'roles': [
                    {'tagfilter': 'Office.VPN', 'type': 'tagfilter'},
                    [{'tag': 'mobile_tag', 'type': 'tag'}, {'serial_number': 'B1'}],
                    [{'tag': 'pc_tag', 'type': 'tag'}, {'serial_number': 'B1'}],
                    [{'tag': 'pc_tag', 'type': 'tag'}, {'serial_number': 'B2'}]
                ]
            },
            {
                'login': 'jake',
                'roles': [
                    {'tagfilter': 'Office.VPN', 'type': 'tagfilter'},
                    {'tagfilter': 'Office.WiFi.Yandex', 'type': 'tagfilter'},
                    [{'tag': 'pc_tag', 'type': 'tag'}, {'serial_number': 'D1'}],
                ]
            }
        ]
    }

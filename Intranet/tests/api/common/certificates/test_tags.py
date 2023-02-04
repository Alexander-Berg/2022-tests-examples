import pytest
from rest_framework import status

from intranet.crt.actions.models import Action, ACTION_TYPE
from intranet.crt.constants import TAG_SOURCE
from intranet.crt.tags.models import CertificateTagRelation
from __tests__.utils.common import create_certificate, create_certificate_tag

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('api', ['v1', 'v2'])
def test_invalid_cert_type(crt_client, users, api, certificate_types):
    normal_user = users['normal_user']
    normal_user.is_superuser = True
    normal_user.save()
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['host'])
    tag1 = create_certificate_tag('tag1')

    if api == 'v1':
        url = '/api/certificate/{}/'.format(cert.pk)
        response = crt_client.json.post(url, data={'action': 'update', 'manual_tags': ['tag1']})
    else:
        url = '/api/v2/certificate/{}/'.format(cert.pk)
        response = crt_client.json.patch(url, data={'manual_tags': ['tag1']})
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == ['Tags cannot be edited for certificates of type "host"']


@pytest.mark.parametrize('api', ['v1', 'v2'])
def test_invalid_tag(crt_client, users, api, certificate_types):
    normal_user = users['normal_user']
    normal_user.is_superuser = True
    normal_user.save()
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['host'])
    tag1 = create_certificate_tag('tag1')

    if api == 'v1':
        url = '/api/certificate/{}/'.format(cert.pk)
        response = crt_client.json.post(url, data={'action': 'update', 'manual_tags': ['tag2']})
    else:
        url = '/api/v2/certificate/{}/'.format(cert.pk)
        response = crt_client.json.patch(url, data={'manual_tags': ['tag2']})
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'manual_tags': ['Select a valid choice. tag2 is not one of the available choices.']}



@pytest.mark.parametrize('api', ['v1', 'v2'])
@pytest.mark.parametrize('append_only', [None, False, True])
def test_get_and_update_certificate_tags(crt_client, users, api, certificate_types, append_only):
    normal_user = users['normal_user']
    normal_user.is_superuser = True
    normal_user.save()
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['pc'])
    tag0 = create_certificate_tag('tag0')
    tag0.is_active = False
    tag0.save()
    tag1 = create_certificate_tag('tag1')
    tag2 = create_certificate_tag('tag2')
    tag3 = create_certificate_tag('tag3')
    tag4 = create_certificate_tag('tag4')
    tag5 = create_certificate_tag('tag5')
    tag6 = create_certificate_tag('tag6')
    CertificateTagRelation.objects.create(certificate=cert, tag=tag0, source=TAG_SOURCE.MANUAL)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag1, source=TAG_SOURCE.CERT_TYPE)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag2, source=TAG_SOURCE.MANUAL)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag3, source=TAG_SOURCE.CERT_TYPE)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag3, source=TAG_SOURCE.MANUAL)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag4, source=TAG_SOURCE.CERT_TYPE)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag4, source=TAG_SOURCE.MANUAL)
    CertificateTagRelation.objects.create(certificate=cert, tag=tag5, source=TAG_SOURCE.MANUAL)

    Action.objects.all().delete()

    if api == 'v1':
        url = '/api/certificate/{}/'.format(cert.pk)
    else:
        url = '/api/v2/certificate/{}/'.format(cert.pk)

    response = crt_client.json.get(url)

    assert response.status_code == status.HTTP_200_OK

    cert_description = response.json()
    sorting_key = lambda x: (x['name'], x['source'])
    assert sorted(cert_description['tags'], key=sorting_key) == sorted([
        {
            'name': 'tag0',
            'is_active': False,
            'source': 'manual',
        },
        {
            'name': 'tag1',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag2',
            'is_active': True,
            'source': 'manual',
        },
        {
            'name': 'tag3',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag3',
            'is_active': True,
            'source': 'manual',
        },
        {
            'name': 'tag4',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag4',
            'is_active': True,
            'source': 'manual',
        },
        {
            'name': 'tag5',
            'is_active': True,
            'source': 'manual',
        },
    ], key=sorting_key)
    assert cert_description['manual_tags'] == ['tag0', 'tag2', 'tag3', 'tag4', 'tag5']

    data = {}
    if append_only is not None:
        data['append_tags_only'] = append_only
    if api == 'v1':
        data.update({'action': 'update', 'manual_tags': ['tag0', 'tag4', 'tag5', 'tag6']})
        response = crt_client.json.post(url, data=data)
    else:
        data.update({'manual_tags': ['tag0', 'tag4', 'tag5', 'tag6']})
        response = crt_client.json.patch(url, data=data)
    assert response.status_code == status.HTTP_200_OK

    response = crt_client.json.get(url)
    assert response.status_code == status.HTTP_200_OK
    cert_description = response.json()
    new_tags = [
        {
            'name': 'tag0',
            'is_active': False,
            'source': 'manual',
        },
        {
            'name': 'tag1',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag3',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag4',
            'is_active': True,
            'source': 'cert_type',
        },
        {
            'name': 'tag4',
            'is_active': True,
            'source': 'manual',
        },
        {
            'name': 'tag5',
            'is_active': True,
            'source': 'manual',
        },
        {
            'name': 'tag6',
            'is_active': True,
            'source': 'manual',
        },
    ]
    new_manual_tags = ['tag0', 'tag4', 'tag5', 'tag6']
    if append_only:
        new_tags.extend([
            {
                'name': 'tag2',
                'is_active': True,
                'source': 'manual',
            },
            {
                'name': 'tag3',
                'is_active': True,
                'source': 'manual',
            },
        ])
        new_manual_tags.extend(['tag2', 'tag3'])

    assert sorted(cert_description['tags'], key=sorting_key) == sorted(new_tags, key=sorting_key)
    assert cert_description['manual_tags'] == sorted(new_manual_tags)
    for tag in [tag2, tag3]:
        to_remove = 0 if append_only else 1
        assert Action.objects.filter(certificate=cert, tag=tag, type=ACTION_TYPE.CERT_REMOVE_TAG).count() == to_remove
    assert Action.objects.filter(certificate=cert, type=ACTION_TYPE.CERT_REMOVE_TAG).count() == (0 if append_only else 2)
    assert Action.objects.filter(certificate=cert, tag=tag6, type=ACTION_TYPE.CERT_ADD_TAG).count() == 1
    assert Action.objects.filter(certificate=cert, type=ACTION_TYPE.CERT_ADD_TAG).count() == 1

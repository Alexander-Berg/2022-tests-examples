import pytest

from intranet.crt.constants import CERT_TYPE, TAG_SOURCE, CERT_STATUS
from intranet.crt.core.models import Certificate
from intranet.crt.tags.tasks.sync_cert_type_tags import SyncCertTypeTagsTask
from intranet.crt.tags.tasks.clean_tags import CleanTagsTask
from intranet.crt.tags.models import CertificateTagRelation
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db

filters_users = {
    'princesses': [
        'bubblegum',
        'flame',
        'lumpy_space',
        'ghost',
        'hot_dog',
    ],
    'tree_fort': [
        'finn',
        'jake',
        'bmo',
        'shelby',
    ],
    'candy_people': [
        'bubblegum',
        'lemongrab',
        'banana_guard',
        'cinnamon_bun',
    ],
    'main_characters': [
        'bubblegum',
        'finn',
        'jake',
    ],
}

filters_tags = {
    'princesses': [
        'candy',
        'all_access',
        'ice_king_protect',
        'evil_protect',
    ],
    'tree_fort': [
        'gold',
        'all_access',
        'power',
    ],
    'candy_people': [
        'candy',
        'evil_protect',
    ],
    'main_characters': [
        'all_access',
        'power',
    ],
}

tag_filter_cert_types = {
    'candy': [
        CERT_TYPE.PC,
        CERT_TYPE.RC_SERVER,
        CERT_TYPE.CLIENT_SERVER,
        CERT_TYPE.SDC,
    ],
    'all_access': [
        CERT_TYPE.PC,
        CERT_TYPE.LINUX_PC,
        CERT_TYPE.MOBVPN,
    ],
    'ice_king_protect': [
        CERT_TYPE.PC,
        CERT_TYPE.ASSESSOR,
    ],
    'evil_protect': [
        CERT_TYPE.PC,
        CERT_TYPE.NINJA,
        CERT_TYPE.NINJA_EXCHANGE,
        CERT_TYPE.IMDM,
        CERT_TYPE.ASSESSOR,
    ],
    'gold': [
        CERT_TYPE.PC,
        CERT_TYPE.IMDM,
        CERT_TYPE.HOST,
        CERT_TYPE.WIN_PC_AUTO,

    ],
    'power': [
        CERT_TYPE.PC,
        CERT_TYPE.HOST,
        CERT_TYPE.RC_SERVER,
    ],
}

cert_type_tags = {
    'evil_protect': [
        CERT_TYPE.PC,
        CERT_TYPE.HOST,
    ],
    'candy': [
        CERT_TYPE.VPN_TOKEN,
    ],
}


@pytest.fixture
def tag_ecosystem(certificate_types, tag_filters, tags, users):
    for filter_name, user_names in list(filters_users.items()):
        tag_filter = tag_filters[filter_name]
        for username in user_names:
            user = users[username]
            tag_filter.users.add(user)

    for filter_name, filter_tag_names in list(filters_tags.items()):
        tag_filter = tag_filters[filter_name]
        for tag_name in filter_tag_names:
            tag = tags[tag_name]
            tag_filter.tags.add(tag)

    for tag_name, cert_types_names in list(tag_filter_cert_types.items()):
        tag = tags[tag_name]
        for cert_type_name in cert_types_names:
            cert_type = certificate_types[cert_type_name]
            tag.filter_cert_types.add(cert_type)

    for tag_name, cert_types_names in list(cert_type_tags.items()):
        tag = tags[tag_name]
        for cert_type_name in cert_types_names:
            cert_type = certificate_types[cert_type_name]
            tag.cert_types.add(cert_type)


@pytest.mark.parametrize('data', [
    ('bubblegum', CERT_TYPE.PC, ['candy', 'all_access', 'evil_protect', 'ice_king_protect', 'power']),
    ('finn', CERT_TYPE.HOST, ['evil_protect', 'gold', 'power']),
    ('hot_dog', CERT_TYPE.ASSESSOR, ['evil_protect', 'ice_king_protect']),
    ('lemongrab', CERT_TYPE.WIN_PC_AUTO, []),
    ('jake', CERT_TYPE.VPN_TOKEN, ['candy']),
])
def test_update_tags(certificate_types, users, tag_ecosystem, data):
    username, cert_type, result_tags = data
    cert = create_certificate(users[username], certificate_types[cert_type])
    cert = Certificate.objects.filter(pk=cert.pk).prefetch_tags().get()
    cert_tags = [tag.name for tag in cert.tags.all()]
    assert cert_tags == result_tags


user_certificates = {
    'bubblegum': [
        (CERT_TYPE.VPN_TOKEN, '1', [
            ('evil_protect', TAG_SOURCE.CERT_TYPE),
        ]),
        (CERT_TYPE.PC, '2', [
            ('evil_protect', TAG_SOURCE.FILTERS),
            ('candy', TAG_SOURCE.CERT_TYPE),
        ]),
        (CERT_TYPE.HOST, '3', [
            ('candy', TAG_SOURCE.CERT_TYPE),
            ('candy', TAG_SOURCE.FILTERS),
        ]),
    ]
}


@pytest.fixture
def tagged_certificates(users, certificate_types, tags):
    for username, certs_data in list(user_certificates.items()):
        for cert_type, serial_number, new_tags in certs_data:
            certificate = create_certificate(
                users[username],
                certificate_types[cert_type],
                serial_number=serial_number,
            )
            CertificateTagRelation.objects.filter(certificate=certificate).delete()
            for tag_name, source in new_tags:
                certificate.add_tag(tags[tag_name], source)


def test_sync_cert_type_tags(tag_ecosystem, tagged_certificates, tags):
    synchronizer = SyncCertTypeTagsTask()
    synchronizer.locked_stamped_run()

    certificate = Certificate.objects.get(serial_number='1')
    assert list(certificate.tags.all()) == [tags['candy']]

    certificate = Certificate.objects.get(serial_number='2')
    assert list(certificate.tags.all()) == [tags['evil_protect'], tags['evil_protect']]

    certificate = Certificate.objects.get(serial_number='3')
    assert list(certificate.tags.all()) == [tags['candy'], tags['evil_protect']]


def test_sync_clean_inactive_tags(tag_ecosystem, tagged_certificates, tags):
    tags['candy'].is_active = False
    tags['candy'].save()

    CleanTagsTask().locked_stamped_run()

    certificate = Certificate.objects.get(serial_number='3')
    assert list(certificate.tags.all()) == []

    certificate = Certificate.objects.get(serial_number='2')
    assert list(certificate.tags.all()) == [tags['evil_protect']]


def test_sync_clean_no_issued_cert_tags(tag_ecosystem, tagged_certificates, tags):
    certificate = Certificate.objects.get(serial_number='1')
    certificate.status = CERT_STATUS.HOLD
    certificate.save()

    CleanTagsTask().locked_stamped_run()

    certificate = Certificate.objects.get(serial_number='1')
    assert list(certificate.tags.all()) == []


def test_sync_tags_cert_type_removed(tag_ecosystem, certificate_types, users, tags):
    tag = tags['candy']
    cert = create_certificate(users['bmo'], certificate_types['vpn-token'])
    assert cert.tags.get() == tag

    tag.cert_types.clear()
    SyncCertTypeTagsTask().locked_stamped_run()

    assert not cert.tags.exists()


def test_sync_cert_type_tags_skips_inactive_tags(users, certificate_types, tags):
    pc_type = certificate_types[CERT_TYPE.PC]

    pc_tag = tags['pc_tag']
    pc_tag.cert_types.add(pc_type)

    mobile_tag = tags['mobile_tag']
    mobile_tag.cert_types.add(pc_type)
    mobile_tag.is_active = False
    mobile_tag.save()

    cert = create_certificate(users['normal_user'], pc_type)

    SyncCertTypeTagsTask().locked_stamped_run()

    assert cert.tags.get() == pc_tag

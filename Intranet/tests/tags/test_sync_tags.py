import collections
import constance
import json
import mock
import pytest
import copy

from django.conf import settings
from django.core.management import call_command
from django.test.utils import override_settings
from django.urls import reverse
from django.utils.encoding import force_bytes

from intranet.crt.actions.models import Action
from intranet.crt.constants import ACTION_TYPE, CA_NAME, CERT_TYPE, TAG_SOURCE
from intranet.crt.core.models import CrtUser, Certificate
from intranet.crt.tags.tasks.sync_cvs_tags import compare_cert_sets, JsonDiffEncoder, are_diffs_equal
from intranet.crt.tags.tasks.sync_filters_tags import changes_is_unsafe
from intranet.crt.tags.models import TagFilter, CertificateTagRelation
from intranet.crt.tags.serializers import NocCertSerializer
from intranet.crt.utils.tags import TagsDiffDict
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


def make_tags(*tags):
    return [f'tag{i}' for i in tags]


def make_test_certificate(serial_number, tags, cert_type=CERT_TYPE.PC):
    return {
        'serial_number': serial_number,
        'common_name': 'pc@cert',
        'ca_name': CA_NAME.TEST_CA,
        'type': cert_type,
        **tags
    }


def make_test_certificates(tags_cert_types):
    certificates = []
    for i, (tags, cert_type) in enumerate(tags_cert_types):
        tags_dict = {'tags': make_tags(*tags)}
        cert = make_test_certificate(f'a{i+1}', tags_dict, cert_type)
        certificates.append(cert)
    return {'certificates': certificates}


def get_serial_numbers_from_writer_mock(writer_mock):
    return {cert['serial_number'] for cert in writer_mock.call_args[0][0]['certificates']}


def make_mock_cvs_client():
    mocked_client = mock.Mock()
    mocked_client.up.return_value = None
    mocked_client.get_json_data.return_value = {
        'certificates': [],
        'meta': {'last_sync': 123456789}
    }
    mocked_client.get_old_certs_with_types.return_value = []
    return mocked_client


@pytest.fixture()
def certificates(certificate_types, users, tags):
    tag_user = users['tag_user']
    normal_user = users['normal_user']

    pc_type = certificate_types[CERT_TYPE.PC]
    mobile_type = certificate_types[CERT_TYPE.MOBVPN]
    linux_pc_type = certificate_types[CERT_TYPE.LINUX_PC]

    normal_pc_cert = create_certificate(normal_user, pc_type)
    normal_pc_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.FILTERS)

    return {
        'pc_cert': create_certificate(tag_user, pc_type),
        'mobile_cert': create_certificate(tag_user, mobile_type),
        'linux_pc_cert': create_certificate(tag_user, linux_pc_type),
        'normal_pc_cert': normal_pc_cert,
    }


@pytest.fixture()
def new_noc_data(new_noc_certificates):
    CertificateTagRelation.objects.exclude(source=TAG_SOURCE.MANUAL).delete()
    return NocCertSerializer(instance=new_noc_certificates, many=True).certificates


@pytest.fixture()
def old_noc_data():
    return [
        {
            'serial_number': 'a1',
            'common_name': 'pc@cert',
            'tags': ['tag1'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
        {
            'serial_number': 'a2',
            'common_name': 'pc@cert',
            'tags': ['tag1', 'tag2'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
        {
            'serial_number': 'a3',
            'common_name': 'pc@cert',
            'tags': ['tag1', 'tag2'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
        {
            'serial_number': 'a4',
            'common_name': 'pc@cert',
            'tags': ['tag1', 'tag2', 'tag3'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
        {
            'serial_number': 'a5',
            'common_name': 'pc@cert',
            'tags': ['tag1', 'tag2', 'tag3'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
        {
            'serial_number': 'a6',
            'common_name': 'pc@cert',
            'tags': ['tag1', 'tag2', 'tag3'],
            'ca_name': CA_NAME.TEST_CA,
            'type': CERT_TYPE.PC,
        },
    ]


@pytest.fixture()
def old_noc_data2(new_noc_data):
    data = copy.copy(new_noc_data)
    data.append({
        'serial_number': 'a7',
        'common_name': 'pc@cert',
        'tags': ['tag1', 'tag2', 'tag3'],
        'ca_name': CA_NAME.TEST_CA,
        'type': CERT_TYPE.PC,
    })
    return data


def staff_person_getiter(lookup):
    if 'pc_filter' in list(lookup.keys()):
        return [{'login': 'tag_user'}]
    elif 'mobile_filter' in list(lookup.keys()):
        return [{'login': 'tag_user'}]
    else:
        return []


def test_sync_tags(certificates, tags):
    mock_getiter_path = 'ids.services.staff.repositories.person.StaffPersonRepository.getiter'
    with mock.patch(mock_getiter_path, side_effect=staff_person_getiter):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        SyncFilterTagsTask.locked_stamped_run()

    cert_tags = certificates['pc_cert'].tags.all()
    assert len(cert_tags) == 1
    assert cert_tags[0] == tags['pc_tag']

    cert_tags = certificates['mobile_cert'].tags.all()
    assert len(cert_tags) == 1
    assert cert_tags[0] == tags['mobile_tag']

    cert_tags = certificates['linux_pc_cert'].tags.all()
    assert len(cert_tags) == 0

    cert_tags = certificates['normal_pc_cert'].tags.all()
    assert len(cert_tags) == 0

    filter_users = set()
    for tag_filter in tags['pc_tag'].filters.all():
        filter_users |= {user.username for user in tag_filter.users.all()}
    assert filter_users == {'tag_user'}

    filter_users = set()
    for tag_filter in tags['mobile_tag'].filters.all():
        filter_users |= {user.username for user in tag_filter.users.all()}
    assert filter_users == {'tag_user'}


def test_check_consistency_fail(new_noc_data, old_noc_data):
    new_noc_data = {'certificates': new_noc_data}
    old_noc_data = {'certificates': old_noc_data}
    data_diff = compare_cert_sets(new_noc_data, old_noc_data)
    assert not data_diff.is_safe()
    assert sorted(data_diff.make_error_msg().split(' | ')) == [
        'pc: new=1/1,changed=3/2,removed=3/3',
        'tag_changes: added: (tag3: 3, tag2: 1); removed: (tag2: 1)'
    ]
    assert json.loads(json.dumps(data_diff.tags, cls=JsonDiffEncoder)) == {
        'added': {
            'tag3': 3,
            'tag2': 1,
        },
        'removed': {
            'tag2': 1,
        }
    }

    def assert_certs_are_equal(actual, expected):
        for key in ['serial_number', 'common_name', 'ca_name', 'type']:
            assert actual[key] == expected[key]
        for tag_key in ['tags', 'added_tags', 'removed_tags']:
            if tag_key in expected:
                assert tag_key in actual
                assert set(actual[tag_key]) == set(expected[tag_key])

    assert_certs_are_equal(
        data_diff.certificates['new']['a7'],
        make_test_certificate(
            'a7',
            {'tags': make_tags(1, 2, 3)},
        ),
    )
    for common_name, tags in [
        ('a1', {'added_tags': make_tags(2, 3)}),
        ('a2', {'added_tags': make_tags(3)}),
        ('a3', {
            'added_tags': make_tags(3),
            'removed_tags': make_tags(2)
        }),
    ]:
        assert_certs_are_equal(
            data_diff.certificates['changed'][common_name],
            make_test_certificate(common_name, tags),
        )
    for common_name, tags in [
        ('a4', [1, 2, 3]),
        ('a5', [1, 2, 3]),
        ('a6', [1, 2, 3]),
    ]:
        assert_certs_are_equal(
            data_diff.certificates['removed'][common_name],
            make_test_certificate(
                common_name,
                {'tags': make_tags(*tags)},
            ),
        )


def test_check_consistency_done(new_noc_data, old_noc_data2):
    new_noc_data = {'certificates': new_noc_data}
    old_noc_data2 = {'certificates': old_noc_data2}
    data_diff = compare_cert_sets(new_noc_data, old_noc_data2)
    assert data_diff.is_safe()


def test_remove_users_from_tag_filter(certificates, users):
    mock_getiter_path = 'ids.services.staff.repositories.person.StaffPersonRepository.getiter'
    with mock.patch(mock_getiter_path, side_effect=staff_person_getiter):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        SyncFilterTagsTask.locked_stamped_run()

    Action.objects.all().delete()

    tag_user = users['tag_user']
    cert_tag = certificates['pc_cert'].tags.get()
    tag_filter = cert_tag.filters.get()
    assert tag_filter.users.count() == 1
    assert tag_filter.users.get() == tag_user

    tag_filter.remove_user(tag_user)
    assert tag_filter.users.count() == 0
    assert Action.objects.count() == 1
    action = Action.objects.get()
    assert action.type == 'tag_filter_remove_user'


@pytest.mark.parametrize('test_condition', [
    'ok', 'rewrited', 'too_much_removed', 'too_much_added',
    'too_much_removed_only', 'too_much_added_only', 'too_much_changed',
])
def test_filter_brokes_if_fetch_diff_is_big(crt_client, test_condition):
    # ((Добавлено, Удалено), (Добавлено, ...), ...))
    # Первый кортеж — начальное состояние фильтра
    filter_changes = {
        'ok': ((10, 0), (0, 10)),
        'rewrited': ((50, 0), (50, 50)),
        'too_much_removed': ((20, 0), (5, 15)),
        'too_much_added': ((20, 0), (20, 5)),
        'too_much_removed_only': ((20, 0), (0, 15)),
        'too_much_added_only': ((20, 0), (15, 0)),
        'too_much_changed': ((100, 0), (30, 30)),
    }
    all_usernames = ['user' + str(i) for i in
                     range(0, sum([a[0] for a in filter_changes[test_condition]]))]
    CrtUser.objects.bulk_create(
        [
            CrtUser(username=username) for username in all_usernames
        ]
    )

    class StaffPersonResponse(object):
        def __init__(self, changes_map):
            self.filter_users = []
            self.call_count = 0
            self.changes_map = changes_map

        def return_value(self, *args, **kwargs):
            try:
                added_count, removed_count = self.changes_map[self.call_count]
            except IndexError:
                return [{'login': username} for username in self.filter_users]

            removed_usernames = self.filter_users[:removed_count]
            added_usernames = list(set(all_usernames) - set(self.filter_users))[:added_count]
            self.filter_users = list(
                set(self.filter_users) - set(removed_usernames) | set(added_usernames)
            )
            self.call_count += 1

            return [{'login': username} for username in self.filter_users]

    class StartrekIssue(object):
        def __init__(self, key, summary, description):
            self.key = key
            self.summary = summary
            self.description = description

    class StartrekIssueResponse(object):
        def __init__(self):
            self.call_count = 0

        def return_value(self, *args, **kwargs):
            self.call_count += 1
            key = '{}-{}'.format(kwargs['queue'], self.call_count)
            return StartrekIssue(key, kwargs['summary'], kwargs['description'])

    user = CrtUser.objects.first()
    user.is_superuser = True
    user.save()

    crt_client.login(user.username)
    response = crt_client.json.get(reverse('broken-filters'))
    assert response.status_code == 200
    assert response.content == b'ok'

    staff_person = StaffPersonResponse(filter_changes[test_condition])
    staff_person_mock_path = 'ids.services.staff.repositories.person.StaffPersonRepository.getiter'

    tag_filter = TagFilter.objects.create(name='f1', filter='some=value', type='staff_api')
    with mock.patch(staff_person_mock_path, side_effect=staff_person.return_value):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        SyncFilterTagsTask.apply_async()

    st_issue_repo = StartrekIssueResponse()
    st_mock_path = 'ids.services.startrek2.repositories.base.StBaseRepository.create'

    with (
        mock.patch(staff_person_mock_path, side_effect=staff_person.return_value),
        mock.patch(st_mock_path, side_effect=st_issue_repo.return_value) as st_mocked_method,
    ):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        if test_condition == 'rewrited':
            tag_filter.filter = 'some=another_value'
            tag_filter.save()
        SyncFilterTagsTask.apply_async()

    tag_filter.refresh_from_db()

    # Переписанный фильтр не ломается
    if test_condition == 'ok':
        assert not tag_filter.is_broken
        assert not st_mocked_method.called

    elif test_condition == 'rewrited':
        assert not tag_filter.is_broken
        assert not st_mocked_method.called

        assert tag_filter.actions.filter(type=ACTION_TYPE.TAG_FILTER_CHANGED).exists()
        assert not tag_filter.actions.filter(type__in=[
            ACTION_TYPE.TAG_FILTER_MARKED_BROKEN,
            ACTION_TYPE.TAG_FILTER_MARKED_NOT_BROKEN,
        ]).exists()

        assert tag_filter.users.count() == (
            filter_changes[test_condition][0][0]
            +
            filter_changes[test_condition][1][0]
            -
            filter_changes[test_condition][1][1]
        )

    else:
        assert tag_filter.actions.last().type == ACTION_TYPE.TAG_FILTER_MARKED_BROKEN

        response = crt_client.json.get(reverse('broken-filters'))
        assert response.status_code == 412
        assert response.content == force_bytes(
            'filter "{}" is broken ({}-1: changes exceeded threshold)'.format(
                tag_filter.name, settings.CRT_STARTREK_SECTASK_QUEUE
            ))

        assert st_mocked_method.call_count == 1
        assert st_mocked_method.call_args[1]['queue'] == settings.CRT_STARTREK_SECTASK_QUEUE
        assert st_mocked_method.call_args[1]['followers'] == [
            username.strip() for username in
            constance.config.CRT_BROKEN_STAFF_FILTER_SECTASK_FOLLOWERS.split(',')
        ]
        assert st_mocked_method.call_args[1]['summary'] == (
            'Изменения в фильтре "{}" превысили допустимый лимит'.format(tag_filter.name)
        )

        if test_condition == 'too_much_removed_only':
            assert 'Удаленные логины' in st_mocked_method.call_args[1]['description']
            assert 'Новые логины' not in st_mocked_method.call_args[1]['description']
        elif test_condition == 'too_much_added_only':
            assert 'Удаленные логины' not in st_mocked_method.call_args[1]['description']
            assert 'Новые логины' in st_mocked_method.call_args[1]['description']
        else:
            assert 'Удаленные логины' in st_mocked_method.call_args[1]['description']
            assert 'Новые логины' in st_mocked_method.call_args[1]['description']

        assert tag_filter.users.count() == filter_changes[test_condition][0][0]
        tag_filter.is_broken = False
        with mock.patch(staff_person_mock_path, side_effect=staff_person.return_value):
            tag_filter.save()

        assert tag_filter.actions.last().type == ACTION_TYPE.TAG_FILTER_MARKED_NOT_BROKEN
        assert tag_filter.users.count() == (
            filter_changes[test_condition][0][0]
            +
            filter_changes[test_condition][1][0]
            -
            filter_changes[test_condition][1][1]
        )

        response = crt_client.json.get(reverse('broken-filters'))
        assert response.status_code == 200
        assert response.content == b'ok'


def test_calc_changes_ratio():
    tag_filter = TagFilter.objects.create(name='dummy')
    tag_filter.actions.create(type=ACTION_TYPE.TAG_FILTER_MARKED_BROKEN)

    for current_logins, fetched_logins, correct_ratio in [
        ({'1'}, {'1'}, 0.0),
        (set(), set(), 0.0),
        ({'1'}, {'2'}, 200.0),

        ({'1'}, {'1', '2', '3'}, 200.0),
        ({'1', '2', '3'}, {'1'}, 66.7),

        ({'1', '2', '3'}, set(), 100.0),
        (set(), {'1', '2', '3'}, 100.0),

        ({'1', '2', '3', '4', '5', '6', '7', '8', '9', '10'}, {'1', '2'}, 80.0),
        ({'1', '2'}, {'1', '2', '3', '4', '5', '6', '7', '8', '9', '10'}, 400.0),
    ]:
        _, ratio = changes_is_unsafe(tag_filter, current_logins, fetched_logins)
        assert ratio == correct_ratio


def test_sync_cvs_tags_dismissed(certificate_types, tags, users):
    tag_user = users['tag_user']
    normal_user = users['normal_user']

    pc_type = certificate_types[CERT_TYPE.PC]

    normal_pc_cert = create_certificate(normal_user, pc_type)
    normal_pc_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.FILTERS)

    tag_pc_cert = create_certificate(tag_user, pc_type)
    tag_pc_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.FILTERS)

    mocked_client = make_mock_cvs_client()

    with (
        mock.patch('intranet.crt.tags.tasks.sync_cvs_tags.write_files') as files_writer,
        mock.patch('intranet.crt.utils.cvs.NocCvsClient.__new__', mock.Mock(return_value=mocked_client)),
    ):
        def check_users_after_sync(users):
            call_command('sync_cvs_tags')
            assert {cert['common_name'] for cert in files_writer.call_args[0][0]['certificates']} == users

        check_users_after_sync({'normal_user@ld.yandex.ru', 'tag_user@ld.yandex.ru'})

        CrtUser.objects.filter(username=normal_user.username).update(is_active=False)
        check_users_after_sync({'tag_user@ld.yandex.ru'})

        CrtUser.objects.filter(username=normal_user.username).update(is_active=True)
        check_users_after_sync({'normal_user@ld.yandex.ru', 'tag_user@ld.yandex.ru'})


def test_sync_cvs_tags_without_tags(certificate_types, tags, users):
    pc_type = certificate_types[CERT_TYPE.PC]
    assert pc_type.name in CERT_TYPE.TAGGABLE_TYPES

    host_type = certificate_types[CERT_TYPE.HOST]
    assert host_type.name not in CERT_TYPE.TAGGABLE_TYPES

    normal_user = users['normal_user']
    tag_user = users['tag_user']

    # Тегируемый с тегом
    taggable_with_tag = create_certificate(normal_user, pc_type, serial_number='1')
    taggable_with_tag.add_tag(tags['pc_tag'], source=TAG_SOURCE.FILTERS)

    # Тегируемый без тега
    taggable_wo_tag = create_certificate(tag_user, pc_type, serial_number='2')
    assert taggable_wo_tag.tags.count() == 0

    # Не тегируемый, в выгрузку не попадет
    untaggable = create_certificate(tag_user, host_type, serial_number='3')
    assert untaggable.tags.count() == 0

    mocked_client = make_mock_cvs_client()

    with (
        mock.patch('intranet.crt.tags.tasks.sync_cvs_tags.write_files') as files_writer,
        mock.patch('intranet.crt.utils.cvs.NocCvsClient.__new__', mock.Mock(return_value=mocked_client)),
    ):
        call_command('sync_cvs_tags', force=True)
        assert get_serial_numbers_from_writer_mock(files_writer) == {'1', '2'}


def test_tags_are_applies_to_idm_filter(users, certificates, certificate_types, tags, idm_tag_filters):
    bubblegum = users['bubblegum']
    vpn_filter = idm_tag_filters['Office.VPN']

    assert vpn_filter.tags.count() == 0
    assert bubblegum.certificates.count() == 0

    vpn_filter.tags.add(tags['candy'])

    cert = create_certificate(bubblegum, certificate_types['linux-pc'])
    assert cert.tags.count() == 0

    vpn_filter.add_user(bubblegum)

    mock_getiter_path = 'ids.services.staff.repositories.person.StaffPersonRepository.getiter'
    with mock.patch(mock_getiter_path, side_effect=staff_person_getiter):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        SyncFilterTagsTask.locked_stamped_run()

    assert cert.tags.get() == tags['candy']


def test_sync_cvs_tags_export_nouser_certs(certificate_types, tags, users):
    normal_user = users['normal_user']

    pc_type = certificate_types[CERT_TYPE.PC]
    assert pc_type.name in CERT_TYPE.TAGGABLE_TYPES

    win_wh_shared_type = certificate_types[CERT_TYPE.WIN_WH_SHARED]
    assert win_wh_shared_type.name in CERT_TYPE.TAGGABLE_TYPES
    assert win_wh_shared_type.name in CERT_TYPE.NO_USER_TYPES

    # Пользовательский c пользователем
    user_cert = create_certificate(normal_user, pc_type, serial_number='1')
    user_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.MANUAL)
    # no user type без пользователя
    nouser_cert = create_certificate(normal_user, win_wh_shared_type, serial_number='2')
    nouser_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.MANUAL)
    Certificate.objects.filter(pk=nouser_cert.pk).update(user=None)
    # Пользовательский без пользователя, в выгрузку не попадет
    user_cert_wo_user = create_certificate(normal_user, pc_type, serial_number='3')
    user_cert_wo_user.add_tag(tags['pc_tag'], source=TAG_SOURCE.MANUAL)
    Certificate.objects.filter(pk=user_cert_wo_user.pk).update(user=None)

    mocked_client = make_mock_cvs_client()

    with (
        mock.patch('intranet.crt.tags.tasks.sync_cvs_tags.write_files') as files_writer,
        mock.patch('intranet.crt.utils.cvs.NocCvsClient.__new__', mock.Mock(return_value=mocked_client)),
    ):
        call_command('sync_cvs_tags', force=True)
        assert get_serial_numbers_from_writer_mock(files_writer) == {'1', '2'}


@override_settings(CRT_NOC_THRESHOLDS = {
    'default': {
        'new': 3,
        'removed': 3,
        'changed': 3,
    }
})
def test_certificate_new_limit_triggered():
    old_certs = make_test_certificates([])
    new_certs = make_test_certificates([
        ([1], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.POSTAMATE),
        ([1, 2, 3], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.PC),
        ([1, 2, 3], CERT_TYPE.PC),
    ])
    data_diff = compare_cert_sets(new_certs, old_certs)
    assert data_diff.is_safe()
    assert sorted(data_diff.make_error_msg().split(' | ')) == [
        'pc: new=2/3',
        'postamate: new=3/3',
    ]


@override_settings(CRT_NOC_THRESHOLDS = {
    'default': {
        'new': 2,
        'removed': 2,
        'changed': 2,
    },
    CERT_TYPE.POSTAMATE: {
        'new': 3,
    },
})
def test_certificate_new_limit_overriden():
    old_certs = make_test_certificates([])
    new_certs = make_test_certificates([
        ([1], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.POSTAMATE),
        ([1, 2, 3], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.PC),
        ([1, 2, 3], CERT_TYPE.PC),
    ])
    data_diff = compare_cert_sets(new_certs, old_certs)
    assert data_diff.is_safe()
    assert sorted(data_diff.make_error_msg().split(' | ')) == [
        'pc: new=2/2',
        'postamate: new=3/3',
    ]


@override_settings(CRT_NOC_THRESHOLDS = {
    'default': {
        'new': 2,
        'removed': 2,
        'changed': 2,
    },
    CERT_TYPE.POSTAMATE: {
        'new': 4,
        'removed': 4,
    },
})
def test_add_remove_many_postamates():
    certs_without_postamates = make_test_certificates([])
    certs_with_postamates = make_test_certificates([
        ([1], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.POSTAMATE),
        ([1, 2, 3], CERT_TYPE.POSTAMATE),
        ([1, 2], CERT_TYPE.POSTAMATE),
    ])
    adding_diff = compare_cert_sets(certs_with_postamates, certs_without_postamates)
    assert adding_diff.is_safe()
    assert adding_diff.make_error_msg() == 'postamate: new=4/4'

    removing_diff = compare_cert_sets(certs_without_postamates, certs_with_postamates)
    assert removing_diff.is_safe()
    assert removing_diff.make_error_msg() == 'postamate: removed=4/4'


def test_cvs_file_formats(certificate_types, users, tags):
    sample_cert = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.PC],
        serial_number='1'
    )
    sample_cert.add_tag(tags['pc_tag'], source=TAG_SOURCE.FILTERS)

    mocked_client = make_mock_cvs_client()

    mocked_noc_csv_writer = mock.Mock()
    mocked_noc_json_writer = mock.Mock()
    mocked_json_with_old_certs_writer = mock.Mock()

    mocked_open = mock.Mock()
    mocked_open.__enter__ = mock.Mock(side_effect=[
        mocked_noc_csv_writer,
        mocked_noc_json_writer,
        mocked_json_with_old_certs_writer,
    ])
    mocked_open.__exit__ = mock.Mock()

    with (
        mock.patch('intranet.crt.utils.cvs.NocCvsClient.__new__', mock.Mock(return_value=mocked_client)),
        mock.patch('intranet.crt.utils.file.SafetyWriteFile.__new__', mock.Mock(return_value=mocked_open)),
        mock.patch('intranet.crt.utils.time.aware_to_timestamp', mock.Mock(return_value=1337)),
    ):
        call_command('sync_cvs_tags')
        assert list(mocked_noc_csv_writer.writelines.call_args[0][0]) == ['|1|pc_tag|normal_user@ld.yandex.ru|TestCA|\n']

        def join_write_calls_args(writer):
            return ''.join(call.args[0] for call in writer.write.call_args_list)

        assert join_write_calls_args(mocked_noc_json_writer) == '''{
    "certificates": [
        {
            "serial_number": "1",
            "tags": [
                "pc_tag"
            ],
            "common_name": "normal_user@ld.yandex.ru",
            "ca_name": "TestCA"
        }
    ],
    "meta": {
        "last_sync": 1337
    }
}'''
        assert join_write_calls_args(mocked_json_with_old_certs_writer) == '''[
    {
        "serial_number": "1",
        "tags": [
            "pc_tag"
        ],
        "common_name": "normal_user@ld.yandex.ru",
        "ca_name": "TestCA",
        "type": "pc"
    }
]'''


def test_compare_diffs(certificate_types, tags, users):
    sample_cert = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.POSTAMATE],
        serial_number='AAAA1234',
        common_name='postamate-test@market.yandex',
        ca_name=CA_NAME.TEST_CA,
    )
    sample_cert.add_tag(tags['tag1'], source=TAG_SOURCE.MANUAL)
    sample_cert.save()

    new_certs = NocCertSerializer.from_db().data
    old_certs = {
        'certificates': []
    }

    data_diff = compare_cert_sets(new_certs, old_certs)
    old_diff = {
        'tags': {
            'added': {},
            'removed': {}
        },
        'certificates': {
            'new': {
                'aaaa1234': {
                    'serial_number': 'aaaa1234',
                    'common_name': 'postamate-test@market.yandex',
                    'ca_name': 'TestCA',
                    'tags': ['tag1'],
                    'type': 'postamate'
                },
            },
            'changed': {},
            'removed': {}
        }
    }

    assert data_diff.diff == {
        'tags': TagsDiffDict(),
        'certificates': {
            'new': {
                'aaaa1234': collections.OrderedDict([
                    ('serial_number', 'aaaa1234'),
                    ('common_name', 'postamate-test@market.yandex'),
                    ('ca_name', 'TestCA'),
                    ('tags', ['tag1']),
                    ('type', 'postamate'),
                ]),
            },
            'changed': {},
            'removed': {},
        }
    }
    assert old_diff != data_diff.diff
    assert are_diffs_equal(old_diff, data_diff.diff)

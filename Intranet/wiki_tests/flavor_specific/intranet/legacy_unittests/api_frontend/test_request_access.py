
from unittest import skipIf

import mock
from django.conf import settings
from django.contrib.auth import get_user_model
from django.test import override_settings
from rest_framework.serializers import ValidationError
from ujson import loads

from wiki import access as wiki_access
from wiki.api_frontend.serializers.process_access_request import (
    ACTION_ALLOW_APPLICANT,
    ACTION_ALLOW_GROUPS,
    ACTION_DENY,
    ProcessAccessRequestSerializer,
)
from wiki.notifications.generators.base import EventTypes
from wiki.notifications.models import PageEvent
from wiki.pages.access import get_raw_access, interpret_raw_access
from wiki.pages.models import AccessRequest
from wiki.users import dao
from wiki.users.models import Group
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase, now_for_tests

if settings.IS_BUSINESS:
    from wiki.users.models import GROUP_TYPES

User = get_user_model()


class APIRequestAccessHandlerTest(BaseApiTestCase):
    def setUp(self):
        super(APIRequestAccessHandlerTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def _check_request(self, page, moment_before, moment_after, reason, event_count=1):
        access_request = AccessRequest.objects.get(applicant=self.user, page=page, verdict_by=None)

        self.assertTrue(moment_before <= access_request.created_at <= moment_after)
        self.assertEqual(access_request.reason, reason)

        event_qs = PageEvent.objects.filter(page=page, event_type=EventTypes.request_access, author=self.user)
        self.assertEqual(len(event_qs), event_count)
        if event_count == 1:
            event = event_qs[0]
            self.assertEqual(event.meta['reason'], reason)
            self.assertTrue(moment_before <= event.created_at <= moment_after)

    def _check_no_request(self, page):
        self.assertFalse(AccessRequest.objects.filter(applicant=self.user, page=page, verdict_by=None).exists())
        self.assertFalse(
            PageEvent.objects.filter(page=page, event_type=EventTypes.request_access, author=self.user).exists()
        )

    def test_request_valid(self):
        page = self.create_page(
            tag='СтраницаАнтона1', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)

        # 1st request
        moment_before = now_for_tests()
        reason = 'хочу всё\nзнать'
        response = self.client.put(
            '/_api/frontend/{page_supertag}/.requestaccess'.format(page_supertag=page.supertag), {'reason': reason}
        )
        moment_after = now_for_tests()
        self.assertEqual(response.status_code, 200)
        self._check_request(page, moment_before, moment_after, reason)

        # 2nd request
        moment_before = now_for_tests()
        reason = 'хочу!!!'
        response = self.client.put(
            '/_api/frontend/{page_supertag}/.requestaccess'.format(page_supertag=page.supertag), {'reason': reason}
        )
        moment_after = now_for_tests()
        self.assertEqual(response.status_code, 200)
        self._check_request(page, moment_before, moment_after, reason, 2)

    def test_request_invalid_no_reason(self):
        page = self.create_page(
            tag='СтраницаАнтона2', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)

        response = self.client.put(
            '/_api/frontend/{page_supertag}/.requestaccess'.format(page_supertag=page.supertag), {}
        )
        self.check_invalid_form(response, error_key='reason')
        self._check_no_request(page)

    def test_request_already_has_access(self):
        page = self.create_page(
            tag='СтраницаАнтона3', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )

        response = self.client.put(
            '/_api/frontend/{page_supertag}/.requestaccess'.format(page_supertag=page.supertag),
            {'reason': 'some reason'},
        )
        self.check_error(response, 409, 'ALREADY_HAS_ACCESS')
        self._check_no_request(page)


class APIAccessRequestProcessingHandlerTest(BaseApiTestCase):
    def setUp(self):
        super(APIAccessRequestProcessingHandlerTest, self).setUp()
        self.setGroupMembers()
        self.user = self.user_chapson
        self.client.login(self.user.username)

        def f(x):
            return [self.user_kolomeetz.staff.pk]

        self.responsible_patch = mock.patch(
            target='wiki.api_frontend.serializers.process_access_request.get_group_responsibles_for_page', new=f
        )
        self.responsible_patch.start()

    def set_groups_intranet(self):
        super(APIAccessRequestProcessingHandlerTest, self).set_groups_intranet()
        for group in Group.objects.all():
            group.name = group.url
            group.save()

    def set_groups_business(self):
        from intranet.wiki.tests.wiki_tests.common.ddf_compat import new

        group_data = self.open_json_fixture('group.json')
        parent_child_map = {}
        for group in group_data:
            fields = group['fields']
            if fields['parent']:
                fields['parent'] = parent_child_map[fields['parent']]
            fields['name'] = fields.pop('url')
            fields['org'] = None
            fields['group_type'] = GROUP_TYPES.group
            g = new(Group, **fields)
            parent_child_map[group['pk']] = g
            g.save_base()
            setattr(self, str('group_' + g.name), g)

    def set_group_members_extranet(self):
        Group.objects.get(name='yandex_mnt').user_set.add(self.user_thasonic)
        Group.objects.get(name='yandex_mnt').user_set.add(self.user_kolomeetz)
        Group.objects.get(name='yandex').user_set.add(self.user_thasonic)
        Group.objects.get(name='yandex').user_set.add(self.user_kolomeetz)
        Group.objects.get(name='yandex_mnt_srv').user_set.add(self.user_thasonic)

    def tearDown(self):
        super(APIAccessRequestProcessingHandlerTest, self).tearDown()
        self.responsible_patch.stop()

    def test_get_missing(self):
        response = self.client.get('/_api/frontend/.requestaccess/1')
        self.assertEqual(response.status_code, 404)

    def test_get_not_owner_not_group_responsible(self):
        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )
        access_request_pk = self._request_access(page, self.user_chapson)

        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.check_error(response, 403, 'USER_HAS_NO_ACCESS')

    def create_page(self, **kwargs):
        return super(APIAccessRequestProcessingHandlerTest, self).create_page(
            tag=kwargs.get('tag', 'СтраницаАнтона'),
            body=kwargs.get('body', 'page test'),
            authors_to_add=kwargs.get('authors_to_add', [self.user_chapson]),
            last_author=kwargs.get('last_author', self.user_chapson),
        )

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_get_pending(self):
        page = self.create_page()

        wiki_access.set_access(
            page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            send_notification_signals=False,
            groups=[Group.objects.get(name='ext')],
        )
        moment_before = now_for_tests()
        access_request_pk = self._request_access(page, self.user_thasonic, reason='надо')
        moment_after = now_for_tests()

        # for group responsible
        self.client.login(self.user_kolomeetz.username)
        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.assertEqual(response.status_code, 200)
        json = response.json()

        data = json['data']

        self.assertEqual(data.pop('status'), 'pending')
        self.assertEqual(data.pop('may_allow'), False)
        self.assertEqual(data.pop('applicant')['login'], self.user_thasonic.username)
        self.assertEqual(data.pop('reason'), 'надо')
        data.pop('requested_at')
        self.assertFalse(len(data))
        access_request = AccessRequest.objects.get(id=access_request_pk)
        self.assertTrue(moment_before < access_request.created_at < moment_after)

        # for page owner
        self.client.login(self.user_chapson.username)
        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        data = json['data']
        self.assertEqual(data.pop('status'), 'pending')
        self.assertEqual(data.pop('may_allow'), True)
        self.assertEqual(data.pop('applicant')['login'], self.user_thasonic.username)
        self.assertEqual(data.pop('reason'), 'надо')
        access_request = AccessRequest.objects.get(id=access_request_pk)
        self.assertTrue(moment_before <= access_request.created_at <= moment_after)
        if settings.IS_INTRANET:
            self.assertEqual(
                data.pop('applicant_services'),
                list(
                    (
                        {
                            'id': getattr(Group.objects.get(name='yandex_mnt_srv'), dao.get_id_attr_name()),
                            'name': 'yandex_mnt_srv',
                        },
                    )
                ),
            )
            self.assertEqual(
                data.pop('applicant_departments'),
                [
                    {'id': getattr(Group.objects.get(name='yandex'), dao.get_id_attr_name()), 'name': 'yandex'},
                    {'id': getattr(Group.objects.get(name='yandex_mnt'), dao.get_id_attr_name()), 'name': 'yandex_mnt'},
                ],
            )
        elif settings.IS_BUSINESS:
            self.assertEqual(
                data.pop('applicant_services'),
                [
                    {'id': getattr(Group.objects.get(name='yandex'), dao.get_id_attr_name()), 'name': 'yandex'},
                    {'id': getattr(Group.objects.get(name='yandex_mnt'), dao.get_id_attr_name()), 'name': 'yandex_mnt'},
                    {
                        'id': getattr(Group.objects.get(name='yandex_mnt_srv'), dao.get_id_attr_name()),
                        'name': 'yandex_mnt_srv',
                    },
                ],
            )
            self.assertEqual(data.pop('applicant_departments'), [])
        else:
            data.pop('applicant_services')
            data.pop('applicant_departments')
        data.pop('requested_at')
        self.assertFalse(len(data))

    def test_get_processed(self):
        page = self.create_page()
        wiki_access.set_access(
            page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            send_notification_signals=False,
            groups=[Group.objects.get(name='ext')],
        )

        # granted / for group responsible
        access_request_pk = self._request_access(
            page, self.user_thasonic, reason='надо', verdict_by=self.user_chapson, verdict=True
        )

        self.client.login(self.user_kolomeetz.username)

        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))

        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        data = json['data']
        self.assertEqual(data.pop('status'), 'processed')
        self.assertEqual(data.pop('granted'), True)
        self.assertEqual(data.pop('verdict_by')['login'], self.user_chapson.username)
        self.assertEqual(data.pop('verdict_reason', True), None)
        self.assertFalse(len(data))

        # denied / for page owner
        access_request_pk = self._request_access(
            page,
            self.user_thasonic,
            reason='надо',
            verdict_by=self.user_chapson,
            verdict=False,
            verdict_reason='не надо',
        )

        self.client.login(self.user_chapson.username)
        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        data = json['data']
        self.assertEqual(data.pop('status'), 'processed')
        self.assertEqual(data.pop('granted'), False)
        self.assertEqual(data.pop('verdict_by')['login'], self.user_chapson.username)
        self.assertEqual(data.pop('verdict_reason'), 'не надо')
        self.assertFalse(len(data))

    def test_get_obsolete(self):
        page = self.create_page()

        wiki_access.set_access(
            page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            send_notification_signals=False,
            groups=[Group.objects.get(name='ext')],
            staff_models=[self.user_thasonic.staff],
        )
        access_request_pk = self._request_access(page, self.user_thasonic, reason='надо')

        # for group responsible
        self.client.login(self.user_kolomeetz.username)
        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        data = json['data']
        self.assertEqual(data.pop('status'), 'obsolete')
        self.assertFalse(len(data))

        # for page owner
        self.client.login(self.user_chapson.username)
        response = self.client.get('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk))
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        data = json['data']
        self.assertEqual(data.pop('status'), 'obsolete')
        self.assertFalse(len(data))

    def test_process_not_owner_not_group_responsible(self):
        page = self.create_page(tag='СтраницаСаши', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic)
        access_request_pk = self._request_access(page, self.user_chapson)
        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_APPLICANT})
        self.check_error(response, 403, 'USER_HAS_NO_ACCESS')

    def test_process_allow_by_group_responsible(self):
        page = self.create_page(tag='СтраницаСаши', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic)

        wiki_access.set_access(
            page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            send_notification_signals=False,
            staff_models=[self.user_thasonic.staff, self.user_kolomeetz.staff],
        )

        access_request_pk = self._request_access(page, self.user_chapson)

        self.client.login(self.user_kolomeetz.username)
        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_APPLICANT})

        self.check_error(response, 403, 'USER_HAS_NO_ACCESS')

    def test_process_already_processed(self):
        page = self.create_page()
        access_request_pk = self._request_access(
            page, self.user_thasonic, reason='надо', verdict_by=self.user_chapson, verdict=True
        )

        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_APPLICANT})
        self.check_error(response, 409, 'ACCESS_REQUEST_ALREADY_PROCESSED')

    def test_process_invalid_action(self):
        # wrong action
        response = self._process_access_request(1, {'action': 'wrong_action'})
        self.check_invalid_form(response, error_key='action')

        # missing action
        response = self._process_access_request(1, {})
        self.check_invalid_form(response, error_key='action')

    def test_process_missing(self):
        response = self._process_access_request(99999, {'action': ACTION_ALLOW_APPLICANT})
        self.assertEqual(response.status_code, 404)

    def _check_granted_notification(self, page, author, access_request_pk):
        event_qs = PageEvent.objects.filter(page=page, event_type=EventTypes.resolve_access, author=author, notify=True)
        self.assertEqual(len(event_qs), 1)
        meta = event_qs[0].meta
        self.assertEqual(meta['staff_id'], self.user_thasonic.staff.id)
        self.assertEqual(meta['group_id'], None)
        self.assertEqual(meta['resolution_type'], 'granted')
        self.assertEqual(meta['access_request_id'], access_request_pk)

    def _check_denied_notification(self, page, author, access_request_pk):
        event_qs = PageEvent.objects.filter(page=page, event_type=EventTypes.resolve_access, author=author, notify=True)
        self.assertEqual(len(event_qs), 1)
        meta = event_qs[0].meta
        self.assertEqual(meta['receiver_id'], [self.user_thasonic.id])
        self.assertEqual(meta['resolution_type'], 'notchanged')
        self.assertEqual(meta['access_request_id'], access_request_pk)

    def _check_group_responsible_notification(self, page, author, group_urls, applicant, access_request_pk):
        event_qs = PageEvent.objects.filter(page=page, event_type=EventTypes.request_access, author=author, notify=True)
        group_ids = list(Group.objects.filter(name__in=group_urls).values_list('id', flat=True))
        for event in event_qs:
            meta = event.meta
            group_id = meta['group_id']
            self.assertTrue(group_id in group_ids)
            group_ids.remove(group_id)
            group = Group.objects.get(pk=group_id)
            self.assertEqual(meta['resolution_type'], 'addtogroup')
            self.assertEqual(meta['group_name'], group.name)
            self.assertEqual(meta['emitter_name'], applicant.username)
            self.assertEqual(meta['access_request_id'], access_request_pk)
            self.assertEqual(meta['notify_all'], False)
        self.assertFalse(len(group_ids))

    def _check_access(self, page, users, group_urls):
        access = interpret_raw_access(get_raw_access(page))
        users = [user.staff for user in users]
        groups = [Group.objects.get(name=id) for id in group_urls]
        self.assertEqual(set(access['users']), set(users))
        self.assertEqual(set(access['groups']), set(groups))

    def _check_processed_access_request(self, access_request_pk, verdict=True, verdict_reason=None, verdict_by=None):
        access_request = AccessRequest.objects.select_related('verdict_by').get(pk=access_request_pk)
        if verdict_by is None:
            verdict_by = self.user
        self.assertEqual(access_request.verdict, verdict)
        self.assertEqual(
            access_request.verdict_by, None if verdict is None else User.objects.get(username=verdict_by.username)
        )
        self.assertEqual(access_request.verdict_reason, verdict_reason)

    def _request_access(self, page, applicant, reason='fake', verdict_by=None, verdict=None, verdict_reason=None):
        access_request = AccessRequest(
            applicant=User.objects.get(username=applicant.username),
            page=page,
            reason=reason,
            verdict_by=User.objects.get(username=verdict_by.username) if verdict_by else None,
            verdict=verdict,
            verdict_reason=verdict_reason,
        )
        access_request.save()
        return access_request.pk

    def _process_access_request(self, access_request_pk, data):
        return self.client.post('/_api/frontend/.requestaccess/{id}'.format(id=access_request_pk), data=data)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_allow_applicant(self):
        page = self.create_page()
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)
        access_request_pk = self._request_access(page, self.user_thasonic)

        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_APPLICANT})
        self.assertEqual(response.status_code, 200)
        self._check_processed_access_request(access_request_pk)
        self._check_access(page, [self.user_thasonic], [])
        self._check_granted_notification(page, self.user, access_request_pk)

    def _test_process_allow_groups(self, applicant_groups, other_groups):
        page = self.create_page()
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)
        access_request_pk = self._request_access(page, self.user_thasonic)

        all_groups_urls = applicant_groups + other_groups
        qs = Group.objects.filter(name__in=all_groups_urls)
        all_groups_ids = list(qs.values_list('dir_id' if settings.IS_BUSINESS else 'id', flat=True))

        response = self._process_access_request(
            access_request_pk, {'action': ACTION_ALLOW_GROUPS, 'groups': all_groups_ids}
        )
        self.assertEqual(response.status_code, 200)
        self._check_processed_access_request(access_request_pk, True)
        self._check_access(page, [], all_groups_urls)
        if applicant_groups:
            self._check_granted_notification(page, self.user, access_request_pk)
        self._check_group_responsible_notification(page, self.user, other_groups, self.user_thasonic, access_request_pk)
        return (page, access_request_pk)

    def test_process_deny_empty_groups_list(self):
        page = self.create_page()
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)
        access_request_pk = self._request_access(page, self.user_thasonic)
        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_GROUPS, 'groups': []})

        self.check_invalid_form(
            response,
            error_value='No groups given',
        )

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_allow_applicant_groups(self):
        self._test_process_allow_groups(['yandex_mnt', 'yandex_mnt_srv'], [])

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_allow_other_groups(self):
        self._test_process_allow_groups([], ['ext', 'ext_test'])

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_allow_both_applicant_groups_and_other_groups(self):
        self._test_process_allow_groups(['yandex_mnt', 'yandex_mnt_srv'], ['ext', 'ext_test'])

    def test_process_allow_unknown_group(self):
        page = self.create_page()
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)
        access_request_pk = self._request_access(page, self.user_thasonic)

        response = self._process_access_request(access_request_pk, {'action': ACTION_ALLOW_GROUPS, 'groups': [99999]})
        self.check_invalid_form(response, error_value='No groups with such ids: "99999"')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_deny(self):
        page = self.create_page()
        wiki_access.set_access(
            page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            send_notification_signals=False,
            groups=[Group.objects.get(name='ext')],
        )
        access_request_pk = self._request_access(page, self.user_thasonic)

        # by group responsible
        self.client.login(self.user_kolomeetz.username)
        response = self._process_access_request(access_request_pk, {'action': ACTION_DENY, 'verdict_reason': 'нет1'})
        self.assertEqual(response.status_code, 200)
        self._check_processed_access_request(access_request_pk, False, 'нет1', self.user_kolomeetz)
        self._check_access(page, [], ['ext'])
        self._check_denied_notification(page, self.user_kolomeetz, access_request_pk)

        # by page owner
        self.client.login(self.user_chapson.username)
        access_request_pk = self._request_access(page, self.user_thasonic)
        with self.assertNumQueries(23 if settings.IS_BUSINESS else 16):
            response = self._process_access_request(
                access_request_pk, {'action': ACTION_DENY, 'verdict_reason': 'нет2'}
            )
        self.assertEqual(response.status_code, 200)
        self._check_processed_access_request(access_request_pk, False, 'нет2')
        self._check_access(page, [], ['ext'])
        self._check_denied_notification(page, self.user, access_request_pk)

    def test_process_deny_no_reason(self):

        page = self.create_page()
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson, send_notification_signals=False)
        access_request_pk = self._request_access(page, self.user_thasonic)

        # когда версия rest_framework поднимется выше 3.1.3 надо раскомментировать
        # там это реализовано в CharField.
        # response = self._process_access_request(access_request_pk, {"action": ACTION_DENY, "verdict_reason": "  "})
        # self.check_invalid_form(response, error_key="verdict_reason")

        response = self._process_access_request(access_request_pk, {'action': ACTION_DENY})
        self.check_invalid_form(response, error_value='Verdict reason is required for denial')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_process_allow_then_deny(self):
        (page, access_request_pk) = self._test_process_allow_groups(['yandex_mnt'], ['ext'])

        # deny by group responsible
        self.client.login(self.user_kolomeetz.username)
        response = self._process_access_request(access_request_pk, {'action': ACTION_DENY, 'verdict_reason': 'нет'})
        self.check_error(response, 409, 'ACCESS_REQUEST_ALREADY_PROCESSED')

    def test_departments_are_ok_in_b2b(self):
        serializer = ProcessAccessRequestSerializer()

        def validate():
            serializer.validate(dict(action=ACTION_ALLOW_GROUPS, departments=['non empty'], groups=['not empty']))

        with override_settings(IS_INTRANET=False):
            self.assertNotRaises(ValidationError, validate)
        with override_settings(IS_INTRANET=True):
            self.assertRaises(ValidationError, validate)

# coding: utf-8

from hamcrest import (
    assert_that,
    contains_inanyorder,
    has_length,
    has_entries,
    is_,
    none,
)

from testutils import (
    TestCase,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ActionModel,
    MaillistCheckModel,
    OrganizationRevisionCounterModel,
    OrganizationServiceModel,
    ServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import MAILLIST_SERVICE_SLUG


class TestMaillistCheckCase(TestCase):
    def setUp(self):
        super(TestMaillistCheckCase, self).setUp()

        self.org_id = self.organization['id']
        self.another_org_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']['id']
        action_model = ActionModel(self.main_connection)
        action_model.create(self.org_id, 'test', 1, {}, 'user')

        self.maillist_service_id = ServiceModel(self.main_connection).get_by_slug(
            MAILLIST_SERVICE_SLUG,
            fields=['id']
        )['id']
        OrganizationServiceModel(self.main_connection).create(self.org_id, self.maillist_service_id)
        OrganizationServiceModel(self.main_connection).create(self.another_org_id, self.maillist_service_id)

        self.check1 = MaillistCheckModel(self.main_connection).create(org_id=self.org_id, revision=1)
        self.check2 = MaillistCheckModel(self.main_connection).create(org_id=self.another_org_id, revision=4)

    def test_get_organizations_for_sync(self):
        # Проверяем, что get_organizations_for_sync возвращает организации
        # у которых revision из таблицы revision_counters больше revision из таблицы maillist_checks
        # (если ml_is_ok = True)
        response = MaillistCheckModel(self.main_connection).get_organizations_for_sync()
        assert_that(
            response,
            has_length(1),
        )
        org_last_revision = OrganizationRevisionCounterModel(self.main_connection) \
            .get(self.org_id)['revision']
        assert_that(
            response[0],
            has_entries(
                org_id=self.check1['org_id'],
                last_revision=org_last_revision,
            )
        )

    def test_get_organizations_for_sync_ml_is_ok_false(self):
        # Проверяем, что get_organizations_for_sync возвращает организации, у которых ml_is_ok = False
        MaillistCheckModel(self.main_connection).update(
            update_data={'ml_is_ok': False},
            filter_data={'org_id': self.check2['org_id']}
        )
        response = MaillistCheckModel(self.main_connection).get_organizations_for_sync()
        assert_that(
            response,
            has_length(1),
        )
        org_last_revision = OrganizationRevisionCounterModel(self.main_connection) \
            .get(self.org_id)['revision']

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    org_id=self.check1['org_id'],
                    last_revision=org_last_revision,
                ),
            )
        )

    def test_get_organizations_for_sync_last_migration_none(self):
        # Проверяем, что get_organizations_for_sync возвращает организации, которых нет в maillist_checks

        # Удалим запись об одной из организаций из таблицы maillist_checks
        MaillistCheckModel(self.main_connection).delete(filter_data={'org_id': self.another_org_id})
        org_last_revision = OrganizationRevisionCounterModel(self.main_connection) \
            .get(self.org_id)['revision']
        another_org_last_revision = OrganizationRevisionCounterModel(self.main_connection)\
            .get(self.another_org_id)['revision']

        response = MaillistCheckModel(self.main_connection).get_organizations_for_sync()
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    org_id=self.check1['org_id'],
                    last_revision=org_last_revision,
                ),
                has_entries(
                    org_id=self.check2['org_id'],
                    last_revision=another_org_last_revision,
                ),
            )
        )

    def test_insert_or_update__insert(self):
        # Добавляем новую запись в таблицу maillist_checks
        model = MaillistCheckModel(self.main_connection)
        model.delete(filter_data={'org_id': self.another_org_id})
        assert_that(
            model.get(self.another_org_id),
            is_(none()),
        )
        data = {
            'revision': 25,
            'problems': 'Test',
            'ml_is_ok': False,
        }
        model.insert_or_update(self.another_org_id, data)
        assert_that(
            model.get(self.another_org_id),
            has_entries(
                revision=data['revision'],
                problems=data['problems'],
                ml_is_ok=data['ml_is_ok'],
            )
        )

    def test_insert_or_update__update(self):
        # Обновляем существующую запись в таблице maillist_checks
        model = MaillistCheckModel(self.main_connection)
        updated_data = {
            'revision': 25,
            'problems': 'Test',
            'ml_is_ok': False,
        }
        model.insert_or_update(self.org_id, updated_data)
        assert_that(
            model.get(self.org_id),
            has_entries(
                revision=updated_data['revision'],
                problems=updated_data['problems'],
                ml_is_ok=updated_data['ml_is_ok'],
            )
        )

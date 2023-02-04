import datetime

from testutils import TestCase, tvm2_auth_success
from hamcrest import (
    assert_that,
    has_entries,
    has_item,
    has_key,
    is_not,
    has_length,
)

from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.models import ServiceModel, \
    OrganizationLicenseConsumedInfoModel
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service, UserServiceLicenses


class TestAdminTrackerLicensesView(TestCase):
    enable_admin_api = True
    create_organization = True

    def setUp(self, *args, **kwargs):
        super(TestAdminTrackerLicensesView, self).setUp(*args, **kwargs)

        org_id = self.organization['id']
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            service['slug']
        )

        dates = [
            datetime.date(year=2020, month=1, day=1),
            datetime.date(year=2020, month=1, day=2),
            datetime.date(year=2020, month=1, day=3),
            datetime.date(year=2020, month=1, day=4),
            datetime.date(year=2020, month=1, day=5),
        ]
        self.dates = dates

        self.users = []
        # 2 лицензии в первый день
        for _ in range(2):
            user = self.create_user(org_id=org_id)
            self.users.append(user)
            UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[0])

        # +2 новые лицензии и -1 старая лицензия во второй день
        for _ in range(2):
            user = self.create_user(org_id=org_id)
            self.users.append(user)
            UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        UserServiceLicenses(self.main_connection).delete(filter_data={
            'user_id': self.users[0]['id'],
            'org_id': org_id,
            'service_id': service['id'],
        })
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[1])

        # +2 лицензии в третий день
        for _ in range(2):
            user = self.create_user(org_id=org_id)
            self.users.append(user)
            UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[2])

        # -все лицензии в четвертый день
        UserServiceLicenses(self.main_connection).delete(filter_data={
            'org_id': org_id,
            'service_id': service['id'],
        })
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[3])

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_date_interval(self):
        response = self.get_json(
            '/admin/organizations/{org_id}/services/tracker/licenses/?begin_date={begin_date}&end_date={end_date}'.format(
                org_id=self.organization['id'],
                begin_date=self.dates[1],
                end_date=self.dates[3],
            ),
        )
        assert_that(
            response,
            is_not(has_key(str(self.dates[0])))
        )
        assert_that(
            response,
            is_not(has_key(str(self.dates[4])))
        )

        assert_that(response[str(self.dates[1])], has_entries(
            count=3
        ))
        assert_that(response[str(self.dates[1])]['deleted'], has_item(has_entries(id=self.users[0]['id'])))
        assert_that(response[str(self.dates[1])]['added'], has_item(has_entries(id=self.users[2]['id'])))
        assert_that(response[str(self.dates[1])]['added'], has_item(has_entries(id=self.users[3]['id'])))
        assert_that(response[str(self.dates[1])]['users'], has_item(has_entries(id=self.users[1]['id'])))
        assert_that(response[str(self.dates[1])]['users'], has_item(has_entries(id=self.users[2]['id'])))
        assert_that(response[str(self.dates[1])]['users'], has_item(has_entries(id=self.users[3]['id'])))

        assert_that(response[str(self.dates[2])], has_entries(
            count=5
        ))
        assert_that(response[str(self.dates[2])]['added'], has_item(has_entries(id=self.users[4]['id'])))
        assert_that(response[str(self.dates[2])]['added'], has_item(has_entries(id=self.users[5]['id'])))
        assert_that(response[str(self.dates[2])]['users'], has_item(has_entries(id=self.users[1]['id'])))
        assert_that(response[str(self.dates[2])]['users'], has_item(has_entries(id=self.users[2]['id'])))
        assert_that(response[str(self.dates[2])]['users'], has_item(has_entries(id=self.users[3]['id'])))
        assert_that(response[str(self.dates[2])]['users'], has_item(has_entries(id=self.users[4]['id'])))
        assert_that(response[str(self.dates[2])]['users'], has_item(has_entries(id=self.users[5]['id'])))

        assert_that(response[str(self.dates[3])], has_entries(
            count=0
        ))
        assert_that(response[str(self.dates[3])]['deleted'], has_length(5))
        assert_that(response[str(self.dates[3])]['added'], has_length(0))
        assert_that(response[str(self.dates[3])]['users'], has_length(0))

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_begin_date(self):
        response = self.get_json(
            '/admin/organizations/{org_id}/services/tracker/licenses/?begin_date={begin_date}'.format(
                org_id=self.organization['id'],
                begin_date=self.dates[1],
            ),
        )
        assert_that(response, is_not(has_key(str(self.dates[0]))))
        assert_that(response, has_key(str(self.dates[1])))
        assert_that(response, has_key(str(self.dates[2])))
        assert_that(response, is_not(has_key(str(self.dates[3]))))
        assert_that(response, is_not(has_key(str(self.dates[4]))))

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_end_date(self):
        response = self.get_json(
            '/admin/organizations/{org_id}/services/tracker/licenses/?end_date={end_date}'.format(
                org_id=self.organization['id'],
                end_date=self.dates[4],
            ),
        )
        assert_that(response, has_key(str(self.dates[0])))
        assert_that(response, has_key(str(self.dates[1])))
        assert_that(response, has_key(str(self.dates[2])))
        assert_that(response, has_key(str(self.dates[3])))
        assert_that(response, has_key(str(self.dates[4])))

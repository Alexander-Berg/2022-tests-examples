# coding: utf-8

from hamcrest import (
    assert_that,
    has_entries,
    all_of,
    has_length,
    greater_than,
    has_item,
    contains,
    equal_to,
    matches_regexp,

)

from testutils import (
    create_department,
    TestCase,
    get_oauth_headers,
    oauth_success,
    OAUTH_CLIENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import (
    scope,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    disable_service,
    enable_service,
)


class TestEvent(TestCase):

    def setUp(self):
        super(TestEvent, self).setUp()
        self.clean_actions_and_events()
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.move(self.user, department)

    def move(self, user, dep):
        url = '/users/%s/' % user['id']
        data = {
            'department_id': dep['id'],
        }
        self.patch_json(url, data)

    def test_events_all(self):
        # проверим, что ручка вернёт все события
        # в данном случае, это только два события
        # про перемещение пользователя в отдел
        data = self.get_json('/events/')
        assert_that(
            data['result'],
            all_of(
                has_item(
                    has_entries(
                        revision=4,
                        name='user_moved',
                    )
                ),
                has_item(
                    has_entries(
                        revision=4,
                        name='department_user_added',
                    )
                ),
            )
        )

    def test_events_by_name(self):
        # фильтр события по типу
        data = self.get_json('/events/?name=user_moved')
        assert_that(data['result'], has_length(1))
        assert_that(
            data['result'],
            contains(
                has_entries(
                    revision=4,
                    name='user_moved',
                )
            )
        )

    def test_events_no_revision(self):
        # фильтр события по несуществющей ривизии
        self.get_json('/events/?revision=1000000', expected_code=404)

    def test_events_greater_revision(self):
        # фильтр событий старше ревизии
        data = self.get_json('/events/?revision__gt=0')
        assert_that(len(data['result']), greater_than(0))

    def test_events_greater_revision__404(self):
        # фильтр событий больше максимальной ревизии
        self.get_json('/events/?revision__gt=1000000', expected_code=404)

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_events])
    def test_403_for_disabled_service(self):
        # Проверяем, что неподключёным сервисам возвращается 403
        # с разным сообщением в поле code.
        headers = get_oauth_headers(as_org=self.organization['id'])
        response = self.get_json('/events/', headers=headers, expected_code=403)
        assert_that(
            response['code'],
            equal_to('service_is_not_enabled')
        )
        assert_that(
            response['message'],
            matches_regexp('Service \(identity: \S*, id: \d*\) is not enabled')
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            'service-slug',
        )

        response = self.get_json('/events/', headers=headers, expected_code=200)
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            'service-slug',
            'some-reason',
        )
        # Так как сервис был подключен, но теперь выключен, то сообщение поменялось.
        response = self.get_json('/events/', headers=headers, expected_code=403)
        assert_that(
            response['code'],
            equal_to('service_was_disabled')
        )
        assert_that(
            response['message'],
            matches_regexp('Service \(identity: \S*, id: \d*\) was disabled for this organization.')
        )


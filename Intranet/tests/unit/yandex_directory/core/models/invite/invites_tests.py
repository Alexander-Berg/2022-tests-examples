# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    not_none,
    none,
    has_entries,
    calling,
    raises,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models import InviteModel
from intranet.yandex_directory.src.yandex_directory.core.utils.invite import (
    validate_invite,
    CodeDisabled,
)


class TestInviteModel(TestCase):

    def setUp(self):
        super(TestInviteModel, self).setUp()
        self.org_id = self.organization['id']
        self.department_id = self.department['id']
        self.author_id = 10
        self.invite_code = InviteModel(self.meta_connection).create(
            self.org_id,
            self.department_id,
            self.author_id,
        )

    def test_create(self):
        # создаем новую запись
        InviteModel(self.meta_connection).delete(force_remove_all=True)
        author_id = 1
        counter = 10
        code = InviteModel(self.meta_connection).create(
            self.org_id,
            self.department_id,
            author_id,
            counter,
        )

        # в пустой таблице появилась запись
        assert_that(
            InviteModel(self.meta_connection).count(),
            equal_to(1)
        )
        # код неиспользован
        assert_that(
            InviteModel(self.meta_connection).find({'code': code})[0],
            has_entries(
                enabled=True,
                counter=counter,
                author_id=author_id,
                last_use=none(),
            )
        )

    def test_get(self):
        # получаем данные по коду
        assert_that(
            InviteModel(self.meta_connection).get(self.invite_code),
            has_entries(
                code=equal_to(self.invite_code),
                created_at=not_none(),
                enabled=True,
                counter=app.config['DEFAULT_INVITE_LIMIT'],
                last_use=none(),
                author_id=self.author_id,
            )
        )

    def test_valid__disabled(self):
        # если enabled=False, то код недействителен
        invite_code = dict(enabled=False)
        # код не действителен
        assert_that(
            calling(validate_invite).with_args(invite_code),
            raises(CodeDisabled)
        )

    def test_valid__valid(self):
        # код действителен если enabled=True
        invite_code = dict(enabled=True)
        assert_that(
            validate_invite(invite_code),
            equal_to(None)
        )

    def test_use(self):
        # используем код приглашения
        # Должен уменьшиться счетчик инвайтов
        InviteModel(self.meta_connection).use(self.invite_code)

        # запомили дату использования (utc) и кто использовал
        assert_that(
            InviteModel(self.meta_connection).get(self.invite_code),
            has_entries(
                enabled=True,
                counter=app.config['DEFAULT_INVITE_LIMIT'] - 1,
                last_use=not_none()
            )
        )

    def test_use_last_invite(self):
        # используем код приглашения, когда остался последний инвай
        # код должен стать недействительным (enabled=False)
        InviteModel(self.meta_connection).filter(code=self.invite_code).update(counter=1)
        InviteModel(self.meta_connection).use(self.invite_code)

        # запомили дату использования (utc) и кто использовал
        assert_that(
            InviteModel(self.meta_connection).get(self.invite_code),
            has_entries(
                enabled=False,
                counter=0,
                last_use=not_none()
            )
        )

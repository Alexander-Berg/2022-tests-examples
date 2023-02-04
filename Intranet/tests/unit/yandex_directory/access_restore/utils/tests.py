# -*- coding: utf-8 -*-


from datetime import timedelta

from hamcrest import (
    assert_that,
    has_entries,
)
from unittest.mock import patch

from ..... import webmaster_responses
from testutils import (
    TestCase,
    assert_not_called,
)

from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.models.access_restore import (
    RestoreTypes,
    TTL_DAYS,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationAccessRestoreModel,
)
from intranet.yandex_directory.src.yandex_directory.access_restore.utils import (
    init_access_restore_task,
    AccessRestoreTask,
)

class TestInitAccessRestoreTask(TestCase):

    def setUp(self):
        super(TestInitAccessRestoreTask, self).setUp()

        create_params = {
            'domain': self.organization_domain,
            'new_admin_uid': 123,
            'old_admin_uid': self.admin_uid,
            'org_id': self.organization['id'],
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)

    def test_expired(self):
        # если время на передачу истекло то переведем в статус expired заявки в in_progress
        OrganizationAccessRestoreModel(self.meta_connection).filter(
            id=self.first_restore['id']
        ).update(
            expires_at=utcnow() - timedelta(days=TTL_DAYS+1)
        )

        init_access_restore_task()

        assert_that(
            OrganizationAccessRestoreModel(self.meta_connection).get(self.first_restore['id']),
            has_entries(
                state=RestoreTypes.expired,
            )
        )

        for state in [RestoreTypes.success, RestoreTypes.invalid_answers, RestoreTypes.failed]:
            OrganizationAccessRestoreModel(self.meta_connection).filter(
                id=self.first_restore['id']
            ).update(
                state=state
            )

            init_access_restore_task()

            # state не поменялся
            assert_that(
                OrganizationAccessRestoreModel(self.meta_connection).get(self.first_restore['id']),
                has_entries(
                    state=state,
                )
            )

    def test_verified_in_webmaster(self):
        # если домен верифицирован в webmaster запустим задачу не восстановление доступа

        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=True)

        with patch.object(AccessRestoreTask, 'delay') as delay_task:
            init_access_restore_task()

        # ставим задачу на передачу владения
        delay_task.assert_called_once_with(
            restore_id=self.first_restore['id'],
            org_id=self.first_restore['org_id'],
        )


    def test_not_verified_in_webmaster(self):
        # если домен верифицирован в webmaster не ставим задачу не восстановление доступа

        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

        with patch.object(AccessRestoreTask, 'delay') as delay_task:
            init_access_restore_task()

        # не ставим задачу на передачу владения
        assert_not_called(delay_task)

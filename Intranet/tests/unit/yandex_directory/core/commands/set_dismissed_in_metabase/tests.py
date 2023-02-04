# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.core.commands.set_dismissed_in_metabase import Command as SetDismissedInDatabase
from intranet.yandex_directory.src.yandex_directory.core.models.user import (
    UserMetaModel,
    UserModel,
)
from testutils import (
    TestCase,
)
from hamcrest import (
    assert_that,
    equal_to,
)
from intranet.yandex_directory.src.yandex_directory.common.db import (
    get_shard_numbers,
)


class TestSetDismissedInMetabase(TestCase):

    def setUp(self):
        super(TestSetDismissedInMetabase, self).setUp()
        UserMetaModel(self.meta_connection).update(
            {'is_dismissed': None},
            filter_data={
                'is_dismissed': False,
                'id': self.user['id'],
            }
        )
        UserModel(self.main_connection).update(
            {'is_dismissed': False},
            filter_data={
                'is_dismissed': False,
                'id': self.user['id'],
            }
        )

    def test_it_works(self):
        shard = get_shard_numbers()[0]
        uid = self.user['id']
        dismissed, active = SetDismissedInDatabase().proccess_ids([uid], shard)
        assert_that(
            dismissed,
            equal_to(
                set()
            )
        )
        assert_that(
            active,
            equal_to(
                {uid}
            )
        )

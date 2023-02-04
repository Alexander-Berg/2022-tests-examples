# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
)
from unittest.mock import patch

from testutils import TestCase

from intranet.yandex_directory.src.yandex_directory.core.commands.excluded_shards import Command as ExcludedShardsCommand
from intranet.yandex_directory.src.yandex_directory.core.models import ExcludedShardModel


class TestExcludedShards(TestCase):

    create_organization = False

    def test_add_excluded_shard(self):
        # Делаем вид, что у нас 2 шарда (так как нельзя "выключить" последний доступный шард)
        # Добавляем один шард в исключенный
        # Проверяем, что этот шард есть в базе
        with patch('intranet.yandex_directory.src.yandex_directory.core.commands.excluded_shards.get_shard_numbers') as shard_numbers:
            shard_numbers.return_value = [1, 2]
            ExcludedShardsCommand().run(add=2)
            assert_that(
                ExcludedShardModel(self.meta_connection).find(
                    filter_data={'shard': 2},
                    one=True,
                ),
                equal_to(
                    {'shard': 2}
                ),
            )

    def test_delete_excluded_shard(self):
        # Добавляем в базу шард, который хотим "выключить"
        # Удаляем его командой
        # Проверяем, что шарда нет в базе
        model = ExcludedShardModel(self.meta_connection)
        shard = model.create(1)
        assert_that(
            model.find(
                filter_data={'shard': 1},
                one=True,
            ),
            equal_to(shard),
        )
        ExcludedShardsCommand().run(delete=1)
        assert_that(
            model.find(
                filter_data={'shard': 1},
                one=True,
            ),
            equal_to(None),
        )

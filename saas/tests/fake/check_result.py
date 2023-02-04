from typing import Tuple

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.tools.devops.lb_dc_checker.dc_checker.checkers.common import OffsetCheckResult, ShardOffsetCheckResult
from saas.tools.devops.lb_dc_checker.dc_checker.tests.fake.logbroker import LogbrokerDataProvider

fake = Faker()
fake.add_provider(CommonProvider)
fake.add_provider(LogbrokerDataProvider)


class CheckResultProvider(BaseProvider):
    @staticmethod
    def get_shard_offset_check_result(
        shard: Tuple[int, int],
        next_to: ShardOffsetCheckResult = None,
        with_read_session: bool = False
    ) -> ShardOffsetCheckResult:
        partitions_cnt = len(next_to.origin_topic_info.partitions) if next_to else fake.random_int(1, 5)
        origin_topic_info = fake.get_topic_info(
            shard,
            partitions_cnt,
            next_to=next_to.origin_topic_info if next_to else None,
            with_read_session=with_read_session
        )

        return ShardOffsetCheckResult(
            origin_topic_info=origin_topic_info,

            mirror_active_consumer=fake.random_string(10),
            mirror_topic_info=fake.get_topic_info(
                shard,
                partitions_cnt,
                mirror_for=origin_topic_info,
                next_to=next_to.mirror_topic_info if next_to else None,
                with_read_session=with_read_session
            )
        )

    def get_offset_check_result(
        self,
        next_to: OffsetCheckResult = None,
        with_read_session: bool = False
    ) -> OffsetCheckResult:
        shard_to_check_result = {}
        shards_cnt = len(next_to.shard_to_check_result) if next_to else fake.random_int(min=1, max=10)

        for i in range(shards_cnt):
            shard = fake.get_shard(shards_cnt, idx=i)
            shard_id = f'{shard[0]}-{shard[1]}'

            shard_to_check_result[shard_id] = self.get_shard_offset_check_result(
                shard,
                next_to=next_to.shard_to_check_result[shard_id] if next_to else None,
                with_read_session=with_read_session
            )

        return OffsetCheckResult(
            shard_to_check_result=shard_to_check_result
        )

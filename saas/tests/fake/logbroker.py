import re
from typing import Tuple

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.logbroker.internal_api.client import LogbrokerPartitionInfo, LogbrokerTopicInfo
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider

fake = Faker()
fake.add_provider(CommonProvider)


class LogbrokerDataProvider(BaseProvider):
    @staticmethod
    def get_directory() -> str:
        folders = []
        folders_cnt = fake.random_int(min=1, max=5)
        for i in range(folders_cnt):
            len_ = fake.random_int(min=5, max=10)
            folders.append(fake.random_string(len_))
        return '/'.join(folders)

    def get_topic(self, shard: Tuple[int, int]) -> str:
        return f'{self.get_directory()}/shard-{shard[0]}-{shard[1]}'

    def get_read_session_id(self):
        directory = self.get_directory()
        directory = re.sub(r'/', '@', directory)
        tail = f'{fake.random_int(1, 100)}_{fake.random_int(1, 10e9)}'
        return f'{directory}@consumers@{fake.random_int(1, 10)}_{tail}'

    def get_partition_info(
        self,
        mirror_for: LogbrokerPartitionInfo = None,
        next_to: LogbrokerPartitionInfo = None,
        with_read_session: bool = False
    ) -> LogbrokerPartitionInfo:

        if next_to and mirror_for:
            end_min = max(mirror_for.end_offset - 100, next_to.end_offset)
            end_max = min(mirror_for.end_offset, end_min + 100)
        elif next_to:
            end_min = next_to.end_offset
            end_max = end_min + 100
        elif mirror_for:
            end_min = mirror_for.end_offset - 100
            end_max = mirror_for.end_offset
        else:
            end_min = fake.random_int(0, 1000)
            end_max = end_min + 100

        end_offset = max(0, fake.random_int(min=end_min, max=end_max))

        read_min = next_to.read_offset if next_to else fake.random_int(0, end_offset)
        read_offset = fake.random_int(min=read_min, max=end_offset)

        return LogbrokerPartitionInfo(
            end_offset=end_offset,
            read_offset=read_offset,
            commit_time_lag_ms=fake.random_int(min=0, max=10000),
            read_time_lag_ms=fake.random_int(min=0, max=10000),
            read_session_id=self.get_read_session_id() if with_read_session else None
        )

    def get_topic_info(
        self,
        shard: Tuple[int, int],
        partitions_cnt: int,
        mirror_for: LogbrokerTopicInfo = None,
        next_to: LogbrokerTopicInfo = None,
        with_read_session: bool = False
    ) -> LogbrokerTopicInfo:
        topic = next_to.topic if next_to else self.get_topic(shard)
        cluster = next_to.cluster if next_to else fake.random_dc().lower() if mirror_for else 'kafka-bs'

        partitions = []
        for i in range(partitions_cnt):
            partition = self.get_partition_info(
                with_read_session=with_read_session,
                mirror_for=mirror_for.partitions[i] if mirror_for else None,
                next_to=next_to.partitions[i] if next_to else None
            )
            partitions.append(partition)

        return LogbrokerTopicInfo(
            topic,
            cluster,
            partitions
        )

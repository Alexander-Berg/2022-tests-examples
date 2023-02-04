import pytest

from maps_adv.adv_store.v2.lib.core.direct_moderation.schema import (
    DirectModerationIncoming,
    DirectModerationMeta,
)
from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerWrapper
from maps_adv.adv_store.api.schemas.enums import YesNoEnum
from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


class AsyncIter:
    def __init__(self, items):
        self.items = items

    async def __aiter__(self):
        for item in self.items:
            yield item


@pytest.fixture
async def topic_reader():
    class MockTopicReader(LogbrokerWrapper):
        start = coro_mock()
        stop = coro_mock()
        commit = coro_mock()
        finish_reading = coro_mock()

        # unable to set mock value to use in "async for", so implemented this hack
        def set_batch(self, batch):
            self.batch = batch

        def read_batch(self, count):
            return AsyncIter(self.batch)

    return MockTopicReader()


@pytest.fixture
async def logbroker_client(topic_reader):
    class MockLogbrokerClient(LogbrokerWrapper):
        start = coro_mock()
        stop = coro_mock()

        def create_reader(self, topic: str, consumer: str):
            return topic_reader

    return MockLogbrokerClient()


@pytest.mark.parametrize(
    ("raw_message", "parsed_data"),
    [
        (
            b"\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\x03]"
            b"\x92\xcd\x8a\xdb0\x14\x85\xf7\xf3\x18Z;"
            b"\xf1X\x89\x1d\xc8\xbeO0\xabR\x82\xb9c\xddq"
            b"\x04\xb6l$%$\r\x81v\xd6]\x16\xbak_!t\x08"
            b"\x93\xf9\x7f\x85\xeb7\xea\x95=\xc3\xb45"
            b"\x08l\xce\xd1w\xce\x95\xbc\x135z\x10\xf3\x9d("
            b"\xa0nA\x97&\xd7J\xcce$\xd6h\x9dn\x86\xcfd"
            b"\x1f\t\x05\x83O\xd7P\xa2\x98\x8b\xa5\xf7\xad"
            b"\x9b\xc71\xacY\xb0n\\+7\xde\x82Q\xb8\x19"
            b"\x1b\xf4q\x89~Tb\x03j=\xc2\x8d\x8f\xb3t&\xb3X"
            b"\xc2yx\x92\xd9D\xa6\x93\x14 ;W0M.\xb3\xd9"
            b"\x0c\xd5U\x96\xa5\x12R\x88/\xc1\x18\xb4\xf9f*"
            b'"\xe1\xb5\xafB\x18\xfd\xa0\x03\xdd\xd03='
            b"\xf0\xfa\xcd\xeb\x9eE\xa5]Q\x81\xae\xd1\x06"
            b"\xc7w:u_\xe9\x9e\x1dG\xba\xa3G:v_\x02\x00m"
            b"\xedX\xe6\xd7\xa2Y\x19o\xb79\xb7\xea\x87\x12I`"
            b"\xa0+\xacn=O\x1a ?\xe9\xa5\xc7\x1c\xe8\x89Ntd"
            b"\x03\x14Ac\xc4\xa7\x9dX\xd9\x8aM[\x18\xdbU@o"
            b"\xdbP\xadi\xd1\xe4N{\xfc\xbb\xee\xaf\x10\x1f"
            b"\x8at\xd7t\x12\xfbh'\xdaec\x824\x91r$\xe5\xe4}"
            b"\x7f/\xe4\x05T\xd5\xbf\x80g\xba\xed'\xe5\"\xddu"
            b"\xf7\xad\x874\xb6\x04\xa3?\xc3[\xa5$\x92\xd1t"
            b"\x11\x89\xa5v\xbe\xe1\xd1<\x1f\xf5\xdb\xee\xe14"
            b"\x0e\xf4\xf0\x1e\xe5\x10l\xb1\xfc?\xe6\xd58\x84,"
            b"\xf8\xa6-\xbaU\xe5\xc3]\xf3?\xa0t\x11\x90\x1f?\\"
            b"\x88\xa0\x80\x1b\x92\x17\xfb\xfd\xd9\x1f\x99"
            b"\xe8%\xf2<\x02\x00\x00",
            [
                DirectModerationIncoming(
                    meta=DirectModerationMeta(campaign_id=2, version_id=1),
                    verdict=YesNoEnum.YES,
                    reasons=[],
                )
            ],
        ),
        (
            b"\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\x03"
            b"\xed\x94[\x8a\xdb0\x14\x86\xdf\xbb\x0c=;"
            b"\xb1-\xdf\xda,b\xba\x80\x12\xcc\x19Ku\x04"
            b"\xb6l$%$5\x81v\x9e\xfbX\xe8[\xbb\x85\xd0"
            b"\x12\x9a\xceL\xa7[\x90wT]2L\xa6\x0cd\x031"
            b"\x88c\xf1\x1f\xfd\xe7H\xfa\xd0\x80Z\xaa"
            b"\x00\xcd\x06TA\xdb\x03\xaby\xc9\x08\x9a%"
            b"\x01ZQ!Y\xe7\xa7\xf16@\x04|\x1ek\xa1\xa6h"
            b"\x86\x16J\xf5r\x16\x86\xb02\x82\x90\xd3\x96"
            b"\xc8\xe9\x068\xa1\xeb)\xa7*\xac\xa9\x9a"
            b"\xd4\xb4\x03\xb2\x9a\xd0\xb5\n\xf3\xac\xc0y"
            b'\x88!\xb2_\\$8K2\x80<"\x90\xc6\xd7yQP'
            b"\xf2>\xcf3\x0c\x19\x84\xd7\xc09\x15\xe5:E"
            b"\x01RL5\xb6\x98\xfe\xaaw\xfa\xa7~\xd0wf"
            b"\xfc0\xe3\xd6\x88\x84\xc9\xaa\x01\xd6Ra3\xbe"
            b"\xe8\xc3\xf8I\xdf\x9a\x8c\xbd\xfe\xad"
            b"\xef\xf5~\xfch\r\xa8h\xa5\x91\xcdo\xd5-\xb9"
            b"\x12\x9b\xd2t\xe56\x85b\xebAe%X\xaf\xccN"
            b"\xad\xc97\xfd\xd7\xd9\xec\xf4\x1f}\xd0{\x93\x00"
            b"\x95\xd5\x8c\xc5\xbb\x01-Ec\x9260"
            b"\x15Kk\xbd\xe9mk]Oy)\x99\xa2\xa7\xed~\xb7\xe5m#"
            b"\xe3\x8d>\xa0m0\xa0~\xd1q+%\x18O0N\x9e"
            b"\xd6;\xa1\xac\xa0i\x9e\x1b<\xe8_n\xa7\xa6\x91"
            b"\xf1f\xfc\xecL:Q\x03g\x1f\xe0\xb1\xa58"
            b"\xc0A:\x0f\xd0\x82I\xd5\x99\xad)s\xd4\x8f\xab"
            b"\xfdi\xec\xf4\xddS)IAT\x8b\xff\xcb\x1c"
            b"\x13}\x91\xb9\xb9iA\xe5\xb2Q\xf6\xae\r\x03"
            b"\x84U\xd6\xf2\xea-\xb2\x02H_8)p\x14'\x81"
            b"\x0b\xa9\x0b8\xf7\xe18\xf3\x1a\xce|J\xe1BQ"
            b"\xcc\xb7\xdbW\xc3\xcb\xc4\xa5\x17\xe2.\xc4"
            b"\x9d!.z\xe3q\x8a^ .\xc6.D\xaf\xcf\xa3\x96]P"
            b"\xbb\xa0v\x06\xb5\xe3\x03\x16{\x9cp|\xfa\x8e"
            b"\xe1#\x7f\xf8\x04\xb5\x7fR\xfay\x95H\x07\x00\x00",
            [
                DirectModerationIncoming(
                    meta=DirectModerationMeta(campaign_id=3, version_id=1),
                    verdict=YesNoEnum.NO,
                    reasons=[
                        372013,
                        372014,
                        372026,
                        372024,
                        372023,
                        372025,
                        372017,
                        372077,
                    ],
                ),
                DirectModerationIncoming(
                    meta=DirectModerationMeta(campaign_id=4, version_id=1),
                    verdict=YesNoEnum.NO,
                    reasons=[372009, 372010, 372026, 372024, 372012, 372008, 372077],
                ),
                DirectModerationIncoming(
                    meta=DirectModerationMeta(campaign_id=5, version_id=1),
                    verdict=YesNoEnum.NO,
                    reasons=[372023, 372018, 372021, 372017, 372020, 372022, 372077],
                ),
            ],
        ),
    ],
)
async def test_client_retrieves_direct_moderation_responses(
    direct_moderation_client, logbroker_client, topic_reader, raw_message, parsed_data
):
    topic_reader.set_batch([raw_message])

    result = [msg async for msg in direct_moderation_client.retrieve_direct_responses()]
    assert result == parsed_data

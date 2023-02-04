import asyncio
import itertools

from billing.hot.tests.clients.database import client
from billing.hot.tests.config import config
from billing.hot.tests.lib.polling import poll


class ShardedClient:
    def __init__(self, cfg: config.ShardedDatabaseConfig) -> None:
        self.clients = [client.Client(shard_cfg) for shard_cfg in cfg.shards]

    async def connect(self) -> None:
        await asyncio.gather(*[cli.connect() for cli in self.clients])

    async def close(self) -> None:
        await asyncio.gather(*[cli.close() for cli in self.clients])

    async def ping(self) -> list[int]:
        values = await asyncio.gather(*[cli.ping() for cli in self.clients])
        assert not any(isinstance(value, BaseException) for value in values)

        return values

    async def execute(self, command: str) -> list:
        results = await asyncio.gather(*[cli.execute(command) for cli in self.clients])
        assert not any(isinstance(result, BaseException) for result in results)

        return list(itertools.chain(*results))

    async def poll_execute(
        self, command: str, expected_records: int, interval_seconds: float = 1, timeout_seconds: float = 30,
    ) -> list:
        async def poll_body():
            resp = await self.execute(command)
            if len(resp) == expected_records:
                return resp
            raise poll.RetryError

        return await poll.poll(poll_body, interval_seconds=interval_seconds, timeout_seconds=timeout_seconds)

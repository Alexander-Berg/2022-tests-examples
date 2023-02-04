import logging

from billing.hot.tests.lib.polling import poll
from billing.hot.tests.lib.yav import yav
from billing.hot.tests.config import config

from aiopg.sa import create_engine

logger = logging.getLogger(__name__)


class Client:
    def __init__(self, cfg: config.DatabaseConfig) -> None:
        self.config = cfg
        self.engine = None

    async def connect(self) -> None:
        dsn = self.config.dsn or self._create_dsn()

        self.engine = await create_engine(dsn)

    def _create_dsn(self):
        return (
            f'dbname={self.config.db_name} user={self.config.user} '
            f'password={self._get_password()} host={self.config.host} port={self.config.port}'
        )

    def _get_password(self):
        secret_provider = yav.SecretProvider()
        return secret_provider.get_secret(self.config.password_secret.id, self.config.password_secret.key)

    async def ping(self) -> int:
        async with self.engine.acquire() as conn:
            async for row in conn.execute('SELECT 1'):
                return row[0]
        return 0

    async def execute(self, command: str) -> list:
        result = []
        logger.info(f"sql execute command: {command}")
        async with self.engine.acquire() as conn:
            async for row in conn.execute(command):
                result.append(row)
        logger.info(f"sql execute result: {result}")
        return result

    async def poll_execute(
        self, command: str, expected_records: int, interval_seconds: float = 1, timeout_seconds: float = 30,
    ) -> list:
        async def poll_body():
            resp = await self.execute(command)
            if len(resp) == expected_records:
                return resp
            raise poll.RetryError

        return await poll.poll(poll_body, interval_seconds=interval_seconds, timeout_seconds=timeout_seconds)

    async def close(self) -> None:
        self.engine.close()
        await self.engine.wait_closed()

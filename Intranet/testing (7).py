from pydantic import Field

from src.config.development import Settings as DefaultSettings


class Settings(DefaultSettings):
    ENV_TYPE: str = 'testing'

    POSTGRES_HOST: str = Field(
        'vla-3tvp3uw1veotynjv.db.yandex.net',
        env='POSTGRES_HOST',
    )
    POSTGRES_PORT: int = Field(6432, env='POSTGRES_PORT')

    STAFF_API_HOST: str = Field(
        'staff-api.test.yandex-team.ru',
        env='STAFF_API_HOST',
    )

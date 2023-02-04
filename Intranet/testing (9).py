from pydantic import Field

from src.config.development import Settings as DefaultSettings


class Settings(DefaultSettings):
    ENV_TYPE: str = 'testing'

    POSTGRES_HOST: str = Field('vla-3tvp3uw1veotynjv.db.yandex.net', env='POSTGRES_HOST')
    POSTGRES_PORT: int = Field(6432, env='POSTGRES_PORT')

    DATASWAMP_POSTGRES_HOST: str = Field('vla-3tvp3uw1veotynjv.db.yandex.net', env='DATASWAMP_POSTGRES_HOST')
    DATASWAMP_POSTGRES_PORT: int = Field(6432, env='DATASWAMP_POSTGRES_PORT')

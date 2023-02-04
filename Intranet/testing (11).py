from .default import Settings as DefaultSettings


class Settings(DefaultSettings):
    ENV_TYPE: str = 'testing'

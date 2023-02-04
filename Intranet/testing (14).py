from .default import Settings as DefaultSettings
from .duty_constants import TestingConstants


class Settings(DefaultSettings, TestingConstants):
    ENV_TYPE: str = 'testing'
    DEBUG: bool = False

from .default import Settings as DefaultSettings


class Settings(DefaultSettings):
    ENV_TYPE: str = 'testing'
    DEBUG: bool = False
    SKIP_TRIP_APPROVE: bool = True
    ENABLE_ERROR_BOOSTER: bool = True
    ERROR_BOOSTER_PROJECT_ID: str = 'trip-back-testing'
    LOGBROKER_ERROR_BOOSTER_TOPIC: str = '/trip/test-error-booster'

    ENABLE_CHATS: bool = False

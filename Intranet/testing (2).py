from .base import Settings as BaseSettings
from typing import List


class Settings(BaseSettings):
    debug: bool = True

    db_pool_min_size: int = 50
    db_pool_max_size: int = 400

    redis_host: str = 'sas-cpcltfea2y9q8bi1.db.yandex.net'
    redis_port: str = '26379'

    logbroker_write_endpoint = 'logbroker.yandex.net'
    logbroker_port: str = '2135'
    logbroker_topic = 'domenator_logbroker/test/domenator-topic'

    users_with_emulation: List[str] = ['4079090662']
    users_allowed_to_set_register_ts: List[str] = ['4079090662']

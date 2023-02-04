from datetime import datetime
from typing import Any


def gen_diod_key(
    key: str,
    namespace: str,

    value: str = 'processed',
    revision: int = 1,
    created: bool = True,
    service_id: str = 'service_id',
    created_at: datetime = datetime(2022, 1, 1),
    updated_at: datetime = datetime(2022, 1, 1),
) -> dict:

    return {
        'key': key,
        'namespace': namespace,

        'value': value,
        'revision': revision,
        'created': created,
        'service_id': service_id,
        'created_at': created_at,
        'updated_at': updated_at,
    }


def gen_post_diod_key(namespace: str, key: str, value: Any = '1', immutable=True) -> dict:
    return {'namespace': namespace, 'key': key, 'value': value, 'immutable': immutable}

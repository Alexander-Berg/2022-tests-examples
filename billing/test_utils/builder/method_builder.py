from datetime import datetime
from typing import Any, Optional

import arrow


_dt_with_tz = arrow.get(datetime(2020, 1, 1, 10, 54, 0)).datetime


def gen_migration_info(
    namespace: str = 'trust',
    filter: str = 'Firm',
    object_id: int = 13,
    from_dt: datetime = _dt_with_tz,
    dry_run: bool = True,
) -> dict[str, Any]:
    return {
        'namespace': namespace,
        'filter': filter,
        'object_id': object_id,
        'from_dt': from_dt,
        'dry_run': dry_run,
    }


def gen_lock_loc(type: str = 'cutoff_dt_lock', client_id: int = 1, namespace: str = 'trust') -> dict[str, Any]:
    return {'type': type, 'client_id': client_id, 'namespace': namespace}


def gen_state(loc: Optional[dict] = None, state: Any = 'cutoff_dt_state') -> dict[str, Any]:
    return {'loc': loc or gen_lock_loc(), 'state': state}


def gen_lock(states: Optional[list[dict]] = None) -> dict[str, Any]:
    return {'states': states}

import pytest
from django.core.exceptions import ValidationError

from mdh.core.toolbox.logbroker import schedule_resync


def test_schedule_resync(init_lock):

    assert schedule_resync() == {}  # нет параметров
    assert schedule_resync(references=['my_ref']) == {}  # нет задания

    lock = init_lock(
        'logbroker_send_record',
        result='{"since": "2021-01-11T00:00:00+00:00"}',
    )

    result = schedule_resync(references=['my_ref'])

    assert result == {
        'attrs': {},
        'since': '2021-01-11T00:00:00+00:00',
        'domains': [],
        'references': ['my_ref'],
        'records': []
    }

    updated = lock.dt_upd
    lock.refresh_from_db()
    assert lock.dt_upd > updated

    # невалидный uuid
    with pytest.raises(ValidationError):
        schedule_resync(records=['66ce874e-0281-4fb8-8e06-2847deae6e'])

    # валидный uuid
    uuid = '2075b52d-4083-4ee6-a30a-7c3b79e9b3ac'
    result = schedule_resync(records=[uuid])
    assert uuid in result['records']

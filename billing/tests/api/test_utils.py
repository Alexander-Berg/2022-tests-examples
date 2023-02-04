import json
from datetime import datetime

from ift.api.utils import transform_data, value_in, parse_datetime, json_strip_comments


def test_json_strip_comments():

    data = (
        b' {\n'
        b'"a": "b", // comme //nt\n'
        b'  // another comment\n'
        b'// more\n'
        b'"c": [1, // one\n'
        b'5 // five\n'
        b'],  \n'
        b'"d": "\xd0\x97\xd0\xb0\xd0\xb4\xd0\xb0\xd1\x87\xd0\xb0"  }  '
    )

    assert json.loads(json_strip_comments(data)) == {'a': 'b', 'c': [1, 5], 'd': 'Задача'}
    assert json_strip_comments(b'bogus') == b'bogus'


def test_transform(get_track_req):

    service_id = 1
    environment_id = 2

    # Новое событие.
    transformed = transform_data(event='', data=get_track_req(), service_id=service_id, environment_id=environment_id)
    assert 'startTime' in transformed
    assert 'finishTime' not in transformed
    assert transformed['severity'] == 'minor'
    assert transformed['type'] == 'issue'

    data = get_track_req()
    data['tracker']['status'] = 'Закрыт'
    transformed = transform_data(event='', data=data, service_id=service_id, environment_id=environment_id)
    assert 'finishTime' in transformed
    assert transformed['severity'] == 'minor'
    assert transformed['type'] == 'issue'

    # Старое событие. Обновление.
    transformed = transform_data(event='123', data=get_track_req(), service_id=service_id, environment_id=environment_id)
    assert 'startTime' not in transformed
    assert 'finishTime' not in transformed
    assert transformed['severity'] == 'minor'
    assert transformed['type'] == 'issue'

    # Подъём критичности.
    data = get_track_req()
    data['tracker']['priority'] = 'Критичный'
    transformed = transform_data(event='', data=data, service_id=service_id, environment_id=environment_id)
    assert transformed['severity'] == 'major'

    # Смена типа события.
    data = get_track_req()
    data['tracker']['type'] = 'Обслуживание'
    transformed = transform_data(event='', data=data, service_id=service_id, environment_id=environment_id)
    assert transformed['type'] == 'maintenance'

    # Проброс значения напрямую.
    data = get_track_req()
    data['infra']['title'] = 'Заголовок'
    data['infra']['description'] = 'Описание'
    data['infra']['severity'] = 'major'
    transformed = transform_data(event='', data=data, service_id=service_id, environment_id=environment_id)
    assert transformed['title'] == 'Заголовок'
    assert transformed['description'] == 'Описание'
    assert transformed['severity'] == 'major'


def test_value_in():
    assert value_in('oNe', ['One', 'two'])
    assert value_in('one', ['*'])
    assert value_in('one', [' '], default={'one'})


def test_parse_datetime():

    ts = 1614594720

    result = parse_datetime('2021-03-01T10:32:00.000Z')
    assert result == ts

    dt = datetime.fromtimestamp(ts)
    assert dt.strftime('%Y-%m-%d %H:%M') == '2021-03-01 10:32'

    assert parse_datetime(12345) == 12345

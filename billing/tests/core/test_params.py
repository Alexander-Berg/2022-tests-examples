from dwh.grocery.tools.parameters import UpdateIdParameter


def test_update_id(time_freeze):

    parse = UpdateIdParameter.parse_value

    with time_freeze('2022-02-05'):

        # сдвиги по месяцам
        assert parse('now+2mo').startswith('2022-04-04T21')
        assert parse('now-4mo').startswith('2021-10-04T21')

        # по дням
        assert parse('now-2d').startswith('2022-02-02T21')
        assert parse('now+2d').startswith('2022-02-06T21')

        # по часам
        assert parse('now-5h').startswith('2022-02-04T16')
        assert parse('now+2h').startswith('2022-02-04T23')

        # неудача при разборе. оставляем значение как есть
        assert parse('now-4xx') == 'now-4xx'

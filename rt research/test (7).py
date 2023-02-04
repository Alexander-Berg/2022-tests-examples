import responses
import irt.common.staff_adapter


@responses.activate
def test_staff_adapter():
    url = 'https://staff-api.yandex-team.ru/v3/persons?_query=accounts%3D%3Dmatch%28%7B%22type%22%3A+%22telegram%22%2C+%22value_lower%22%3A+%22{}%22%7D%29&_fields=login%2Caccounts'

    responses.add(responses.GET, url.format('good_account'),
                  json={
                      'links': {},
                      'page': 1,
                      'limit': 50,
                      'result': [
                          {'login': 'staff_good',
                           'accounts': [
                               {'value': 'good_account', 'type': 'telegram', 'private': True, 'value_lower': 'good_account', 'id': 90000}
                           ]}],
                      'total': 1,
                      'pages': 1},
                  status=200)
    responses.add(responses.GET, url.format('bad_account'),
                  json={'links': {}, 'page': 1, 'limit': 50, 'result': [], 'total': 0, 'pages': 0},
                  status=200)

    staff_adapter = irt.common.staff_adapter.StaffAdapter('')

    assert staff_adapter.get_user_by_telegram('good_account')['login'] == 'staff_good'
    assert staff_adapter.get_user_by_telegram('bad_account') is None

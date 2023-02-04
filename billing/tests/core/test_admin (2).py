import pytest
from django.core.files.uploadedfile import SimpleUploadedFile

from mdh.core.models import SupportRole


def test_basic(client, init_user):

    # Неавторизован.
    response = client.get('/admin/', follow=True).rendered_content
    assert '/admin/login/' in response
    assert 'not authorized to access' not in response

    # Авторизован, нет прав.
    user = init_user()
    response = client.get('/admin/', follow=True).rendered_content
    assert 'not authorized to access' in response

    # Есть права.
    user.role_add(roles=[SupportRole], author=user)
    response = client.get('/admin/', follow=True).rendered_content
    assert '/admin/core/' in response


def test_schema_details(client, init_user, init_schema):

    user = init_user(roles=[SupportRole])
    schema = init_schema('myschema', user=user)
    response = client.get(f'/admin/core/schema/{schema.id}/change/', follow=True).rendered_content
    assert 'Display schema:' in response
    assert 'Display schema json:' in response


def test_resync_records(client, init_user, init_lock):

    user = init_user(roles=[SupportRole])

    def post(attrs: str = '{"attr1": false, "attr2": "some"}') -> str:
        response = client.post(
            '/admin/admin/resyncpage/',
            data={
                'domains': 'a,b',
                'references': 'c,d',
                'records': '2075b52d-4083-4ee6-a30a-7c3b79e9b3ac,2075b52d-4083-4ee6-a30a-7c3b79e9b3a1',
                'attrs': attrs,
            },
            follow=True
        ).rendered_content
        assert 'Выгрузка в Logbroker' in response
        return response

    # Проверка обработки исключения (из-за неправильного json).
    response = post(attrs='{attr1=22}')
    assert 'Неправильно сформирована json-строка' in response

    # Проверка обработки исключения (из-за незарегистрированного задания).
    response = post()
    assert 'завершилось неудачей' in response

    lock = init_lock('logbroker_send_record', result='{}')

    # Успешная планировка.
    response = post()
    assert 'запланирована успешно' in response

    lock.refresh_from_db()
    assert lock.result == (
        '{"domains": ["a", "b"], '
        '"references": ["c", "d"], '
        '"records": ["2075b52d-4083-4ee6-a30a-7c3b79e9b3ac", "2075b52d-4083-4ee6-a30a-7c3b79e9b3a1"], '
        '"attrs": {"attr1": false, "attr2": "some"}}'
    )


@pytest.mark.parametrize('clonepage', [
    'csvclonepage',
    'csvupgradepage',
])
def test_csv_clone_records(clonepage, client, init_user, init_records):

    init_user(robot=True)  # используется клонировщиком
    init_user(roles=[SupportRole])  # представляемся этим пользователем в клиенте

    init_records('my', ids=[
        'bf6040b2-4686-454d-bdc7-1031750bd01a',
        'f92a5a3c-3f84-466d-b2f3-3593ec463e6c',
    ])

    def post(data: bytes, *, encoding: str = 'utf-8') -> str:
        response = client.post(
            f'/admin/admin/{clonepage}/',
            data={
                'encoding': encoding,
                'csv': SimpleUploadedFile('my.csv', data)
            },
            follow=True
        ).rendered_content
        return response

    out = post(b'my___master_uid,hint,string1___old,string1___new,integer1___old,integer1___new\n')
    assert 'Обработано записей — 0' in out

    out = post(
        b'my___master_uid,hint,string1___old,string1___new,integer1___old,integer1___new\n'
        b'f92a5a3c-3f84-466d-b2f3-3593ec463e6c,hehe,some,' + 'что-то'.encode('cp1251') + b',0,22\n'
    )
    assert 'Не удалось декодировать данные из файла' in out

    out = post(
        b'my___master_uid,hint,string1___old,string1___new,integer1___old,integer1___new\n'
        b'bf6040b2-4686-454d-bdc7-1031750bd01a,one,,myvalue1,0,22\n'
        b'f92a5a3c-3f84-466d-b2f3-3593ec463e6c,two,,myvalue1,0,22\n'
        b'f92a5a3c-3f84-466d-b2f3-3593ec463e6c,three,,myvalue2,0,55\n'
    )
    assert 'Обработано записей — 3' in out

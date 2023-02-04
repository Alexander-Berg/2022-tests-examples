import pytest
from django.urls import reverse

from dwh.core.models import Work


def test_index(client, init_user, init_work):
    response = client.get('/', follow=True)
    assert b'Not Found' in response.content

    # Авторизован.
    init_user()
    init_work()
    response = client.get('/', follow=True)
    assert response.status_code == 200
    assert '<td>echo</td>' in response.content.decode()


def test_work_details(client, init_user, init_work):
    init_user()

    work = init_work({'meta': {'task_name': 'echo', 'hint': 'some [link](/aa/) goes\n\nnewline'}, 'params': {'a': 1}})
    url_details = reverse('work_details', kwargs={'work_id': work.id})
    response = client.get(url_details, follow=True)
    assert response.status_code == 200
    content = response.content.decode()
    assert '<td>ID запроса: <b>143456</b></td>' in content
    assert '<p>some <a href="/aa/">link</a> goes</p>' in content

    # проверим клонирование
    response = client.post(url_details, data={
        'action': 'clone',
    }, follow=True)
    assert response.status_code == 200
    assert Work.objects.order_by('-id').first().id > work.id

    # проверим планировку остановки
    assert not work.dt_stop
    work.status = work.Status.STARTED
    work.save()

    response = client.post(url_details, data={
        'action': 'stop',
    }, follow=True)
    assert response.status_code == 200
    work.refresh_from_db()
    assert work.dt_stop
    assert work.stopper

    # проверим вывод данных об остановившем
    content = client.get(url_details, follow=True).content.decode()
    assert 'остановил: <b class="ya-title"><span data-login="tester">tester' in content

    # Проверяем связывание работ.
    work_2 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 2}})
    work_3 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 3}})

    work.next = work_2
    work.save()

    work_2.next = work_3
    work_2.save()

    content = client.get(reverse('work_details', kwargs={'work_id': work_2.id}), follow=True).content.decode()
    assert 'Вытесняет работу' in content
    assert 'Вытеснена работой' in content


def test_check_error(client, init_user):

    response = client.get('/errcheck', follow=True)
    assert b'Not Found' in response.content

    # Авторизован.
    init_user()

    with pytest.raises(RuntimeError) as e:
        client.get('/errcheck', follow=True)

    assert str(e.value) == 'errboo test'


def test_ping(monkeypatch, client):

    ok = False

    class DummyConnection:

        def connect(self):
            return

        def is_usable(self):
            if not ok:
                raise RuntimeError('wrong')
            return ok

    monkeypatch.setattr('dwh.core.views.connection', DummyConnection())

    assert b'failure' in client.get('/ping').content

    ok = True
    assert b'success' in client.get('/ping').content


def test_tasks_list(client, init_user):
    init_user()

    url_details = reverse('tasks_list')
    content = client.get(url_details, follow=True).content.decode()
    assert 'echo' in content
    assert '>String</span>' in content
    assert '>DateInterval</span>' in content
    assert 'Словарь метаданных по таблицам экспорта' in content


def test_stat(client, init_user, init_work):
    init_user()

    work = init_work()
    work.status = work.Status.STARTED
    work.host = 'myhost'
    work.save()

    url_details = reverse('stat')
    response = client.get(url_details, follow=True)
    assert response.status_code == 200
    content = response.content.decode()

    assert '<b>myhost</b> <sup>1</sup>' in content

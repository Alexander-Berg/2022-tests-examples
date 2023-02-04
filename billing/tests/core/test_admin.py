from dwh.core.admin import CmdConvertPage
from dwh.core.models import Work, Interruption


def test_basic(client, init_user):

    # Неавторизован.
    response = client.get('/admin/', follow=True).rendered_content
    assert '/admin/login/' in response
    assert 'not authorized to access' not in response

    # Авторизован, нет прав.
    user = init_user()
    response = client.get('/admin/', follow=True).rendered_content
    assert 'недостаточно прав для просмотра данной страницы' in response

    # Есть права.
    user.roles = {'support': {}}
    user.save()
    response = client.get('/admin/', follow=True).rendered_content
    assert '/admin/core/' in response


def test_works(client, init_user, init_work):
    init_user(support=True)

    work = init_work()

    # детализация по работе
    response = client.get(f'/admin/core/work/{work.id}/change/', follow=True).rendered_content
    assert '<div class="readonly">143456</div>' in response

    # список работ
    response = client.get('/admin/core/work/', follow=True).rendered_content
    assert '<td class="field-name">echo</td>' in response

    # клонирование работы
    response = client.post('/admin/core/work/', data={
        'action': 'clone',
        '_selected_action': f'{work.id}',
    }, follow=True).rendered_content
    assert 'Клонирование произведено успешно' in response


def test_interruptions(client, init_user):
    init_user(support=True)

    target_interruped = 'myinter'

    interruption = Interruption.objects.create(target=target_interruped)

    # детализация
    response = client.get(f'/admin/core/interruption/{interruption.id}/change/', follow=True).rendered_content
    assert f'<div class="readonly">{target_interruped}</div>' in response

    # список
    response = client.get('/admin/core/interruption/', follow=True).rendered_content
    assert f'{target_interruped}</a>' in response


def test_addwork(client, init_user):
    init_user(support=True)

    def add(body: str):
        response = client.post(
            '/admin/admin/addworkpage/',
            data={'body': body},
            follow=True
        ).rendered_content
        return response

    # ошибка в данных
    response = add('{"bogus": "uuu"}')
    assert 'validation errors for InputSchema' in response
    assert '{&quot;bogus&quot;:' in response

    # удачная регистрация
    response = add('{"meta": {"task_name": "echo"}, "params": {"a": "10"}}')
    assert 'Работа успешно зарегистирована к исполнению' in response

    work = Work.objects.first()
    assert work.input == {
        'meta': {'task_name': 'echo', 'workers': 1, 'timeout': 0, 'retries': 1, 'hint': ''}, 'params': {'a': '10'}}
    assert work.remote == 'tester'


def test_cmd_convert(client, init_user):
    init_user(support=True)

    def convert(cmd: str):
        response = client.post(
            '/admin/admin/cmdconvertpage/',
            data={'cmd': cmd},
            follow=True
        ).rendered_content
        return response

    candidates = {

        '/usr/bin/dwh/run_with_env.sh -m luigi --module grocery.dwh-512 DWH512 '
        '--workers 8 --local-scheduler --with-boost nirvana': {
            'meta': {'task_name': 'DWH512', 'workers': 8},
            'params': {'with_boost': 'nirvana'}
        },

        '/usr/bin/dwh/run_with_env.sh -m luigi --module grocery.dwh-671 DWH671 '
        '--local-scheduler --workers 4 --cluster arnold': {
            'meta': {'task_name': 'DWH671', 'workers': 4},
            'params': {'cluster': 'arnold'}
        },

        '''"/usr/bin/dwh/run_with_env.sh -m luigi --module grocery.yt_export YTExport
        --tables '["t_thirdparty_transactions"]' --local-scheduler  --transfer-to-clusters '[""hahn"",""arnold""]'
        --workers 16 --scheduler-retry-count 5 --worker-max-reschedules 3 --scheduler-disable-window 4
        --update-id 'now+1mo'"''': {
            'meta': {
                'task_name': 'YTExport',
                'workers': 16,
                'scheduler_retry_count': 5,
                'worker_max_reschedules': 3,
                'scheduler_disable_window': 4,
            },
            'params': {
                'tables': ['t_thirdparty_transactions'],
                'transfer_to_clusters': ['hahn', 'arnold'],
                'update_id': 'now+1mo'
            }
        },
    }

    for cmd, expected in candidates.items():
        assert CmdConvertPage.convert(cmd) == expected

    # ошибка в данных
    response = convert('{"bogus": "uuu"}')
    assert "Task &#x27;&#x27; is unknown." in response

    # удачная конвертация
    response = convert(
        '''/usr/bin/dwh/run_with_env.sh -m luigi --module grocery.yt_export YTExport '''
        '''--tables '["bpmonline_ext_lb_yacalls_cdr_full"]' --local-scheduler '''
        '''--workers 1 --bunker-path export_cfg/crm''')

    assert ', &quot;bunker_path&quot;: &quot;export_cfg/crm&quot;' in response

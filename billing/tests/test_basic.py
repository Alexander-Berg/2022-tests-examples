import logging
from os import environ

import pytest

try:
    from billing.library.tools.baf.baf.commands.app import parse_logfile
except ImportError:
    from baf.commands.app import parse_logfile

DCTL_LIST_RESULT = {'out': '\n|vla|12345|zz1\nip6_address: "1.2.3.4"'}
DCTL_LIST_COMMANDS = [
    'ya tool dctl list pod myapp-test-stage.backend',
    'ya tool dctl status pod -c vla 12345',
]


class MockGroup:

    def __init__(self):
        self.hosts_all = []
        self.command = []

    def __call__(self, *hosts):
        self.hosts_all.extend(hosts)
        return self

    def run(self, cmd):
        self.command.append(cmd)


@pytest.fixture
def mock_remote_run(monkeypatch, arcadia_prefix: str):
    mock = MockGroup()
    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.remote.ThreadingGroup', mock)
    return mock


def test_release(run_command_mock, response_mock):

    with response_mock(
        'GET https://registry.yandex.net/v2/balance/myapp/tags/list -> 200:'
        '{"tags": ["0.0.1", "0.3.1"]}'
    ):
        assert run_command_mock('release') == [
            'Мы готовы к тому, чтобы отправить MYAPP 0.3.1 (balance/myapp:0.3.1) на выпуск в Deploy.',
            'ya tool dctl release docker --title "MYAPP 0.3.1" --description "MYAPP 0.3.1" -t 0.3.1 balance/myapp']


def test_image_push(run_command_mock):
    result = run_command_mock(
        'image-push',
        run_result={'err': "'registry.yandex.net/balance/myapp:1.2.5'"}
    )
    assert len(result) == 2
    assert result[1] == 'ya package package.json --docker --docker-push --docker-repository "balance"'

    # проверим раскатку
    result = run_command_mock(
        'image-push',
        '--rollout',
        run_result={
            'err': "'registry.yandex.net/balance/myapp:1.2.5'",
            'out': '''spec:
  deploy_units:
    backend:
      images_for_boxes:
        backend:
          tag: 0.0.211224154803-idlesign
      revision: 60
  revision: 60
  revision_info: {}''',
        }
    )
    assert len(result) == 4
    assert (
        'ya package package.json --docker --docker-push --docker-repository "balance"'
        'ya tool dctl get stage myapp-test-stage'
        'ya tool dctl put stage'
    ) in ''.join(result)


def test_log(run_command_mock, mock_remote_run):

    assert run_command_mock('log', run_result=DCTL_LIST_RESULT) == DCTL_LIST_COMMANDS

    assert mock_remote_run.command == ['tail -f /porto_log_files/myapp_*.portolog']
    assert mock_remote_run.hosts_all == ['root@1.2.3.4']


def test_app_stop(run_command_mock, mock_remote_run):

    assert run_command_mock('app.stop', run_result=DCTL_LIST_RESULT) == DCTL_LIST_COMMANDS

    assert mock_remote_run.command == ['supervisorctl stop app']
    assert mock_remote_run.hosts_all == ['root@1.2.3.4']


def test_app_restart(run_command_mock, mock_remote_run):

    assert run_command_mock('app.restart', run_result=DCTL_LIST_RESULT) == DCTL_LIST_COMMANDS

    assert mock_remote_run.command == ['supervisorctl restart app']
    assert mock_remote_run.hosts_all == ['root@1.2.3.4']


def test_logfind(run_command_mock, response_mock):

    with response_mock(
        ''
    ):
        result = run_command_mock(
            'logfind', 'something'
        )
        assert len(result) == 1
        assert 'grep -i   "something" -R ' in result[0]


def test_config(run_command_mock, capsys):
    run_command_mock('config')
    out = capsys.readouterr().out

    # дополнено автоматом
    assert '"docker_repo": "balance",' in out
    assert '"myapp-test-stage.backend"' in out
    assert '"logfile": "/porto_log_files/myapp_*.portolog",' in out


def test_event(run_command_mock, monkeypatch, arcadia_prefix: str):
    opened = []

    def open_mock(*args):
        opened.append(args[0])

    monkeypatch.setattr(f'{arcadia_prefix}baf.utils.open_new_tab', open_mock)

    run_command_mock('event', '123')
    assert ': 123' in opened[0]


@pytest.fixture
def exec_mock(monkeypatch, arcadia_prefix: str):
    """Имитатор для os.execvp"""
    fired = []

    def execvp_mock(*args):
        fired.append(args)

    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.deploy.os.execvp', execvp_mock)

    return fired


def test_ssh(run_command_mock, exec_mock):

    # любой хост
    run_command_mock('ssh', run_result=DCTL_LIST_RESULT)
    assert exec_mock == [('ssh', ['ssh', 'root@1.2.3.4'])]
    exec_mock.clear()

    # из указанного датацентра
    run_command_mock('ssh', '--dc', 'vla', run_result=DCTL_LIST_RESULT)
    assert exec_mock == [('ssh', ['ssh', 'root@1.2.3.4'])]


def test_pod_state(run_command_mock, exec_mock):

    run_command_mock('deploy.pod-state', 'active', 'vla', run_result=DCTL_LIST_RESULT)
    assert exec_mock == []


def test_stage_spec(run_command_mock, exec_mock):

    run_command_mock('deploy.stage-spec', run_result=DCTL_LIST_RESULT)
    assert exec_mock == []


def test_manage(run_command_mock, exec_mock):

    run_command_mock('manage', 'check', run_result=DCTL_LIST_RESULT)
    assert exec_mock == [('ssh', ['ssh', 'root@1.2.3.4', 'myapp', 'check'])]


def test_format_log_terminal(datafix_dir, datafix_read, run_command_mock, capsys):

    sample_path = str(datafix_dir / 'sample.log')
    logrepr = parse_logfile(sample_path)
    formatted = logrepr.format()

    assert formatted.strip() == datafix_read('sample_expected.log').strip()

    run_command_mock('logformat', sample_path)
    out = capsys.readouterr().out
    assert 'Traceback (most recent call last)' in out


def test_format_log_html(datafix_dir, datafix_read, run_command_mock, capsys):

    run_command_mock('logformat', str(datafix_dir / 'sample.log'), '--fmt=html')
    out = capsys.readouterr().out
    assert '<code>mdh.core.integration.solomon: ' in out


def test_log_l7(run_command_mock, mock_remote_run, response_mock):

    with response_mock(
        'POST https://awacs.yandex-team.ru/api/ListEndpointSets/ -> 200:'
        '{"endpointSets": [{'
        '"meta": {"id": "abcd"}, '
        '"spec": {"instances": [{"host": "some.com"}]}'
        '}]}'
    ):
        assert run_command_mock('log-l7') == []

    assert mock_remote_run.command == ['tail -f /logs/current-access_log-balancer-443']


def test_monitoring(run_command_mock, monkeypatch, arcadia_prefix: str):
    opened = []

    def open_mock(*args):
        opened.append(args[0])

    monkeypatch.setattr(f'{arcadia_prefix}baf.utils.open_new_tab', open_mock)

    run_command_mock('mon', '--dc', 'vla')
    assert len(opened) == 3
    assert 'geo=vla' in opened[0]
    assert 'myapp-test-stage' in opened[0]
    assert 'myapp/' in opened[1]
    assert 'myapp/' in opened[2]
    assert '%20testing' in opened[2]


def test_batch(run_command_mock, monkeypatch, tmp_path, caplog, arcadia_prefix: str):
    executed = []

    def run_mock(*args, **kwargs):
        executed.append((args, kwargs))

    for path in [tmp_path / 'one/baf.yml', tmp_path / 'two/src/baf.yml']:
        path.parent.mkdir(exist_ok=True, parents=True)
        path.write_text('xxx')

    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.batching.call', run_mock)
    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.batching.run', run_mock)
    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.batching.which', lambda cmd: cmd)

    cmd = ['batch', '"config"', '--projects', 'one,/some/here,two', '--basedir', f'{tmp_path}']

    run_command_mock(*cmd)
    assert executed == [(('baf "config"',), {}), (('baf "config"',), {})]

    executed.clear()
    cmd.append('--detach')
    run_command_mock(*cmd)

    assert executed == [
        (('terminator --new-tab -e "baf "config"; exec $SHELL"',), {'shell': True}),
        (('terminator --new-tab -e "baf "config"; exec $SHELL"',), {'shell': True})
    ]

    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.batching.which', lambda cmd: False)
    executed.clear()
    run_command_mock(*cmd)

    messages = caplog.messages
    assert 'No baf.yml found for /some/here. Skipped.' in messages
    assert 'Your terminal emulator required for the detach mode is not supported.' in messages


def test_unit_ips(run_command_mock, exec_mock, capsys):

    run_command_mock('deploy.unit-ips', run_result={
        'out': (
            # вывод спеки
            '\n'
            '- cluster: vla\n'
            'ip4_address_pool_id: 1:2:3:4\n'

            # вывод от yp
            '32 object(s) selected\n'
            '+-------------------+\n'
            '| /spec/ip4_address |\n'
            '+-------------------+\n'
            '|   "5.5.5.5"  |\n'
            '|  "6.6.6.6"  |\n'
        )
    })
    assert exec_mock == []
    out = capsys.readouterr().out

    assert '5.5.5.5\n6.6.6.6' in out


def test_secrets_sync_smoke(run_command_mock, response_mock, caplog):
    # проверка всей команды без деталей

    environ['MY_SECRET_VAL'] = 'here'
    environ['DO_ITEM2'] = 'there'
    environ['DO_MY_ITEM1'] = 'dummy'

    caplog.set_level(logging.INFO)

    requests_create = [
        # обновляем роли
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/roles/ -> 200:{}',

        # создаём секрет
        'POST https://vault-api.passport.yandex.net/1/secrets/ -> 200:'
        '{"uuid": "sec-dummy1020"}',

        # создаём версию
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-dummy1020/versions/ -> 200:'
        '{"secret_version": "ver-dummy200"}',

        # добавляем роли
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-dummy1020/roles/ -> 200: {}'
    ]

    # проверяем случай, когда у первого секрета уже есть версия
    with response_mock([
        # берём последнюю версию
        'GET https://vault-api.passport.yandex.net/1/versions/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"version": {'
        '"secret_name": "my-secret", '
        '"secret_uuid": "sec-01g53bh2e6zzfjdp54hdfa442n", '
        '"version": "ver-dummy100", '
        '"value": [{"key": "item1", "value": "newone"}]'
        '}}',

        # получаем метаданные секрета
        'GET https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"secret": {'
        '"name": "oldname",'
        '"uuid": "sec-01g53bh2e6zzfjdp54hdfa442n",'
        '"comment": "oldcomment",'
        '"tags": ["oldtag1", "oldtag2"],'
        '"secret_roles": [{"login": "idlesign", "role_slug": "OWNER"}, {"abc_id": "123", "abc_scope": "development", "role_slug": "READER"}]'
        '}}',

        # обновляем метаданные
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:{}',

        # основываемся на существующей версии
        'POST https://vault-api.passport.yandex.net/1/versions/ver-dummy100/ -> 200: '
        '{"version": "ver-dummy188"}',

    ] + requests_create):
        assert run_command_mock('secrets-sync', '--update-cfg') == []

    messages = ''.join(caplog.messages)
    expected = (
        'Значение секрета "my-secret.item1" взято из "MY_SECRET_VAL".'
        'Значение секрета "my-secret.item2" взято из "DO_ITEM2".'
        'Секрет "my-secret [sec-01g53bh2e6zzfjdp54hdfa442n]" обновлён. Версия: ver-dummy188 \n'
        'https://yav.yandex-team.ru/secret/sec-01g53bh2e6zzfjdp54hdfa442n/explore/version/ver-dummy188'
        'Значение секрета "new-other-secret.my_item1" взято из "DO_MY_ITEM1".'
        'Значение секрета "new-other-secret.my_item2" не задано в "DO_MY_ITEM2". Пропускаем.'
        'Секрет "new-other-secret [sec-dummy1020]" создан. Версия: ver-dummy200 \n'
        'https://yav.yandex-team.ru/secret/sec-dummy1020/explore/version/ver-dummy200'
        'Нет элементов для обновления секрета "new-another-secret [sec-dummy1020]". Пропускаем создание версии.'
        'Секрет "new-another-secret [sec-dummy1020]" создан. Версия:  \n'
        'https://yav.yandex-team.ru/secret/sec-dummy1020'
    )
    assert messages.endswith(expected), f'\n{messages}\n!=\n{expected}'

    # а теперь случай, когда у первого секрета нет версии
    with response_mock([
        # обновляем метаданные
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:{}',

        # берём последнюю версию - её нет
        'GET https://vault-api.passport.yandex.net/1/versions/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"status": "error", "code": "nonexistent_entity_error", "message": "nope"}',

        # добавляем новую версию
        'POST https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/versions/ -> 200:'
        '{"secret_version": "ver-dummy188"}',

    ] + requests_create):
        assert run_command_mock('secrets-sync', '--update-cfg') == []


def test_secrets_dump_smoke(run_command_mock, response_mock, capsys):

    with response_mock([
        # получаем версию
        'GET https://vault-api.passport.yandex.net/1/versions/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"version": {'
        '"secret_name": "my-secret", '
        '"secret_uuid": "sec-01g53bh2e6zzfjdp54hdfa442n", '
        '"version": "xxx", '
        '"value": [{"key": "item1", "value": "newone"}]'
        '}}',
        # получаем метаданные секрета
        'GET https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"secret": {'
        '"name": "oldname",'
        '"uuid": "sec-01g53bh2e6zzfjdp54hdfa442n",'
        '"comment": "oldcomment",'
        '"tags": ["oldtag1", "oldtag2"],'
        '"secret_roles": [{"login": "idlesign", "role_slug": "OWNER"}, {"abc_id": "123", "abc_scope": "development", "role_slug": "READER"}]'
        '}}',
    ]):
        assert run_command_mock('secrets-dump', '--with-env') == []

    out = capsys.readouterr().out
    assert out == 'MY_SECRET_VAL = newone\nMY_VAR1 = myvalue1\nMY_ANOTHER_VAR = some\n'


RUN_RESULT_SHARED = {
    'err': "'registry.yandex.net/balance/myapp:1.2.5'",
    'out': '''spec:
  deploy_units:
    backend:
      multi_cluster_replica_set:
        replica_set:
          pod_template_spec:
            spec:
              pod_agent_payload:
                spec:
                  boxes:
                    - env:
                        - name: MY_SECRET_VAL
                          value:
                            secret_env:
                              alias: sec-01g53bh2e6zzfjdp54hdfa442n:xxx
                              id: somekey
                        - name: MY_ANOTHER_VAR
                          value:
                            literal_env:
                              value: oldone
                      id: backend
              secret_refs:
                sec-01g53bh2e6zzfjdp54hdfa442n:xxx:
                  secret_id: sec-01g53bh2e6zzfjdp54hdfa442n
                  secret_version: xxx

  revision_info: { }''',
}


def test_secrets_deploy_smoke(run_command_mock, response_mock):
    # проверка всей команды без деталей

    with response_mock([
        # получаем версию
        'GET https://vault-api.passport.yandex.net/1/versions/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"version": {'
        '"secret_name": "my-secret", '
        '"secret_uuid": "sec-01g53bh2e6zzfjdp54hdfa442n", '
        '"version": "xxx", '
        '"value": [{"key": "item1", "value": "newone"}]'
        '}}',
        # получаем метаданные секрета
        'GET https://vault-api.passport.yandex.net/1/secrets/sec-01g53bh2e6zzfjdp54hdfa442n/ -> 200:'
        '{"secret": {'
        '"name": "oldname",'
        '"uuid": "sec-01g53bh2e6zzfjdp54hdfa442n",'
        '"comment": "oldcomment",'
        '"tags": ["oldtag1", "oldtag2"],'
        '"secret_roles": [{"login": "idlesign", "role_slug": "OWNER"}, {"abc_id": "123", "abc_scope": "development", "role_slug": "READER"}]'
        '}}',
    ]):
        result = run_command_mock(
            'secrets-deploy', '--with-env',
            run_result=RUN_RESULT_SHARED
        )

    assert len(result) == 2


def test_env_deploy_smoke(run_command_mock, response_mock):
    # проверка всей команды без деталей

    result = run_command_mock(
        'env-deploy',
        run_result=RUN_RESULT_SHARED
    )

    assert len(result) == 2

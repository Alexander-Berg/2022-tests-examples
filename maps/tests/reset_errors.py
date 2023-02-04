from pytest import raises
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import NotFoundError


def test_reset_errors_tolerance_one(coordinator):
    hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    hosts = resolve_hosts(['rtc:maps_b'])
    coordinator.announce(hash, hosts)

    # first fail
    coordinator.postdl_failed('pkg-b', '1.0', hosts[0])
    coordinator.postdl('pkg-b', '1.0', hosts[1:])

    coordinator.require_version('pkg-b', '__NONE__', hosts[0])
    coordinator.require_version('pkg-b', '1.0', hosts[1:])

    # second fail
    coordinator.switch_failed('pkg-b', '1.0', hosts[-1])
    coordinator.require_version('pkg-b', '__CURRENT__', hosts)

    coordinator.reset_errors('pkg-b', '1.0', 'rtc:maps_b')
    coordinator.require_version('pkg-b', '__NONE__', hosts[0])
    coordinator.require_version('pkg-b', '1.0', hosts[1:])

    assert 'pkg-b\t1.0' in coordinator.get_postdl(hosts[0]).text
    coordinator.postdl('pkg-b', '1.0', hosts[0])
    coordinator.require_version('pkg-b', '1.0', hosts)


def test_reset_errors_dataset_with_tag(coordinator):
    hosts = resolve_hosts(['rtc:maps_b'])
    tags = ['1of3', '2of3', '3of3']

    for v in ['1.0', '2.0']:
        for tag in tags:
            hash = coordinator.upload(f'pkg-b:{tag}', v, 'gen-ab1', branch='stable', tvm_id=1)
            coordinator.announce(hash, hosts)
            coordinator.postdl(f'pkg-b:{tag}', v, hosts)
            coordinator.require_version(f'pkg-b:{tag}', v, hosts)

    coordinator.switch_failed('pkg-b:1of3', '2.0', hosts[0])
    coordinator.switch_failed('pkg-b:1of3', '2.0', hosts[1])

    for tag in tags:
        coordinator.require_version(f'pkg-b:{tag}', '1.0', hosts)

    # wrong commands
    wrong_commands = [
        ('pkg-b:1of3', '1.0', 'rtc:maps_b'),
        ('pkg-b:2of3', '2.0', 'rtc:maps_b'),
        ('pkg-b', '2.0', 'rtc:maps_b'),
    ]
    for cmd in wrong_commands:
        response = coordinator.reset_errors(*cmd)
        assert response.status_code == 304
    for tag in tags:
        coordinator.require_version(f'pkg-b:{tag}', '1.0', hosts)

    # correct command
    coordinator.reset_errors('pkg-b:1of3', '2.0', 'rtc:maps_b')
    for tag in tags:
        coordinator.require_version(f'pkg-b:{tag}', '2.0', hosts)


def test_reset_errors_after_retries(coordinator):
    def check_versions(dataset, version, hosts, now):
        for host in hosts:
            response = coordinator.versions(host, now=now).text
            assert (f'{dataset}\t{version}') in response

    hosts = resolve_hosts(['rtc:maps_a'])
    retries = 3
    interval = 60
    time = 0

    hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(hash, hosts)

    # postdl failed
    coordinator.postdl('pkg-a', '1.0', hosts[1:])
    for i in range(1 + retries):
        assert 'pkg-a\t1.0' in coordinator.get_postdl(hosts[0], now=time).text
        coordinator.postdl_failed('pkg-a', '1.0', hosts[0], now=time)
        time += interval

    assert 'pkg-a\t1.0' not in coordinator.get_postdl(hosts[0], now=time).text
    coordinator.reset_errors('pkg-a', '1.0', 'rtc:maps_a')
    assert 'pkg-a\t1.0' in coordinator.get_postdl(hosts[0], now=time).text

    coordinator.postdl('pkg-a', '1.0', hosts[0])

    # switch failed
    for i in range(1 + retries):
        check_versions('pkg-a', '1.0', hosts, now=time)
        coordinator.switch_failed('pkg-a', '1.0', hosts[-1], now=time)
        time += interval

    check_versions('pkg-a', '__CURRENT__', hosts, now=time)
    coordinator.reset_errors('pkg-a', '1.0', 'rtc:maps_a')
    check_versions('pkg-a', '1.0', hosts, now=time)


def test_reset_errors_nonexistent_deployment_404(coordinator):
    group = 'rtc:maps_b'
    hosts = resolve_hosts([group])
    pkg = 'pkg-b'
    tag = 'tag1'
    ver = '1.0'

    hash = coordinator.upload(f'{pkg}:{tag}', ver, 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(hash, hosts)
    coordinator.postdl(f'{pkg}:{tag}', ver, hosts)
    coordinator.require_version(f'{pkg}:{tag}', ver, hosts)

    # invalid group
    with raises(NotFoundError):
        coordinator.reset_errors(f'{pkg}:{tag}', ver, 'rtc:maps_a')

    # invalid dataset
    with raises(NotFoundError):
        coordinator.reset_errors(f'{pkg}-nonexistent:{tag}', ver, group)

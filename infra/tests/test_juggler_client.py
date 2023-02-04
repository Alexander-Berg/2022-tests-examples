import os
import pytest

import juggler_sdk
import six

from awacs.lib.juggler_client import JugglerClient, NotifyRule


@pytest.fixture
def namespace_prefix():
    return 'test_awacs.'


@pytest.fixture
def juggler_client(namespace_prefix):
    return JugglerClient(
        oauth_token=os.environ.get('JUGGLER_CLIENT_TOKEN', 'DUMMY'),
        namespace_prefix=namespace_prefix
    )


def cleanup_namespace(juggler_client, namespace):
    juggler_client.create_or_update_namespace(
        name=namespace,
        abc_service_slug='svc_rclb',
        inherit_downtimers=True,
        downtimers=['i-dyachkov'],
        owners=['torkve']
    )

    for notify_rule in juggler_client.list_notify_rules(namespace):
        juggler_client.remove_notify_rule(notify_rule.rule_id)


@pytest.mark.vcr
def test_juggler_create_update_remove_namespace(namespace_prefix, juggler_client):
    with pytest.raises(ValueError) as error:
        juggler_client.create_or_update_namespace(
            name='fake',
            abc_service_slug='svc_rclb',
            inherit_downtimers=True,
            downtimers=['i-dyachkov']
        )

    assert six.text_type(error.value) == 'Namespace name without prefix "{}" not valid, got "fake"'.format(
        namespace_prefix)

    juggler_namespace = '{}create_or_update_namespace'.format(namespace_prefix)
    juggler_client.remove_namespace_if_exists(juggler_namespace)

    result = juggler_client.create_or_update_namespace(
        name=juggler_namespace,
        abc_service_slug='svc_rclb',
        inherit_downtimers=True,
        downtimers=['i-dyachkov']
    )
    assert result.created and not result.updated

    result = juggler_client.create_or_update_namespace(
        name=juggler_namespace,
        abc_service_slug='svc_rclb',
        inherit_downtimers=True,
        downtimers=[]
    )
    assert not result.created and result.updated

    result = juggler_client.create_or_update_namespace(
        name=juggler_namespace,
        abc_service_slug='svc_rclb',
        inherit_downtimers=True,
        downtimers=[]
    )
    assert not result.created and not result.updated

    namespace_removed = juggler_client.remove_namespace_if_exists(juggler_namespace)
    assert namespace_removed


@pytest.fixture
def juggler_namespace(namespace_prefix):
    return '{}juggler_testing'.format(namespace_prefix)


@pytest.mark.skip(reason="use this only for recreate vcr cassettes (comment skip)")
def test_cleanup_juggler_namespace(juggler_client, juggler_namespace):
    juggler_client.sync_checks(juggler_namespace, [])
    cleanup_namespace(juggler_client, juggler_namespace)


@pytest.mark.vcr
def test_juggler_sync_juggler_checks(juggler_client, juggler_namespace):
    check = juggler_sdk.Check(
        host='testing_juggler_host',
        service='testing_juggler_service',
        namespace=juggler_namespace,
        tags=['testing_juggler_checks']
    )

    res = juggler_client.sync_checks(juggler_namespace, checks=[check])
    assert res.changed == [('testing_juggler_host', 'testing_juggler_service')]

    check.tags = ['testing_juggler_checks', 'testing_juggler_checks_2']
    res = juggler_client.sync_checks(juggler_namespace, checks=[check])
    assert res.changed == [('testing_juggler_host', 'testing_juggler_service')]

    res = juggler_client.cleanup_checks(juggler_namespace)
    assert res.removed == [('testing_juggler_host', 'testing_juggler_service')]


@pytest.mark.vcr
def test_juggler_sync_notify_rules(juggler_client, juggler_namespace):
    check_tag = 'testing_juggler_checks'
    notify_rule = NotifyRule(
        selector='tag={}'.format(check_tag),
        template_name='on_status_change',
        template_kwargs={
            'status': [{'from': 'OK', 'to': 'CRIT'}],
            'login': ['i-dyachkov'],
            'method': 'sms',
        },
        description="testing juggler"
    )

    result = juggler_client.sync_notify_rules(juggler_namespace, [notify_rule])
    assert result.add == 1 and result.remove == 0

    notify_rule = NotifyRule(
        selector='tag={}'.format(check_tag),
        template_name='on_status_change',
        template_kwargs={
            'status': [{'from': 'WARN', 'to': 'CRIT'}],
            'login': ['i-dyachkov'],
            'method': 'sms',
        },
        description="testing juggler 2"
    )
    result = juggler_client.sync_notify_rules(juggler_namespace, [notify_rule])
    assert result.add == 1 and result.remove == 1

    result = juggler_client.sync_notify_rules(juggler_namespace, [])
    assert result.add == 0 and result.remove == 1


@pytest.mark.vcr
def test_juggler_list_notify_rules(juggler_client, namespace_prefix):
    ns1 = namespace_prefix + 'ci-clickhouse'
    ns2 = namespace_prefix + 'devtools'
    cleanup_namespace(juggler_client, ns1)
    cleanup_namespace(juggler_client, ns2)

    def make_rule(ns, method):
        return NotifyRule(
            selector='namespace={}'.format(ns),
            template_name='on_status_change',
            template_kwargs={
                'status': [{'from': 'OK', 'to': 'CRIT'}],
                'login': ['torkve'],
                'method': method,
            },
            description="testing juggler",
        )

    rule1 = make_rule(ns1, 'telegram')
    rule2 = make_rule(ns2, 'telegram')
    rule3 = make_rule(ns1, 'sms')
    rule4 = make_rule(ns2, 'sms')

    result = juggler_client.sync_notify_rules(ns1, [rule1, rule2])
    assert result.add == 2 and result.remove == 0

    result = juggler_client.sync_notify_rules(ns2, [rule3, rule4])
    assert result.add == 2 and result.remove == 0

    rules = juggler_client.list_notify_rules(ns1)
    assert all(rule.kwargs['namespace'] == ns1 for rule in rules)
    assert rule1 in rules
    assert rule2 in rules

    rules = juggler_client.list_notify_rules(ns2)
    assert all(rule.kwargs['namespace'] == ns2 for rule in rules)
    assert rule3 in rules
    assert rule4 in rules

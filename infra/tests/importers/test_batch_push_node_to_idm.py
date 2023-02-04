import mock

from django.core.management import call_command

from __tests__.utils import create_server, get_or_create_server_group


@mock.patch(
    'infra.cauth.server.master.importers.management.commands.batch_push_node_to_idm.create_dst_requests',
    return_value=[],
)
def test_filter_servers_correctly(patched_create_dst_requests):
    source_name = 'bot'
    server = create_server('example.org', source_name=source_name)

    call_command('batch_push_node_to_idm', source_name='unknown')
    assert patched_create_dst_requests.call_count == 0

    call_command('batch_push_node_to_idm', source_name=source_name)
    patched_create_dst_requests.assert_called_once_with(mock.ANY, server)


@mock.patch(
    'infra.cauth.server.master.importers.management.commands.batch_push_node_to_idm.create_dst_requests',
    return_value=[],
)
def test_filter_groups_correctly(patched_create_dst_requests):
    source_name = 'bot'
    group = get_or_create_server_group(source_name=source_name)

    call_command('batch_push_node_to_idm', object_type='group', source_name='unknown')
    assert patched_create_dst_requests.call_count == 0

    call_command('batch_push_node_to_idm', object_type='group', source_name=source_name)
    patched_create_dst_requests.assert_called_once_with(mock.ANY, group)

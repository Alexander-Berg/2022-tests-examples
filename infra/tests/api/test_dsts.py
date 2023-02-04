import pytest

from django.conf import settings

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import CLIENT_SOURCE_REASON, FLOW_TYPE
from infra.cauth.server.common.models import ServerGroupTrustedSourceRelation, ServerTrustedSourceRelation
from infra.cauth.server.master.api.idm.dsts import ServerDst
from __tests__.utils import create_server, get_or_create_server_group, get_or_create_source


@pytest.mark.parametrize('flow', FLOW_TYPE.choices())
@pytest.mark.parametrize(
    'trusted_sources,group_sources,has_alias',
    (
        (('idm-cms', ), ('cms', ), True),
        (('idm-cms', 'other_source'), ('cms', 'other'), True),
        (('source_1', 'source_2'), ('cms', 'other'), False),
        (('idm-cms', ), ('other1', 'other2'), False),
    )
)
def test_idm_cms_approvers_alias_wrong_trusted_sources(trusted_sources, group_sources, has_alias, flow):
    session = Session()
    server = create_server('fqdn')
    for i, group_source in enumerate(group_sources):
        group = get_or_create_server_group(group_source)
        server.groups.append(group)

    if flow == FLOW_TYPE.CLASSIC:
        for source in trusted_sources:
            source = get_or_create_source(source)
            ServerTrustedSourceRelation.create(
                session=session,
                server_id=server.id,
                source_id=source.id,
                reason=CLIENT_SOURCE_REASON.FROM_CLIENT,
            )
    else:
        backend_source_name = settings.CAUTH_FLOW_SOURCES_BY_PRIO[0]
        auth_group = get_or_create_server_group(backend_source_name, flow=flow)
        server.groups.append(auth_group)
        for source in trusted_sources:
            source = get_or_create_source(source)
            ServerGroupTrustedSourceRelation.create(
                session=session,
                servergroup_id=auth_group.id,
                source_id=source.id,
                reason=CLIENT_SOURCE_REASON.FROM_SOURCE,
            )

    dst = ServerDst(server)
    aliases = dst.aliases

    expected_alias = {
        'type': 'need-idm-cms-approvers',
        'name': {'ru': 'true', 'en': 'true'},
    }
    expected_alias_in_aliases = expected_alias in aliases
    assert expected_alias_in_aliases is has_alias

import json

import pytest

from wiki import access as wiki_access
from wiki.api_svc.acl.acl import verify_grantees

pytestmark = [
    pytest.mark.django_db,
]


def test_verify_grantees(client, cloud_page_cluster, groups, wiki_users, add_user_to_group):
    root = cloud_page_cluster['root']
    add_user_to_group(groups.root_group, wiki_users.chapson)

    wiki_access.set_access_nopanic(
        root,
        wiki_access.TYPES.RESTRICTED,
        wiki_users.thasonic,
        staff_models=[
            wiki_users.volozh.staff,
        ],
        groups=[groups.root_group],
    )

    good, bad = verify_grantees(
        root.supertag,
        [
            wiki_users.volozh.staff.id,
            wiki_users.chapson.staff.id,
            wiki_users.asm.staff.id,
        ],
    )

    assert bad == {
        wiki_users.asm.staff.id,
    }

    client.login('thasonic')
    response = client.post(
        '/_api/svc/acl/.verify_grantees',
        {
            'supertag': root.supertag,
            'staff_ids': [
                wiki_users.volozh.staff.id,
                wiki_users.chapson.staff.id,
                wiki_users.asm.staff.id,
            ],
        },
    )
    data = json.loads(response.content)['data']
    assert data['bad'] == [wiki_users.asm.staff.id]

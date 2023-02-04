"""
WIKI-15291
"""

import pytest
from wiki import settings

from wiki.grids.models import Grid
from intranet.wiki.tests.wiki_tests.common import grid_helper

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "done" : true,
  "fields" : [
    {
      "name" : "responsible",
      "type": "staff",
      "title" : "actor"
    }
  ]
}
"""


@pytest.mark.django_db
def test_change_non_unique(client, api_url, wiki_users):
    client.login('chapson')
    slug = 'testgrid'
    grid_helper.create_grid(client, api_url, slug, GRID_STRUCTURE, wiki_users.chapson)
    wiki_users.asm.id = None
    wiki_users.asm.username = 'asm2'
    if settings.IS_BUSINESS:
        wiki_users.asm.cloud_uid += '1'
    wiki_users.asm.save()

    wiki_users.asm.staff.id = None
    wiki_users.asm.staff.is_dismissed = True
    wiki_users.asm.staff.user_id = wiki_users.asm.id
    wiki_users.asm.staff.login_ld += '1'
    wiki_users.asm.staff.uid += '1'

    wiki_users.asm.staff.save()

    grid = Grid.active.get(supertag=slug)
    response = client.post(
        f'{api_url}/testgrid/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'added_row': {
                        'after_id': -1,
                        'data': {'responsible': 'asm'},
                    }
                }
            ],
        },
    )
    assert response.status_code == 200

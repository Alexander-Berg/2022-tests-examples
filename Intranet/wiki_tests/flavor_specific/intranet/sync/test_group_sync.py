from typing import Dict

import pytest
from model_mommy import mommy
from mock import MagicMock
from wiki.intranet.models import Group
from wiki.sync.staff.mapping.models import GroupMapper


def _group_to_raw(group: Group) -> Dict:
    return {
        'id': group.id,
        'parent': {'id': group.parent_id},
        'is_deleted': False,
        'description': group.description,
        'department': {'id': group.department_id},
        'name': group.name,
        'url': group.url,
        'affiliation_counters': {
            'external': group.externals_count,
            'yandex': group.yandex_count,
            'yamoney': group.yamoney_count,
        },
        'type': 'department',
    }


@pytest.mark.django_db
def test_updated_denorm(wiki_users, test_page):
    """
    root
    |   |
    a   b -- b_1, b_2
    |
    a_1 a_2
    """
    root: Group = mommy.make(Group, name='root')
    a: Group = mommy.make(Group, name='A', parent=root)
    b: Group = mommy.make(Group, name='B', parent=root)
    a_1: Group = mommy.make(Group, name='A1', parent=a)
    a_2: Group = mommy.make(Group, name='A2', parent=a)
    b_1: Group = mommy.make(Group, name='B1', parent=b)
    b_2: Group = mommy.make(Group, name='B2', parent=b)
    Group.tree.rebuild()
    a.refresh_from_db()
    b.refresh_from_db()

    gm = GroupMapper()
    a_2.parent_id = b.id
    b_2.parent_id = a.id
    gm.process_batch([_group_to_raw(b_2), _group_to_raw(a_2)], MagicMock())

    a.refresh_from_db()
    b.refresh_from_db()
    b_2.refresh_from_db()
    a_2.refresh_from_db()

    """
          root
          |   |
          a   b -- b_1, a_2
          |
          a_1 b_2
    """

    assert set(a.get_descendants(include_self=False).all()) == {a_1, b_2}
    assert set(b.get_descendants(include_self=False).all()) == {b_1, a_2}
    assert set(a_2.get_ancestors(include_self=False).all()) == {b, root}
    assert set(b_2.get_ancestors(include_self=False).all()) == {a, root}

from pretend import stub

from plan.common.utils.collection.mapping import DotAccessedDict as DotDict
from plan.services.operations import process

MemberStub = stub


def test_merge_duplicate_memberships_only_new():
    data = DotDict(
        new_members=[
            MemberStub(person_id=1, role_id=1),
            MemberStub(person_id=1, role_id=2),
        ],
        present_members=[],
    )

    process.merge_duplicate_memberships(data)

    assert 'members' in data.result
    assert 'to_create' in data.result.members

    to_create = data.result.members.to_create
    assert len(to_create) == 2

    expected = [(1, 1), (1, 2)]
    assert sorted((m.person_id, m.role_id) for m in to_create) == expected


def test_merge_duplicate_memberships_ignore_new():
    data = DotDict(
        new_members=[
            MemberStub(person_id=1, role_id=1, from_department=None),
        ],
        present_members=[
            MemberStub(person_id=1, role_id=1),
        ],
    )

    process.merge_duplicate_memberships(data)

    to_create = data.result.members.to_create
    to_update = data.result.members.to_update
    assert len(to_create) == 0
    assert len(to_update) == 0


def test_merge_departments():
    data = DotDict(
        new_departments={
            (1, 1): (stub(id=1), stub(id=1)),
            (2, 1): (stub(id=2), stub(id=1)),
        },
        present_departments={
            (1, 1): (stub(id=1), stub(id=1)),
        },
        result={},  # уже будет создан другой функцией
    )
    process.merge_departments(data)

    assert 'to_create' in data.result.departments
    to_create = data.result.departments.to_create
    assert len(to_create) == 1
    assert to_create[0][0].id == 2
    assert to_create[0][1].id == 1

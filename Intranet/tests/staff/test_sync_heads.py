import mock

from review.shortcuts import models
from review.staff.sync.fetch_structure import get_persons_at_staff_change


def test_sync(person_builder, staff_structure_change_builder):
    ssc = staff_structure_change_builder()
    empl = person_builder()
    head_1 = person_builder()
    head_2 = person_builder()
    not_synced = 'asdf'
    get_heads_mock = {'chiefs': [
        {
            'chiefs': [
                {'login': head_1.login},
                {'login': head_2.login},
            ],
            'person': {'login': empl.login}
        },
        {
            'chiefs': [
                {'login': head_2.login},
                {'login': not_synced},
            ],
            'person': {'login': head_1.login}
        },
        {
            'chiefs': [
                {'login': head_2.login},
            ],
            'person': {'login': not_synced}
        },
    ]}
    get_heads_path = 'review.staff.sync.fetch_structure.fetch_persons_with_heads'
    get_joined_today = mock.Mock(get_pages=mock.Mock(
        return_value=[
            [
                {'login': not_synced}
            ],
            [
                {'login': head_2.login},
                {'login': empl.login},
            ],
        ],
    ))
    get_persons_path = 'review.staff.sync.fetch_structure.get_persons_result_set'

    with mock.patch(get_heads_path, return_value=get_heads_mock):
        with mock.patch(get_persons_path, return_value=get_joined_today):
            get_persons_at_staff_change()

    expected_chain = ','.join(map(str, (head_1.id, head_2.id)))
    db_chain = models.PersonHeads.objects.get(
        structure_change=ssc,
        person_id=empl.id,
    ).heads
    assert db_chain == expected_chain


def test_sync_does_not_ceate_redundant_records(person_builder, staff_structure_change_builder):
    ssc1 = staff_structure_change_builder()
    empl_1 = person_builder()
    head_1 = person_builder()
    head_2 = person_builder()
    empl_2 = person_builder()
    head_3 = person_builder()

    models.PersonHeads(
        person_id=empl_1.id,
        heads=f'{head_2.id},{head_1.id}',
        structure_change_id=ssc1.id,
    ).save()
    models.PersonHeads(
        person_id=empl_2.id,
        heads=f'{head_3.id},{head_1.id}',
        structure_change_id=ssc1.id,
    ).save()

    get_heads_mock = {
        'chiefs': [
            {
                'person': {'login': empl_1.login},
                'chiefs': [
                    {'login': head_2.login},
                    {'login': head_1.login},
                ],
            },
            {
                'person': {'login': empl_2.login},
                'chiefs': [
                    {'login': empl_1.login},
                    {'login': head_1.login},
                ],
            },
        ],
    }
    get_heads_path = 'review.staff.sync.fetch_structure.fetch_persons_with_heads'

    get_joined_today = mock.Mock(get_pages=mock.Mock(return_value=[]))
    get_persons_path = 'review.staff.sync.fetch_structure.get_persons_result_set'

    staff_structure_change_builder()

    assert models.PersonHeads.objects.count() == 2

    with mock.patch(get_heads_path, return_value=get_heads_mock):
        with mock.patch(get_persons_path, return_value=get_joined_today):
            get_persons_at_staff_change()

    assert models.PersonHeads.objects.count() == 3
    assert models.PersonHeads.objects.filter(person_id=empl_1.id).count() == 1
    assert models.PersonHeads.objects.filter(person_id=empl_2.id).count() == 2

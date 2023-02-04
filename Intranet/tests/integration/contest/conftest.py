import pytest

from intranet.femida.src.candidates import choices
from intranet.femida.tests import factories as f
from ..deduplication.conftest import dd_dataset  # noqa


@pytest.fixture
def contest_dataset():
    dataset = {}
    # Challenge, который уже выгрузили и с ним ничего делать не надо
    dataset['challenge_1_1'] = f.ChallengeFactory.create(
        type=choices.CHALLENGE_TYPES.contest,
        status=choices.CHALLENGE_STATUSES.pending_review,
        # Тут должны быть и results, но нам на них все равно в данном контексте
        answers={
            'contest': {'id': 1},
            'participation': {'id': 1},
            'results': {},
        }
    )

    # (1, 2) - challenge, который завершен в Контесте и должен быть синхронизирован
    # (1, 3) - challenge, который не завершен в Контесте и должен быть проигнорирован
    # (1, 4) - challenge, которого нет в контесте
    # (2, 5) - challenge другого контеста
    for contest_id, participation_id in [(1, 2), (1, 3), (1, 4), (2, 5)]:
        key = 'challenge_{}_{}'.format(contest_id, participation_id)
        dataset[key] = f.ChallengeFactory.create(
            type=choices.CHALLENGE_TYPES.contest,
            status=choices.CHALLENGE_STATUSES.assigned,
            answers={
                'contest': {'id': contest_id},
                'participation': {'id': participation_id},
            }
        )

    return dataset

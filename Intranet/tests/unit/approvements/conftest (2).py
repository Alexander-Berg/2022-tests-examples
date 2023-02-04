import pytest

from django.db.models import Prefetch

from ok.approvements.models import Approvement

from tests import factories as f
from tests.factories import make_approvement_stages_overdue


@pytest.fixture
def two_persons():
    return 'agrml', 'qazaq'


@pytest.fixture
def active_approvements_with_active_stages_only(two_persons):
    f.create_approvement(
        approvers=two_persons[:1],
        is_parallel=False,
    )
    f.create_approvement(
        approvers=two_persons,
        is_parallel=True,
    )
    return (
        Approvement.objects
        .prefetch_related(
            Prefetch(
                lookup='stages',
                to_attr='active_stages',
            ),
        )
        .all()
    )


@pytest.fixture
def overdue_approvements_map(two_persons):
    sequential_approvement_author1 = f.create_approvement(
        author=two_persons[0],
        is_parallel=False,
    )
    parallel_approvement_author1 = f.create_approvement(
        stages_count=2,
        author=two_persons[0],
        is_parallel=True,
    )
    parallel_approvement_author2 = f.create_approvement(
        author=two_persons[1],
        is_parallel=True,
    )
    in_time_approvement_author1 = f.create_approvement(
        author=two_persons[0],
    )

    # Просрачиваем дедлайны
    overdue_approvement_stages = [
        sequential_approvement_author1.stages.get(position=0),
        *parallel_approvement_author1.stages.all(),
        *parallel_approvement_author2.stages.all(),
    ]
    make_approvement_stages_overdue(overdue_approvement_stages)
    return {
        'sequential_approvement_author1': sequential_approvement_author1,
        'parallel_approvement_author1': parallel_approvement_author1,
        'parallel_approvement_author2': parallel_approvement_author2,
        'in_time_approvement_author1': in_time_approvement_author1,
    }

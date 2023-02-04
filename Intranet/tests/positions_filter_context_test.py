from datetime import date

import pytest

from staff.budget_position.models import Reward
from staff.oebs.models import Reward as OebsReward

from staff.headcounts.positions_filter_context import PositionsFilterContext
from staff.headcounts.tests.factories import HeadcountPositionFactory


@pytest.mark.django_db()
def test_filter_by_category(company):
    # given
    hp1 = HeadcountPositionFactory(department=company.yandex)
    mass_reward = Reward.objects.create(category='Mass', name='Mass', start_date=date.today(), scheme_id=1)
    OebsReward.objects.create(
        dis_instance=mass_reward,
        scheme_id=hp1.reward_id,
        category='Mass',
        name='Mass',
        start_date=date.today(),
    )
    hp2 = HeadcountPositionFactory(department=company.yandex)
    mass_reward = Reward.objects.create(category='Support', name='Support', start_date=date.today(), scheme_id=2)
    OebsReward.objects.create(
        dis_instance=mass_reward,
        scheme_id=hp2.reward_id,
        category='Support',
        name='Support',
        start_date=date.today(),
    )

    filter_context = PositionsFilterContext(category=['Mass'])

    # when
    result = filter_context.positions_objects_qs()

    # then
    assert result.get().code == hp1.code

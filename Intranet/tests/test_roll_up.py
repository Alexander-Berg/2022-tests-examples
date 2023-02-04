import pytest

from staff.lib.testing import ValueStreamFactory, GroupFactory
from staff.umbrellas.models import Umbrella
from staff.umbrellas.tests.factories import UmbrellaFactory

from staff.syncs.models import StartrekUmbrella
from staff.syncs.umbrellas.roll_up import UmbrellaRollUpper
from staff.syncs.umbrellas.tests.factories import StarTrekUmbrellaFactory, StarTrekVsFactory


@pytest.mark.django_db
def test_umbrella_roll_upper_no_vs():
    StarTrekUmbrellaFactory(vs=StarTrekVsFactory(abc_service_id=None))

    UmbrellaRollUpper.rollup(create_absent=True)

    assert Umbrella.objects.all().count() == 0


@pytest.mark.django_db
def test_umbrella_roll_upper_no_data():
    UmbrellaRollUpper.rollup(create_absent=True)

    assert Umbrella.objects.all().count() == 0


@pytest.mark.django_db
def test_umbrella_roll_upper_update_existing():
    umbrella = UmbrellaFactory()
    star_trek_umbrella = StarTrekUmbrellaFactory(staff_instance=umbrella)
    value_stream = ValueStreamFactory()
    GroupFactory(service_id=star_trek_umbrella.vs.abc_service_id, url=value_stream.url)
    assert umbrella.issue_key != star_trek_umbrella.issue_key

    UmbrellaRollUpper.rollup(create_absent=True)

    assert Umbrella.objects.all().count() == 1
    _assert_db_umbrella(star_trek_umbrella, value_stream.id, id=umbrella.id)


@pytest.mark.django_db
def test_umbrella_roll_upper_create_new():
    star_trek_umbrella = StarTrekUmbrellaFactory()
    value_stream = ValueStreamFactory()
    GroupFactory(service_id=star_trek_umbrella.vs.abc_service_id, url=value_stream.url)
    assert Umbrella.objects.all().count() == 0

    UmbrellaRollUpper.rollup(create_absent=True)

    assert Umbrella.objects.all().count() == 1
    _assert_db_umbrella(star_trek_umbrella, value_stream.id, issue_key=star_trek_umbrella.issue_key)


def _assert_db_umbrella(star_trek_umbrella: StartrekUmbrella, value_stream_id: int, **lookup) -> None:
    db_instance = Umbrella.objects.get(**lookup)
    assert db_instance.goal_id == int(star_trek_umbrella.issue_key.split('-', 1)[1])
    assert db_instance.issue_key == star_trek_umbrella.issue_key
    assert db_instance.name == star_trek_umbrella.name
    assert db_instance.value_stream.id == value_stream_id

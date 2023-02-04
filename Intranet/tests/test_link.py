import pytest

from staff.umbrellas.tests.factories import UmbrellaFactory

from staff.syncs.umbrellas.link import UmbrellaLinker
from staff.syncs.models import StartrekUmbrella
from staff.syncs.umbrellas.tests.factories import StarTrekUmbrellaFactory


@pytest.mark.django_db
def test_umbrella_linker_already_linked():
    UmbrellaFactory()
    umbrella = UmbrellaFactory()
    StarTrekUmbrellaFactory(issue_key=umbrella.issue_key, staff_instance=umbrella)
    target = UmbrellaLinker()

    target.run()

    star_trek_umbrella = StartrekUmbrella.objects.get(issue_key=umbrella.issue_key)
    assert star_trek_umbrella.staff_instance == umbrella


@pytest.mark.django_db
def test_umbrella_linker():
    UmbrellaFactory()
    umbrella = UmbrellaFactory()
    StarTrekUmbrellaFactory(issue_key=umbrella.issue_key, staff_instance=None)
    target = UmbrellaLinker()

    target.run()

    star_trek_umbrella = StartrekUmbrella.objects.get(issue_key=umbrella.issue_key)
    assert star_trek_umbrella.staff_instance == umbrella


@pytest.mark.django_db
def test_umbrella_linker_no_target_instance():
    UmbrellaFactory()
    issue_key = StarTrekUmbrellaFactory(staff_instance=None).issue_key
    target = UmbrellaLinker()

    target.run()

    star_trek_umbrella = StartrekUmbrella.objects.get(issue_key=issue_key)
    assert star_trek_umbrella.staff_instance is None

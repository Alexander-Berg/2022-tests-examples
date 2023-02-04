# coding: utf-8

import pytest

from intranet.dogma.dogma.core.models import Source, Repo, Clone
from intranet.dogma.dogma.core.utils import get_current_node


pytestmark = pytest.mark.django_db(transaction=True)


def test_source_rate():
    source = Source.objects.create(code='testsource', rate=0.5)

    repo1 = Repo.objects.create(owner='root', name='repo1', source=source)
    repo2 = Repo.objects.create(owner='root', name='repo2', source=source)
    repo3 = Repo.objects.create(owner='root', name='repo3', source=source)
    repo4 = Repo.objects.create(owner='root', name='repo4', source=source)

    Clone.objects.create(repo=repo1, node=get_current_node())

    assert source.get_rate_delta() == 1

    Clone.objects.create(repo=repo2, node=get_current_node())

    assert source.get_rate_delta() == 0

    Clone.objects.create(repo=repo3, node=get_current_node())

    assert source.get_rate_delta() == 0

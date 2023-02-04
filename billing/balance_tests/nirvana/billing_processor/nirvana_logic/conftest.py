import pytest

from medium.medium_nirvana import NirvanaLogic


@pytest.fixture()
def logic():
    return NirvanaLogic()

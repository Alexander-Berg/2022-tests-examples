import pytest


@pytest.fixture
def rfid_cmp():
    def cmp(a):
        return a['badges'][0]['rfid']
    return cmp

import copy
import pytest
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import keyring


KEYRING1 = {
    "center_group": "gpg-yabs",
    "type": "group",
    "storage_key": "901484/__gpg__/gpg-yabs.gpg_94b387f21bdd10e9f6531a0b6cfa62f87e586c4e848450ae81ecf31abeaf466c",
    "sha265": "94b387f21bdd10e9f6531a0b6cfa62f87e586c4e848450ae81ecf31abeaf466c",
    "size": 1608
}
KEYRING2 = {
    "center_group": "main_keyring",
    "type": "group",
    "storage_key": "1028107/__gpg__/main_keyring.gpg_9657e056dd5e2b53056469be2d42449986df406a6b563332beb4e51284cbf192",
    "sha265": "9657e056dd5e2b53056469be2d42449986df406a6b563332beb4e51284cbf192",
    "size": 2778965
}
KEYRING3 = {
    "center_group": "gpg-passport",
    "type": "group",
    "storage_key": "45740/__gpg__/gpg-passport.gpg_ebf7bcca2217502695b98aa2b78287346007af780e4167eeea6410f49fdbaea8",
    "sha265": "ebf7bcca2217502695b98aa2b78287346007af780e4167eeea6410f49fdbaea8",
    "size": 13914
}


@pytest.fixture
def mock_db():
    mongo_connection.cacus()['__gpg__'].insert_many(copy.deepcopy([KEYRING1, KEYRING2, KEYRING3]))


def test_from_dict():
    k = keyring.Keyring.from_dict(KEYRING1)
    assert k.group == KEYRING1['center_group']
    assert k.type == KEYRING1['type']
    assert k.storage_key == KEYRING1['storage_key']
    assert k.sha256 == KEYRING1['sha265']
    assert k.size == KEYRING1['size']


def test_to_dict():
    k = keyring.Keyring.from_dict(KEYRING1)
    assert KEYRING1 == k.to_dict()


def test_find_query():
    expected = {'center_group': 'mock'}
    assert expected == keyring.KeyringStorage._find_query(group='mock')
    expected['type'] = 'group'
    assert expected == keyring.KeyringStorage._find_query(group='mock', type_='group')


@pytest.mark.usefixtures("mock_db")
def test_find_one():
    assert keyring.default_storage.find_one(group='non-existing') is None
    assert keyring.default_storage.find_one(group='main_keyring').to_dict() == KEYRING2


@pytest.mark.usefixtures("mock_db")
def test_find():
    assert not keyring.default_storage.find(group='non-existing')
    assert not keyring.default_storage.find(type_='non-existing')
    assert not keyring.default_storage.find(group='non-existing', type_='non-existing')
    assert len(keyring.default_storage.find(type_='group')) == 3


@pytest.mark.usefixtures("mock_db")
def test_save():
    k = keyring.default_storage.find_one(group='main_keyring')
    k.type = 'main'
    keyring.default_storage.save(k)
    k = keyring.default_storage.find_one(group='main_keyring')
    assert k.type == 'main'

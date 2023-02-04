# coding: utf-8



import pytest


@pytest.fixture(autouse=True)
def mongo_mock(mocker):
    from mongomock import MongoClient
    mocker.patch(
        target='at.aux_.MongoStorage.MongoClient',
        new=MongoClient,
    )


# @pytest.fixture(autouse='session')
# def startrek_mock(mocker):
#     mocker.patch(
#         target='at.aux_.Trackers.startrek_client',
#     )

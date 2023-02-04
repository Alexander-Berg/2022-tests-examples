import datetime
import os
import random
import string

import requests
import pytest

import yt.wrapper

from intranet.search.sandbox.updateAchievery import update_achievery

LOGINS = {
    "wrong": "1121100000111111",
    "error": "1121100000212121",
    "ok": "2222200000222222",
    "without": "3333300000232323",
    "inactive": "3333300000444444",
}


def generate_rows(login, amt, last_id) -> list[update_achievery.Row]:
    strs = string.ascii_lowercase + string.ascii_uppercase
    return [
        update_achievery.Row(
            id=last_id + i,
            created_at=datetime.datetime.now().isoformat(),
            request_id=''.join([random.choice(strs) for _ in range(10)]),
            score=random.choice(["relevant", "vital", "irrelevant"]),
            uid=LOGINS[login],
            updated_at=datetime.datetime.now().isoformat(),
            url=''.join([random.choice(strs) for _ in range(32)]),
        )
        for i in range(amt)
    ]


def fill_yt(table_name, proxy) -> None:
    client = yt.wrapper.YtClient(proxy)
    actions = []
    actions.extend(generate_rows("error", 1, 0))
    actions.extend(generate_rows("ok", 20, 1))
    actions.extend(generate_rows("wrong", 50, 21))
    actions.extend(generate_rows("inactive", 50, 71))
    actions.extend(generate_rows("without", 20, 121))

    client.write_table_structured(table_name, update_achievery.Row, actions)


@pytest.fixture()
def yt_proxy():
    if "YT_PROXY" in os.environ:
        yield os.environ["YT_PROXY"]
    else:
        import yt.local

        env = yt.local.start()
        yield env.get_proxy_addr()
        yt.local.stop(env.id, remove_working_dir=True)


class TestAchievery:
    testing = update_achievery.IntrasearchAchieveryManager(
        token="",
        table_with_marks="",
        yt_token="",
    )

    def test_level(self):
        scores = [0, 20, 50, 100]
        for index, score in enumerate(scores):
            res = update_achievery.get_level(score)
            assert res == index

    def test_uids(self, monkeypatch):
        test_uid = "1120000000000000"

        class MockResponse:
            @staticmethod
            def json():
                return {"result": [{"uid": test_uid, "login": "test"}]}

            @property
            def ok(self):
                return True

        def mock_get(*args, **kwargs):
            return MockResponse()

        monkeypatch.setattr(requests, "get", mock_get)
        res = TestAchievery.testing._resolve_uids([test_uid])
        assert res[test_uid] == "test"

    def test_statuses(self, monkeypatch):
        class MockResponseEmpty:
            @staticmethod
            def json():
                return {
                    "objects": [],
                    "total": 0,
                }

        class MockResponseInactive:
            @staticmethod
            def json():
                return {
                    "objects": [
                        {"is_active": False},
                        {"is_active": True},
                    ],
                    "total": 2,
                }

        class MockResponseActive:
            @staticmethod
            def json():
                return {
                    "objects": [
                        {
                            "is_active": True,
                            "level": 0,
                        },
                    ],
                    "total": 1,
                }

        def mock_get(*args, **kwargs):
            if kwargs["params"]["person.login"] == "empty":
                return MockResponseEmpty()
            if kwargs["params"]["person.login"] == "inactive":
                return MockResponseInactive()
            if kwargs["params"]["person.login"] == "active":
                return MockResponseActive()

        monkeypatch.setattr(requests, "get", mock_get)
        res = TestAchievery.testing._check_achievement("empty", 0)
        assert res == update_achievery.UserStatus.without_achieve

        res = TestAchievery.testing._check_achievement("inactive", 0)
        assert res == update_achievery.UserStatus.inactive

        res = TestAchievery.testing._check_achievement("active", 0)
        assert res == update_achievery.UserStatus.ok

        res = TestAchievery.testing._check_achievement("active", 1)
        assert res == update_achievery.UserStatus.wrong_level

    def test_issuing(self, monkeypatch):
        def mock_req(*args, **kwargs):
            if kwargs["data"]["person.login"] == "error":
                raise
            else:
                return True

        monkeypatch.setattr(requests, "request", mock_req)

        TestAchievery.testing._give_achievement("error")
        TestAchievery.testing._give_achievement("normal")

    def test_edit(self, monkeypatch):
        class MockResponsePutGood:
            status_code = 201

        class MockResponsePutBad:
            status_code = 404

        class MockResponseGetGood:
            @staticmethod
            def json():
                return {
                    "objects": [{"id": "123", "revision": "123123"}],
                    "total": 1,
                }

        class MockResponseGetBad:
            @staticmethod
            def json():
                return {
                    "objects": [{"id": "123", "revision": "789789"}],
                    "total": 1,
                }

        def mock_req(*args, **kwargs):
            if kwargs["method"] == "get":
                if kwargs["params"]["person.login"] == "errorGet":
                    raise Exception("Error")
                elif kwargs["params"]["person.login"] == "errorPut":
                    return MockResponseGetBad()
                else:
                    return MockResponseGetGood()
            elif kwargs["method"] == "put":
                if kwargs["data"]["revision"] == "789789":
                    return MockResponsePutBad()
                else:
                    return MockResponsePutGood()

        monkeypatch.setattr(requests, "request", mock_req)

        TestAchievery.testing._edit_achievement("errorGet", is_active=True)
        TestAchievery.testing._edit_achievement("errorPut", is_active=True)
        TestAchievery.testing._edit_achievement("noError", is_active=True)

    def test_handle(self, monkeypatch, yt_proxy):
        def mock_check_achievement(login, level):
            if login == "error":
                raise Exception("No such user")
            users = {
                "wrong": update_achievery.UserStatus.wrong_level,
                "ok": update_achievery.UserStatus.ok,
                "without": update_achievery.UserStatus.without_achieve,
                "inactive": update_achievery.UserStatus.inactive,
            }
            return users["login"]

        def mock_edit(login, **kwargs):
            return True

        def mock_resolve(self, uids):
            return LOGINS

        monkeypatch.setattr(update_achievery.IntrasearchAchieveryManager, "_edit_achievement", mock_edit)
        monkeypatch.setattr(update_achievery.IntrasearchAchieveryManager, "_give_achievement", mock_edit)
        monkeypatch.setattr(update_achievery.IntrasearchAchieveryManager, "_check_achievement", mock_check_achievement)
        monkeypatch.setattr(update_achievery.IntrasearchAchieveryManager, "_resolve_uids", mock_resolve)

        path = "//marks"
        fill_yt(path, yt_proxy)
        update_achievery.IntrasearchAchieveryManager(
            token="",
            table_with_marks=path,
            yt_token="",
            proxy=yt_proxy,
        ).handle()

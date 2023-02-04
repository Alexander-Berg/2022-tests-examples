from mdh.core.integration.solomon import SolomonClient


def test_basic(response_mock):

    with response_mock(
            'POST https://solomon-prestable.yandex.net/api/v2/push?project=mdh&cluster=default&service=push -> 200:'
            'ok'
    ):
        assert SolomonClient().send([])

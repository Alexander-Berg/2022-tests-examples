from unittest import mock

import pytest
from aiohttp import hdrs
from aioresponses import aioresponses as base_aioresponses


class aioresponses(base_aioresponses):
    def __init__(self, *args, custom_mocker=None, **kwargs):
        super().__init__(*args, **kwargs)
        self._mocks = {}
        self._custom_mocker = custom_mocker or mock

    def add(self, url, method, *args, **kwargs):
        super().add(url, method, *args, **kwargs)
        matcher = list(self._matches.values())[-1]
        mock_ = self._mocks[matcher] = self._custom_mocker.Mock()
        return mock_

    def head(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_HEAD, **kwargs)

    def get(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_GET, **kwargs)

    def post(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_POST, **kwargs)

    def put(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PUT, **kwargs)

    def patch(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PATCH, **kwargs)

    def delete(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_DELETE, **kwargs)

    def options(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_OPTIONS, **kwargs)

    async def match(self, method, url, **kwargs):
        response = await super().match(method, url, **kwargs)
        if response is None:
            print('Failed to match:', method, url)  # noqa: T001
        else:
            for matcher, mock_ in self._mocks.items():
                if matcher.match(method, url):
                    mock_(**kwargs)
                    break
        return response


@pytest.fixture(autouse=True)
def aioresponses_mocker(mocker):
    with aioresponses(passthrough=['http://127.0.0.1:'], custom_mocker=mocker) as m:
        yield m

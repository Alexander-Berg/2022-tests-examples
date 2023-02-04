import time

from django.http import HttpResponse

from wiki.utils.request import ratelimiter


@ratelimiter(rate=5)
def dontcallmeoften():
    return True


def test_ratelimiter():
    for i in range(10):
        result = dontcallmeoften()
        if i < 5:
            assert result is True
        else:
            assert isinstance(result, HttpResponse)
            assert result.status_code == 429

    time.sleep(1)
    assert dontcallmeoften() is True

from django.http.response import HttpResponse

from kelvin.common.views import ping


def test_ping(rf):
    """
    Тест вьюхи с пингом
    """
    request = rf.get('')
    response = ping(request)
    assert isinstance(response, HttpResponse)
    assert response.status_code == 200
    assert response.content == b'pong'

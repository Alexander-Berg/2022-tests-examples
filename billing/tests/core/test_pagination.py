import pytest
from django.http import HttpResponse, HttpRequest, QueryDict

from refs.core.pagination import pagination_middleware, Paginator, Page, EmptyPage, pagination


@pytest.fixture
def response_from_pagination_middleware():
    def wrapper(get_response):
        pager = pagination_middleware(get_response)
        request = HttpRequest()
        request.META['HTTP_HOST'] = 'testserver'
        response = pager(request)
        return response

    return wrapper


def test_pagination_first(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        request.page = Paginator([1,2,3,4,5], per_page=2).page(1)
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)
    raw_link = response.get('Link')
    # '<http://testserver?page=2>; rel="next", <http://testserver?page=3>; rel="last"'
    links = {l.split(';')[1].strip(): l.split(';')[0].strip() for l in raw_link.split(',')}

    assert links['rel="next"'] == '<http://testserver?page=2>'
    assert links['rel="last"'] == '<http://testserver?page=3>'
    assert len(links) == 2


def test_pagination_middle(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        request.page = Paginator([1,2,3,4,5], per_page=2).page(2)
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)
    raw_link = response.get('Link')
    # '<http://testserver?page=2>; rel="next", <http://testserver?page=3>; rel="last"'
    links = {l.split(';')[1].strip(): l.split(';')[0].strip() for l in raw_link.split(',')}

    assert links['rel="first"'] == '<http://testserver?page=1>'
    assert links['rel="prev"'] == '<http://testserver?page=1>'
    assert links['rel="next"'] == '<http://testserver?page=3>'
    assert links['rel="last"'] == '<http://testserver?page=3>'
    assert len(links) == 4


def test_pagination_last(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        request.page = Paginator([1,2,3,4,5], per_page=2).page(3)
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)
    raw_link = response.get('Link')
    # '<http://testserver?page=2>; rel="next", <http://testserver?page=3>; rel="last"'
    links = {l.split(';')[1].strip(): l.split(';')[0].strip() for l in raw_link.split(',')}

    assert links['rel="first"'] == '<http://testserver?page=1>'
    assert links['rel="prev"'] == '<http://testserver?page=2>'
    assert len(links) == 2


def test_pagination_not_found(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        request.page = None
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)

    assert response.status_code == 404
    assert response.get('Link') is None


def test_pagination_did_not_set(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)

    assert response.status_code == 200
    assert response.get('Link') is None


def test_pagination_page_is_not_page_object(response_from_pagination_middleware):
    def get_response(request: 'HttpRequest') -> HttpResponse:
        request.page = 'hello'
        return HttpResponse(status=200)

    response = response_from_pagination_middleware(get_response)
    assert response.status_code == 200
    assert response.get('Link') is None


def test_pagination_decorator():
    class SomeView:
        @pagination(per_page=2)
        def resolve(self, info):
            return [1,2,3,4,5]

    class Info:
        # graphene.ResolveInfo
        def __init__(self, request):
            self.context = request

    request = HttpRequest()
    request.GET = QueryDict(query_string='page=1')
    info = Info(request)

    page = SomeView().resolve(info)
    assert page.number == 1
    assert page.paginator.num_pages == 3
    assert list(page) == [1, 2]
    assert request.page == page

    # мимо
    request.GET = QueryDict(query_string='page=999')

    with pytest.raises(EmptyPage):
        SomeView().resolve(info)
        assert request.page is None

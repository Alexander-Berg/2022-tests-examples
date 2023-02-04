from parts.routers import router
from parts.views import PartViewSet, SearcherViewSet


def test_searcher_parse_registered():
    assert_router_contains(SearcherViewSet)


def test_parts_view_set_registered():
    assert_router_contains(PartViewSet)


def assert_router_contains(viewset: type):
    for _, viewset_, basename in router.registry:
        if viewset_ == viewset:
            assert basename == viewset.BASE_NAME
            break
    else:
        assert False, f"{viewset.__name__} was not registered"

import pytest

from wiki.pages.models import Page
from wiki.api_v2.collections import CollectionFactory, OrderingQuery, PaginationQuery, Cursor

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def collection_page_factory():
    factory = CollectionFactory(name='page')
    factory.with_ordering({'author': ['owner__staff__first_name', 'owner__staff__last_name']})
    return factory


def test_cursor_serdes():
    # Очень важно чтобы курсор при сериализации не кастил типы
    c = Cursor(
        t=None,
        v=["0000000011", "true", True, 12345],
        i=1,
        p=1234,
    )
    spell = c.encode()
    c2 = Cursor.decode(spell)
    assert c2 == c


def test_collection__default(page_cluster, collection_page_factory):
    qs = Page.objects.all()
    pages = list(qs.order_by('pk'))

    serializer = lambda x: x  # noqa: E731
    ordering = OrderingQuery()

    # first page
    pagination = PaginationQuery(page_size=2)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering)
    assert collection.results == pages[:2]
    assert collection.prev_cursor is None and collection.next_cursor

    # next page
    pagination = PaginationQuery(page_size=2, cursor=collection.next_cursor)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering)
    assert collection.results == pages[2:4]
    assert collection.prev_cursor and collection.next_cursor

    # return back
    pagination = PaginationQuery(page_size=2, cursor=collection.prev_cursor)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering)
    assert collection.results == pages[:2]
    assert collection.prev_cursor is None and collection.next_cursor

    # go to end
    pagination = PaginationQuery(page_size=25, cursor=collection.next_cursor)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering)
    assert collection.results == pages[2:]
    assert collection.prev_cursor and collection.next_cursor is None


def test_collection__filter(page_cluster, collection_page_factory):
    qs = Page.objects.all()
    pages = list(qs.order_by('pk'))

    serializer = lambda x: x  # noqa: E731
    filter_ = lambda x: [i for i in x if i.id >= pages[4].id]  # noqa: E731
    ordering = OrderingQuery()

    pagination = PaginationQuery(page_size=2)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering, filter=filter_)
    assert collection.results == pages[4:6]
    assert collection.prev_cursor is None and collection.next_cursor

    # get part of qs
    pagination = PaginationQuery(page_size=5)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering, filter=filter_)
    assert collection.results == pages[4:5]
    assert collection.prev_cursor is None and collection.next_cursor


def test_collection__legacy(page_cluster, collection_page_factory):
    qs = Page.objects.all()
    pages = list(qs.order_by('pk'))

    serializer = lambda x: x  # noqa: E731
    ordering = OrderingQuery()

    pagination = PaginationQuery(page_size=2, page_id=2)
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering)
    assert collection.results == pages[2:4]
    assert collection.page_id == 2
    assert collection.has_next is True
    assert collection.prev_cursor is None and collection.next_cursor is None

    # filter + legacy могут возвращать пустые ответы
    filter_ = lambda x: []  # noqa: E731
    collection = collection_page_factory.ordered_build(qs, serializer=serializer, pagination=pagination,
                                                       ordering=ordering, filter=filter_)
    assert collection.results == []
    assert collection.page_id == 2
    assert collection.has_next is True
    assert collection.prev_cursor is None and collection.next_cursor is None

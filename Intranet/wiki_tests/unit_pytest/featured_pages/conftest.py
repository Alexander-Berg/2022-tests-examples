from dataclasses import dataclass
from typing import List, Optional, Tuple

import pytest
from django.conf import settings

from intranet.wiki.tests.wiki_tests.common.factories.featured_pages import FeaturedPageGroupFactory
from wiki.featured_pages.models import (
    LinkGroup,
    VisibilityFilter,
    DbLinksJsonSchema,
    DbLinkSchema,
    VisibilityOption,
)
from wiki.pages.models import Page

pytestmark = [pytest.mark.django_db]


def create_featured_page_group(
    pages: List[Tuple[Page, int]] = None,
    title='undefined',
    links: Optional[List[DbLinkSchema]] = None,
    visibility: Optional[VisibilityFilter] = None,
    rank: int = 0,
    org=None,
) -> LinkGroup:
    if visibility is None:
        visibility = VisibilityFilter(visibility=VisibilityOption.VISIBLE)
    links = DbLinksJsonSchema(links=links or [])

    return FeaturedPageGroupFactory(
        org=org, rank=rank, title=title, visibility=visibility.dict(), pages=pages, links=links.dict()
    )


@pytest.fixture()
def featured_pages_msk(page_cluster, test_org):
    """
    Москва 0
      - root 0
      - https://ya.ru Yandex 10
      - root/a 20
      - https://auto.ru AutoRu 25
    """
    return create_featured_page_group(
        rank=0,
        org=test_org,
        title='Москва',
        pages=[
            (page_cluster['root/a'], 20),
            (page_cluster['root'], 0),
        ],
        links=[
            DbLinkSchema(title='Yandex', url='https://ya.ru', rank=10),
            DbLinkSchema(title='AutoRu', url='https://auto.ru', rank=15),
        ],
    )


@pytest.fixture()
def featured_pages_spb(page_cluster, test_org):
    """
    Питер 10
      - root/b 0
      - root/a/aa 10
    """
    return create_featured_page_group(
        rank=10,
        org=test_org,
        title='Питер',
        pages=[
            (page_cluster['root/b'], 0),
            (page_cluster['root/a/aa'], 10),
        ],
    )


@pytest.fixture()
def geo():
    @dataclass
    class GeoFixture:
        ru: 'Country' = None
        il: 'Country' = None
        spb: 'City' = None
        msk: 'City' = None
        telaviv: 'City' = None
        redrose: 'Office' = None
        sarona: 'Office' = None
        neva: 'Office' = None

    if not settings.IS_INTRANET:
        return GeoFixture()

    from intranet.wiki.tests.wiki_tests.common.factories.city import CityFactory
    from intranet.wiki.tests.wiki_tests.common.factories.country import CountryFactory
    from intranet.wiki.tests.wiki_tests.common.factories.office import OfficeFactory

    ru = CountryFactory(name='Russia')
    ru.save()

    il = CountryFactory(name='Israel')
    il.save()

    telaviv = CityFactory(name='telaviv', country=il)
    telaviv.save()

    spb = CityFactory(name='spb', country=ru)
    spb.save()

    msk = CityFactory(name='msk', country=ru)
    msk.save()

    sarona = OfficeFactory(name='sarona', city=telaviv)
    sarona.save()

    neva = OfficeFactory(name='neva', city=spb)
    neva.save()

    redrose = OfficeFactory(name='rose', city=msk)
    redrose.save()

    return GeoFixture(ru=ru, il=il, spb=spb, msk=msk, redrose=redrose, neva=neva, sarona=sarona, telaviv=telaviv)

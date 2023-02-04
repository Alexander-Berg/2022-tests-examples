from furl import furl
from datetime import datetime
from typing import List, Optional

from auto.api import api_offer_model_pb2
from vertis.doppel import doppel_model_pb2

from app.core.config import settings
from app.db import models
from app.schemas.enums import AutoCategory, Source, Site, Status
from app.proto.parsing_auto_model_pb2 import ParsedOffer
from app.helpers.enum import is_valid_enum_value
from app.parsers.s3.auto.scrapinghub.cars.avito.parser import ScrapingHubAvitoCarsParser

from tortoise.contrib.test import TestCase


class BucketObj:
    def __init__(self, i):
        self.key = i

    async def get(self):
        return {"Body": "test"}


class AsyncIterator:
    def __init__(self, it_obj):
        self.it_obj = it_obj
        self.idx = -1

    def __aiter__(self):
        self.idx = -1
        return self

    async def __anext__(self):
        self.idx += 1
        if self.idx < len(self.it_obj):
            return self.it_obj[self.idx]
        else:
            raise StopAsyncIteration


class AsyncIteratorObjs:
    """
    Принимает список списков, на каждом новом проходе подставляет следующий список,
    когда списки заканчиваются StopAsyncIteration
    """
    def __init__(self, it_objs):
        self.it_objs = it_objs
        self.current_obj_id = -1

    def __aiter__(self):
        if self.current_obj_id < len(self.it_objs) - 1:
            self.current_obj_id += 1
        else:
            raise StopAsyncIteration
        self.current_obj = self.it_objs[self.current_obj_id]
        self.idx = -1
        return self

    async def __anext__(self):
        self.idx += 1
        if self.idx < len(self.current_obj):
            return self.current_obj[self.idx]
        else:
            raise StopAsyncIteration


async def get_async_wrapper(obj):
    return obj


def parse_offer_id(url: str, site: Optional[Site] = None):
    url = furl(url)
    if not site:
        site = Site.from_url(url)
    if site == Site.AVITO:
        return "".join(filter(str.isdigit, url.path.segments[-1].split("_")[-1]))
    elif site == Site.AMRU:
        return url.path.segments[-1].split("--")[1]
    elif site == Site.DROM:
        return "".join(filter(str.isdigit, url.path.segments[-1].split("-")[-1]))
    raise ValueError("Unknown site, can't parse offer id")


def get_test_auto_offer(
    url: str,
    status: Optional[Status] = Status.NEW,
    parsed_offer: Optional[ParsedOffer] = None,
    category: AutoCategory = AutoCategory.TRUCKS,
    geobase_id: int = 0,
    source: Source = Source.SCRAPPING_HUB,
    now: Optional[datetime] = None,
    doppel_clusters: Optional[List[doppel_model_pb2.Cluster]] = None,
    deactivation_dt: Optional[datetime] = settings.DEFAULT_DATETIME,
    listing_id: Optional[str] = "test_listing_id",
):
    if not parsed_offer:
        parsed_offer = ParsedOffer()
    if not now:
        now = datetime.now()
    site = Site.from_url(url)
    remote_id = f"{site}|{category}|{parse_offer_id(url)}".lower()
    url_hash = ScrapingHubAvitoCarsParser(url, {"listing_id": listing_id}).hash
    if not is_valid_enum_value(parsed_offer.offer.category, api_offer_model_pb2.Category):
        parsed_offer.offer.category = category.proto_value
    if not parsed_offer.offer.additional_info.remote_id:
        parsed_offer.offer.additional_info.remote_id = remote_id
    if not parsed_offer.offer.additional_info.remote_url:
        parsed_offer.offer.additional_info.remote_url = url
    if not parsed_offer.hash:
        parsed_offer.hash = url_hash
    if geobase_id != 0:
        parsed_offer.offer.seller.location.geobase_id = geobase_id
    if not doppel_clusters:
        doppel_cluster: doppel_model_pb2.Cluster = parsed_offer.doppel_cluster.add()
        doppel_cluster.enough_data = True
    else:
        parsed_offer.doppel_cluster.extend(doppel_clusters)
    return models.AutoOffer(
        hash=url_hash,
        category=category,
        status=status,
        site=site,
        url=url,
        data=parsed_offer.SerializeToString(),
        create_dt=now,
        update_dt=now,
        status_update_dt=now,
        sent_dt=None,
        deactivation_dt=deactivation_dt,
        source=source,
        call_center=None,
        offer_id=None,
        version=1,
    )


class TestCaseAutoModeAssertSavelMixin(TestCase):
    async def assert_save_result(self, offer: models.AutoOffer, created: bool = False, updated: bool = False):
        result = await offer.save()
        self.assertEqual(result[0], created)
        self.assertEqual(result[1], updated)

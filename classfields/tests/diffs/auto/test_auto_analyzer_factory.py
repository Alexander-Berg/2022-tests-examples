import json
from datetime import datetime, timedelta
from auto.api import api_offer_model_pb2

from app.schemas.enums import Status, Source
from app.diffs.analyzer_factory import DiffAnalyzerFactory
from app.helpers.formatters import optional_dt_to_iso
from app.constants import filters_reasons, offer_fields
from app.parsers.s3.auto.scrapinghub.cars.avito.parser import ScrapingHubAvitoCarsParser
from app.proto import common_model_pb2, parsing_auto_model_pb2
from tests.helpers import get_test_auto_offer


NEW_ADDRESS = "new address"
NEW_DESCRIPTION = "new description"
EXISTING_ADDRESS = "existing address"


def test_check_has_is_dealer_not_ignored(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.is_dealer.value = False
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert len(result.diffs) == 2
    diff_to_check = next(filter(lambda x: x.name != offer_fields.PARSE_DATE, result.diffs))
    assert diff_to_check.name == offer_fields.IS_DEALER
    assert not diff_to_check.ignored.value


def test_check_has_address(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.description = NEW_DESCRIPTION
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.offer.seller.location.address = EXISTING_ADDRESS
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert result.row.data_proto.offer.description == NEW_DESCRIPTION
    assert result.row.data_proto.offer.seller.location.address == EXISTING_ADDRESS
    assert len(result.diffs) == 3
    diff_to_check = next(filter(lambda x: x.name == offer_fields.ADDRESS, result.diffs))
    assert diff_to_check.ignored.value


def test_address_not_in_russia_no_existing_address(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.description = NEW_DESCRIPTION
    new_parsed_offer.offer.seller.location.geobase_id = 11514
    new_parsed_offer.offer.seller.location.address = NEW_ADDRESS
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert result.row.data_proto.offer.description == NEW_DESCRIPTION
    assert result.row.data_proto.offer.seller.location.geobase_id == 0
    assert not result.row.data_proto.offer.seller.location.address
    assert len(result.diffs) == 4
    diff_to_check = next(filter(lambda x: x.name == offer_fields.ADDRESS, result.diffs))
    assert diff_to_check.ignored.value
    diff_to_check = next(filter(lambda x: x.name == offer_fields.GEOBASE_ID, result.diffs))
    assert diff_to_check.ignored.value


def test_check_address_in_russia(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.description = NEW_DESCRIPTION
    new_parsed_offer.offer.seller.location.geobase_id = 11514
    new_parsed_offer.offer.seller.location.address = NEW_ADDRESS
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.offer.seller.location.geobase_id = 1
    old_parsed_offer.offer.seller.location.address = EXISTING_ADDRESS
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert result.row.data_proto.offer.description == NEW_DESCRIPTION
    assert result.row.data_proto.offer.seller.location.geobase_id == 1
    assert result.row.data_proto.offer.seller.location.address == EXISTING_ADDRESS
    assert len(result.diffs) == 4
    diff_to_check = next(filter(lambda x: x.name == offer_fields.ADDRESS, result.diffs))
    assert diff_to_check.ignored.value
    diff_to_check = next(filter(lambda x: x.name == offer_fields.GEOBASE_ID, result.diffs))
    assert diff_to_check.ignored.value


def test_geobase_id_in_russia(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.description = NEW_DESCRIPTION
    new_parsed_offer.offer.seller.location.geobase_id = 11514
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.offer.seller.location.geobase_id = 1
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert result.row.data_proto.offer.description == NEW_DESCRIPTION
    assert result.row.data_proto.offer.seller.location.geobase_id == 1
    assert len(result.diffs) == 3
    diff_to_check = next(filter(lambda x: x.name == offer_fields.GEOBASE_ID, result.diffs))
    assert diff_to_check.ignored.value


def test_check_has_geobase_id(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.description = NEW_DESCRIPTION
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.offer.seller.location.geobase_id = 1
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert result.row.data_proto.offer.description == NEW_DESCRIPTION
    assert result.row.data_proto.offer.seller.location.geobase_id == 1
    assert len(result.diffs) == 3
    diff_to_check = next(filter(lambda x: x.name == offer_fields.GEOBASE_ID, result.diffs))
    assert diff_to_check.ignored.value


def test_check_has_section(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.offer.section = api_offer_model_pb2.Section.USED
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert not result.row
    assert len(result.diffs) == 2
    diff_to_check = next(filter(lambda x: x.name != offer_fields.PARSE_DATE, result.diffs))
    assert diff_to_check.name == offer_fields.SECTION
    assert diff_to_check.ignored.value


def test_update_required(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.parse_date.seconds = 1
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer, deactivation_dt=datetime.now())
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row


def test_update_required_is_active(avito_trucks_url: str):
    for reason in (
        filters_reasons.OLDER_5_DAYS,
        filters_reasons.OLDER_20_DAYS,
        filters_reasons.TODAY_SENT,
        filters_reasons.RECENTLY_SENT,
        filters_reasons.TODAY_SENT_PHP_BY_PHONE,
        filters_reasons.RECENTLY_SENT_PHP_BY_PHONE,
    ):
        new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        new_parsed_offer.parse_date.seconds = 1
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
        old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        old_parsed_offer.status_history.add().status = common_model_pb2.Status.FILTERED
        old_parsed_offer.filter_reason.append(reason)
        old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
        old_offer.status = Status.FILTERED
        result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
        assert result.row


def test_is_import_possible_sent(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    for status in (Status.SENT, Status.PUBLISHED, Status.NOT_PUBLISHED):
        old_offer = get_test_auto_offer(avito_trucks_url)
        old_offer.status = status
        assert DiffAnalyzerFactory.is_import_possible(
            old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
        )


def test_is_import_possible_no_existing_row(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    assert DiffAnalyzerFactory.is_import_possible(
        None, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
    )


def test_is_import_possible_no_existing_parse_date(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_offer = get_test_auto_offer(avito_trucks_url)
    old_offer.status = Status.FILTERED
    assert DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
    )


def test_is_import_possible_no_existing_parse_date_for_same_source(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.parse_date.FromDatetime(datetime.now())
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    old_offer.source = Source.HARABA_FULL
    old_offer.status = Status.FILTERED
    assert DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
    )


def test_is_import_possible_no_existing_parse_date_for_same_source_2(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_offer = get_test_auto_offer(avito_trucks_url)
    old_offer.source = Source.SCRAPING_HUB_FULL
    old_offer.status = Status.FILTERED
    assert DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
    )


def test_is_import_possible_no_existing_parse_date_for_same_source_3(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now()}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    status_history_row = old_parsed_offer.status_history.add()
    status_history_row.source = Source.HARABA_FULL.proto_value
    diff_row = status_history_row.diff.add()
    diff_row.name = offer_fields.PARSE_DATE
    diff_row.new_value = optional_dt_to_iso(datetime.now())
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    assert DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), Source.SCRAPING_HUB_FULL
    )


def test_is_import_possible_existing_parse_date_newer(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now() - timedelta(hours=1)}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    source = Source.SCRAPING_HUB_FULL
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.parse_date.FromDatetime(datetime.now())
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    old_offer.source = source
    old_offer.status = Status.FILTERED
    assert not DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), source
    )


def test_is_import_possible_existing_parse_date_equal(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    dt = datetime.now()
    new_parsed_offer.json = json.dumps({"sh_last_visited": dt}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    source = Source.SCRAPING_HUB_FULL
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_parsed_offer.parse_date.FromDatetime(dt)
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    old_offer.source = source
    old_offer.status = Status.FILTERED
    assert not DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), source
    )


def test_is_import_possible_existing_parse_date_newer_2(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.json = json.dumps({"sh_last_visited": datetime.now() - timedelta(hours=1)}, default=str)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    source = Source.SCRAPING_HUB_FULL
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    status_history_row = old_parsed_offer.status_history.add()
    status_history_row.source = source.proto_value
    diff_row = status_history_row.diff.add()
    diff_row.name = offer_fields.PARSE_DATE
    diff_row.new_value = optional_dt_to_iso(datetime.now())
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    old_offer.source = Source.HARABA_FULL
    old_offer.status = Status.FILTERED
    assert not DiffAnalyzerFactory.is_import_possible(
        old_offer, ScrapingHubAvitoCarsParser(new_offer.url, new_offer.raw_json), source
    )


def test_analyze_existing_parse_date_equal(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    new_parsed_offer.offer.documents.year = 1996
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer)
    old_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    old_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=old_parsed_offer)
    result = DiffAnalyzerFactory(new_offer, old_offer).analyze()
    assert result.row
    assert len(result.diffs) == 1
    assert result.diffs[0].name == offer_fields.YEAR

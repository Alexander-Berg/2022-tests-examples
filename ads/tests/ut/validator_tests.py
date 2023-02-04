# coding: utf-8

from ads.quality.phf.phf_direct_loader.lib.campaign_generation.validator import (
    validate_banner_strings,
    TITLE_LENGTH_ERROR_CODE,
    TEXT_LENGTH_ERROR_CODE
)
from ads.quality.phf.phf_direct_loader.lib.campaign_generation.validator import DirectLimits
from ads.quality.phf.phf_direct_loader.lib.campaign_generation.expander import BannerTemplate

from ads.quality.phf.phf_direct_loader.tests.test_helpers import make_dummy_region


def test_generates_no_error_on_short_strings():
    banner_template = BannerTemplate(
        texts=["a"*(DirectLimits.MAX_BANNER_TEXT_LEN / 2)] * 3,
        titles=["a"*(DirectLimits.MAX_BANNER_TITLE_LEN / 2)] * 3,
        hrefs=['test']
    )

    regions = [make_dummy_region('test', 1)]

    assert not validate_banner_strings(banner_template, regions)


def test_single_error_on_long_title():
    banner_template = BannerTemplate(
        texts=["a" * (DirectLimits.MAX_BANNER_TEXT_LEN / 2)] * 3,
        titles=["a" * (DirectLimits.MAX_BANNER_TITLE_LEN * 2)],
        hrefs=['test']
    )

    regions = [make_dummy_region('test', 1)]

    errors = validate_banner_strings(banner_template, regions)
    assert len(errors) == 1
    assert errors[0].banner_string == banner_template.titles[0]
    assert errors[0].error_id == TITLE_LENGTH_ERROR_CODE
    assert errors[0].region_names == [regions[0].name]


def test_single_error_on_long_text():
    banner_template = BannerTemplate(
        texts=["a" * (DirectLimits.MAX_BANNER_TEXT_LEN * 2)],
        titles=["a" * (DirectLimits.MAX_BANNER_TITLE_LEN / 2)],
        hrefs=['test']
    )

    regions = [make_dummy_region('test', 1)]

    errors = validate_banner_strings(banner_template, regions)
    assert len(errors) == 1
    assert errors[0].banner_string == banner_template.texts[0]
    assert errors[0].error_id == TEXT_LENGTH_ERROR_CODE
    assert errors[0].region_names == [regions[0].name]


def test_error_on_single_invalid_region():
    banner_template = BannerTemplate(
        texts=[u"(Москва)"],
        titles=[u"(Москва)"],
        hrefs=['test']
    )

    regions = [make_dummy_region('test', 1),
               make_dummy_region('a' * (max(DirectLimits.MAX_BANNER_TITLE_LEN, DirectLimits.MAX_BANNER_TEXT_LEN) + 1),
                                 2)]

    errors = validate_banner_strings(banner_template, regions)

    assert len(errors) == 2  # both in title and text

    for error in errors:
        assert len(error.region_names) == 1
        assert error.region_names[0] == regions[1].name

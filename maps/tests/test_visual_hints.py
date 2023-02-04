from maps.doc.proto.testhelper.validator import Validator

from yandex.maps.proto.search.visual_hints_pb2 import (
    VisualHintsMetadata,
    SerpHints,
)

validator = Validator('search')


def test_visual_hints():
    metadata = VisualHintsMetadata()
    serp_hints = metadata.serp_hints
    card_hints = metadata.card_hints

    serp_hints.show_title = SerpHints.SHORT_TITLE
    serp_hints.show_category = SerpHints.ALL_CATEGORIES
    serp_hints.show_photo = SerpHints.GALLERY
    serp_hints.show_rating = SerpHints.FIVE_STAR_RATING

    serp_hints.show_work_hours = True
    serp_hints.show_bookmark = True
    serp_hints.show_verified = True
    serp_hints.show_eta = True
    serp_hints.show_geoproduct_offer = True

    card_hints.show_claim_organization = True
    card_hints.show_taxi_button = True
    card_hints.show_feedback_button = True
    card_hints.show_reviews = True
    card_hints.show_add_photo_button = True

    card_hints.show_competitors = False
    card_hints.show_direct_banner = False
    card_hints.show_additional_ads = False

    validator.validate_example(metadata, 'visual_hints')

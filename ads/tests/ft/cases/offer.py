import random

from ads.bsyeti.caesar.libs.profiles.proto.offer_pb2 import TOfferProfileProto
from ads.bsyeti.caesar.libs.profiles.python import yabs_md5_buffer
from ads.bsyeti.libs.events.proto.offer_base_pb2 import TOfferBase
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from ads.bsyeti.caesar.tests.ft.common.event import make_event


OFFER_LISTING_ID = "id2"
OFFER_LISTING_CANONIZED_URL = "ya.ru"


class _TestCaseOffersBase:
    table = "Offers"

    extra_profiles = {
        "NormalizedUrls": [],
        "OfferListingDict": [
            {"ID": OFFER_LISTING_ID, "TurboShopID": "yandex.ru", "CanonizedUrl": OFFER_LISTING_CANONIZED_URL}
        ],
    }

    def __init__(self):
        self.expected = {}


class TestCaseOfferResources(_TestCaseOffersBase):
    def __init__(self):
        super().__init__()

    def make_events(self, time, shard, profile_id):
        timestamp = time - random.randint(0, 60)
        body = ExportMessage()
        body.offer.version.version = random.randint(10, 100000)
        body.offer.timestamp.seconds = timestamp

        body.offer.offer_id = str(random.randint(1, 100000))
        body.offer.shop_id = random.randint(1, 100000)
        body.offer.price.price = random.randint(1, 100000)
        body.offer.price.old_price = random.randint(1, 100000)

        body.offer.original_content.url = "ya_{}.ru".format(profile_id)
        body.offer.original_content.description = "<p>description {}</p>".format(profile_id)
        body.offer.original_content.adult = bool(random.randint(0, 1))

        body.offer.market_content.category_id = random.randint(1, 100000)
        body.offer.market_content.category_name = "category {}".format(random.randint(1, 100000))
        body.offer.market_content.model_id = random.randint(1, 100000)
        body.offer.market_content.model_name = "model {}".format(random.randint(1, 100000))
        body.offer.market_content.vendor = random.randint(1, 100000)
        body.offer.market_content.vendor_name = "vendor {}".format(random.randint(1, 100000))
        body.offer.market_content.title = "title {}".format(random.randint(1, 100000))

        picture = body.offer.actual_pictures["picture"]
        original_picture = picture.original
        original_picture.url = "picture_{}.url.com".format(random.randint(1, 100000))
        original_picture.width = random.randint(1, 100000)
        original_picture.height = random.randint(1, 100000)
        original_picture.containerWidth = random.randint(1, 100000)
        original_picture.containerHeight = random.randint(1, 100000)

        event = make_event(
            ",".join([str(profile_id)] * 3),
            timestamp,
            body,
            "datacamp",
        )

        yield event

        if (
            event.ProfileID not in self.expected
            or self.expected[event.ProfileID].version.version <= body.offer.version.version
        ):
            self.expected[event.ProfileID] = body.offer
            self.expected[event.ProfileID].original_content.description = "description {}".format(profile_id)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            profile_id = ",".join(str(getattr(profile, x)) for x in ("OfferIDMd5", "BusinessID", "ShopID"))
            expected = self.expected[profile_id.encode("utf-8")]
            actual = profile.Resources

            assert profile.Flags.Source == TOfferProfileProto.TFlags.EOfferSource.DATACAMP
            assert profile.Flags.Type == TOfferProfileProto.TFlags.EOfferType.MARKET
            assert profile.Flags.Version == expected.version
            assert profile.Flags.UpdateTime == expected.timestamp.seconds
            assert actual.OfferID == expected.offer_id
            assert actual.FeedID == expected.feed_id
            assert actual.Version == expected.version
            assert actual.UpdateTime == expected.timestamp.seconds

            assert actual.MarketContent.ModelID == expected.market_content.model_id
            assert actual.MarketContent.CategoryID == expected.market_content.category_id
            assert actual.MarketContent.CategoryName == expected.market_content.category_name
            assert actual.MarketContent.Vendor == expected.market_content.vendor
            assert actual.MarketContent.VendorName == expected.market_content.vendor_name
            assert actual.MarketContent.ModelName == expected.market_content.model_name
            assert actual.MarketContent.Title == expected.market_content.title

            assert actual.Price.Currency == expected.price.currency
            assert actual.Price.Price == expected.price.price
            assert actual.Price.OldPrice == expected.price.old_price

            assert actual.OriginalContent.Url == expected.original_content.url
            assert actual.OriginalContent.Description == expected.original_content.description

            actual_picture = actual.ActualPictures.Pictures[0]
            expected_picture = expected.actual_pictures["picture"]
            assert actual_picture.Name == "picture"
            assert actual_picture.Height == expected_picture.original.height
            assert actual_picture.Width == expected_picture.original.width
            assert actual_picture.ContainerWidth == expected_picture.original.containerWidth
            assert actual_picture.ContainerHeight == expected_picture.original.containerHeight
            assert actual_picture.Url == expected_picture.original.url

            assert actual.Url.GeminiUrl or actual.Url.IsGeminiUrlBad


class TestCaseShubertOfferBase(_TestCaseOffersBase):
    def __init__(self):
        super().__init__()

    def make_events(self, time, shard, profile_id):
        timestamp = time - random.randint(0, 60)
        offer = TOfferBase()
        offer.Timestamp = timestamp

        offer.OfferID = str(random.randint(1, 100000))
        offer.Price.price = random.randint(1, 100000)
        offer.Price.old_price = random.randint(1, 100000)

        offer.Url = "yandex.ru/something_else"
        offer.Description = "<p>description {}</p>".format(profile_id)
        offer.Title = "title {}".format(random.randint(1, 100000))
        offer.MarketReviewsCount = random.randint(1, 100000)
        offer.MarketReviewsAvg = random.random()
        offer.FeedCategories.extend([5, 7, 11, 16])
        offer.ListingIDs.extend([OFFER_LISTING_ID, "id1"])

        embd = TOfferBase.TEmbedding()
        embd.ModelName = "prod_v10_enc_i2t_v12_200_img"
        embd.Vector.extend([0.04690000042319298, -0.004900000058114529, 0.1348000019788742])
        offer.ImageEmbeddings.extend([embd])

        picture = offer.Pictures["picture"]
        original_picture = picture.original
        original_picture.url = "picture_{}.url.com".format(random.randint(1, 100000))
        original_picture.width = random.randint(1, 100000)
        original_picture.height = random.randint(1, 100000)
        original_picture.containerWidth = random.randint(1, 100000)
        original_picture.containerHeight = random.randint(1, 100000)

        event = make_event(
            ",".join([str(profile_id)] * 3),
            timestamp,
            offer,
            "shubert",
        )

        yield event

        if event.ProfileID not in self.expected or self.expected[event.ProfileID].Timestamp < offer.Timestamp:
            self.expected[event.ProfileID] = offer
            self.expected[event.ProfileID].Description = "description {}".format(profile_id)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            profile_id = ",".join(str(getattr(profile, x)) for x in ("OfferIDMd5", "BusinessID", "ShopID"))
            expected = self.expected[profile_id.encode("utf-8")]
            actual = profile.Resources

            assert profile.Flags.Source == TOfferProfileProto.TFlags.EOfferSource.SHUBERT
            assert profile.Flags.Type == TOfferProfileProto.TFlags.EOfferType.TURBO
            assert profile.Flags.UpdateTime == expected.Timestamp
            assert profile.ImageEmbeddings.UpdateTime == expected.Timestamp
            assert len(profile.ImageEmbeddings.Vectors) == 1
            assert profile.ImageEmbeddings.Vectors[0].ModelName == expected.ImageEmbeddings[0].ModelName
            assert profile.ImageEmbeddings.Vectors[0].Vector == expected.ImageEmbeddings[0].Vector
            assert actual.OfferID == expected.OfferID
            assert actual.UpdateTime == expected.Timestamp

            assert actual.Price.Currency == expected.Price.currency
            assert actual.Price.Price == expected.Price.price
            assert actual.Price.OldPrice == expected.Price.old_price

            assert actual.MarketContent.Title == expected.Title
            assert actual.MarketContent.ReviewsCount == expected.MarketReviewsCount
            assert abs(actual.MarketContent.ReviewsAvg - expected.MarketReviewsAvg) < 0.00001
            assert actual.OriginalContent.Url == expected.Url
            assert actual.OriginalContent.Description == expected.Description
            assert len(actual.FeedCategories.Categories) == len(expected.FeedCategories)
            assert actual.FeedCategories.Categories == expected.FeedCategories

            assert len(actual.Listings.Listings) == len(expected.ListingIDs)
            for listing, id in zip(actual.Listings.Listings, expected.ListingIDs):
                assert listing.ID == id
                if listing.ID == OFFER_LISTING_ID:
                    assert listing.CanonizedUrlMd5 == yabs_md5_buffer(OFFER_LISTING_CANONIZED_URL)

            actual_picture = actual.ActualPictures.Pictures[0]
            expected_picture = expected.Pictures["picture"]
            assert actual_picture.Name == "picture"
            assert actual_picture.Height == expected_picture.original.height
            assert actual_picture.Width == expected_picture.original.width
            assert actual_picture.ContainerWidth == expected_picture.original.containerWidth
            assert actual_picture.ContainerHeight == expected_picture.original.containerHeight
            assert actual_picture.Url == expected_picture.original.url

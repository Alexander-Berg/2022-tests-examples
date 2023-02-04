import re
from typing import List, Optional

import pytest

from maps_adv.geosmb.landlord.proto.common_pb2 import Currency, Error, Money
from maps_adv.geosmb.landlord.proto.instagram_pb2 import (
    InstagramData,
    Settings,
)
from maps_adv.geosmb.landlord.proto.internal.instagram_internal_pb2 import (
    EditInstagramLandingInput,
    EditInstagramLandingOutput,
    SocialButtons,
)
from maps_adv.geosmb.landlord.proto.internal.landing_details_pb2 import ContactsInput
from maps_adv.geosmb.landlord.proto.organization_details_pb2 import (
    LandingType,
    LandingVersion,
    OrganizationDetails,
    OrganizationDetailsInput,
)
from maps_adv.geosmb.landlord.proto.preferences_pb2 import CTAButton, SocialButton
from maps_adv.geosmb.landlord.proto.contacts_pb2 import Contacts
from maps_adv.geosmb.landlord.server.lib.enums import (
    LandingVersion as LandingVersionEnum,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/edit_instagram_landing/"

INSTA_DATA_MIN = InstagramData(
    instagram_account="mangal", posts=[InstagramData.Post(id=1, caption="kebab")]
)

INSTA_DATA_FULL = InstagramData(
    instagram_account="mangal",
    posts=[
        InstagramData.Post(
            id=1,
            caption="kebab",
            media_urls=[
                InstagramData.Media(
                    type=InstagramData.Media.Type.IMAGE,
                    media_url="http://example.com/shaverma",
                    preview_url="http://example.com/shaverma-small",
                    internal_url="https://avatars.mds.yandex.net/get-tycoon/1638958/2a0000016fc2665bb5df96ae8c5b14c76905/orig",
                )
            ],
            offer=InstagramData.Offer(
                type=InstagramData.Offer.Type.PRODUCT,
                name="shaverma",
                price=Money(currency=Currency.RUB, amount=200),
                categories=["fast-food", "bistro"],
            ),
        )
    ],
)

CTA_BUTTON = CTAButton(custom="clickme", value="http://helloworld.org")

CONTACTS_INPUT = ContactsInput(website="http://helloworld.org", instagram="http://instagram.com/someprofile")
CONTACTS = Contacts(website="http://helloworld.org", instagram="http://instagram.com/someprofile")
CONTACTS_INPUT_NEW = ContactsInput(phone="+7 123 456-78-90")
CONTACTS_NEW = Contacts(phone="+7 123 456-78-90")

SOCIAL_BUTTONS = [
    SocialButton(type="VK", url="https://url1.com"),
    SocialButton(type="ZEN", url="https://url2.com", custom_text="some"),
]


async def edit_landing(
    api,
    biz_id: int,
    permalink: int = 0,
    instagram: InstagramData = None,
    cta: CTAButton = None,
    settings: Settings = None,
    contacts: ContactsInput = None,
    social_buttons: Optional[List[SocialButton]] = None,
) -> EditInstagramLandingOutput:
    return await api.post(
        URL,
        proto=EditInstagramLandingInput(
            biz_id=biz_id,
            permalink=permalink,
            instagram=instagram,
            cta_button=cta,
            settings=settings,
            contacts=contacts,
            social_buttons=(
                SocialButtons(buttons=social_buttons)
                if social_buttons is not None else None
            ),
        ),
        expected_status=200,
        decode_as=EditInstagramLandingOutput,
    )


async def fetch_landing(api, slug: str):
    return await api.post(
        "/v1/fetch_landing_data/",
        proto=OrganizationDetailsInput(
            slug=slug,
            token="fetch_data_token",
            version=LandingVersion.STABLE,
        ),
        expected_status=200,
        decode_as=OrganizationDetails,
    )


async def test_edit(api, factory):
    assert not await factory.fetch_biz_state(biz_id=628)

    got = await edit_landing(api, biz_id=628, permalink=12345, instagram=INSTA_DATA_MIN)
    assert got == EditInstagramLandingOutput(slug="mangal")

    biz_state = await factory.fetch_biz_state(biz_id=628)
    assert biz_state
    assert "mangal" == biz_state["slug"]
    assert biz_state["stable_version"]
    assert not biz_state["unstable_version"]

    saved_landing_data = await factory.fetch_landing_data(
        biz_id=628, kind=LandingVersionEnum.STABLE
    )
    assert saved_landing_data
    assert "INSTAGRAM" == saved_landing_data["landing_type"]

    saved = await fetch_landing(api, "mangal")
    assert LandingType.INSTAGRAM == saved.landing_type
    assert INSTA_DATA_MIN == saved.instagram
    assert "" == saved.name
    assert not saved.HasField("logo")
    assert not saved.HasField("description")
    assert saved.permalink == '12345'

    got = await edit_landing(api, biz_id=628, instagram=INSTA_DATA_FULL)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert LandingType.INSTAGRAM == saved.landing_type
    assert INSTA_DATA_FULL == saved.instagram


async def test_edit_slug_in_use(api):
    got = await edit_landing(api, biz_id=100, instagram=INSTA_DATA_MIN)
    assert got.slug == "mangal"
    got = await edit_landing(api, biz_id=200, instagram=INSTA_DATA_MIN)
    assert got.slug == "insta-mangal"
    got = await edit_landing(api, biz_id=300, instagram=INSTA_DATA_MIN)
    assert got.slug == "mangal-insta"
    got = await edit_landing(api, biz_id=400, instagram=INSTA_DATA_MIN)
    assert re.match("mangal-[0-9]{8}", got.slug)

    error = await api.post(
        URL,
        proto=EditInstagramLandingInput(biz_id=500, instagram=INSTA_DATA_MIN),
        expected_status=400,
        decode_as=Error,
    )
    assert error.code == Error.ERROR_CODE.SLUG_IN_USE


async def test_no_landing_to_edit(api):
    error = await api.post(
        URL,
        proto=EditInstagramLandingInput(biz_id=500, cta_button=CTA_BUTTON, contacts=CONTACTS_INPUT),
        expected_status=400,
        decode_as=Error,
    )
    assert error.code == Error.ERROR_CODE.BIZ_ID_UNKNOWN


async def test_edit_cta(api, factory):
    assert not await factory.fetch_biz_state(biz_id=296)

    got = await edit_landing(api, biz_id=296, instagram=INSTA_DATA_MIN, cta=CTA_BUTTON)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert saved.landing_type == LandingType.INSTAGRAM
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.preferences.cta_button == CTA_BUTTON

    cta_new = CTAButton(custom="callme", value="+7 123 456-78-90")
    got = await edit_landing(api, biz_id=296, cta=cta_new)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert saved.landing_type == LandingType.INSTAGRAM
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.preferences.cta_button == cta_new


async def test_edit_doesnt_mess_cta(api, factory):
    await edit_landing(api, biz_id=428, instagram=INSTA_DATA_MIN, cta=CTA_BUTTON)

    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.preferences.cta_button == CTA_BUTTON

    await edit_landing(api, biz_id=428, instagram=INSTA_DATA_FULL)
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_FULL
    assert saved.preferences.cta_button == CTA_BUTTON


async def test_edit_contacts(api, factory):
    assert not await factory.fetch_biz_state(biz_id=746)

    got = await edit_landing(api, biz_id=746, instagram=INSTA_DATA_MIN, contacts=CONTACTS_INPUT)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert saved.landing_type == LandingType.INSTAGRAM
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.contacts == CONTACTS

    got = await edit_landing(api, biz_id=746, contacts=CONTACTS_INPUT_NEW)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert saved.landing_type == LandingType.INSTAGRAM
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.contacts == CONTACTS_NEW


async def test_edit_doesnt_mess_contacts(api, factory):
    await edit_landing(api, biz_id=254, instagram=INSTA_DATA_MIN, contacts=CONTACTS_INPUT)

    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.contacts == CONTACTS

    await edit_landing(api, biz_id=254, instagram=INSTA_DATA_FULL)
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_FULL
    assert saved.contacts == CONTACTS


async def test_enrich_with_avatars(api, factory):
    posted = InstagramData(
        instagram_account="kathmandu",
        posts=[
            InstagramData.Post(
                id=1,
                caption="raz",
                media_urls=[
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="raz-1",
                    ),
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="raz-2",
                        internal_url="raz-2-internal",
                    ),
                ],
            ),
            InstagramData.Post(
                id=2,
                caption="dva",
                media_urls=[
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="dva-1",
                    )
                ],
            ),
        ],
    )

    await edit_landing(api, biz_id=640, instagram=posted)

    await factory.create_avatars(
        source_url="raz-1", avatars_group_id=10, avatars_name="razz"
    )
    await factory.create_avatars(
        source_url="raz-2", avatars_group_id=20, avatars_name="razz"
    )
    await factory.create_avatars(
        source_url="dva-2", avatars_group_id=20, avatars_name="dvaa"
    )

    saved = await fetch_landing(api, "kathmandu")
    assert saved.instagram == InstagramData(
        instagram_account="kathmandu",
        posts=[
            InstagramData.Post(
                id=1,
                caption="raz",
                media_urls=[
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="raz-1",
                        internal_url="http://avatars-outer-read.server/get-tycoon/10/razz/",
                    ),
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="raz-2",
                        internal_url="raz-2-internal",
                    ),
                ],
            ),
            InstagramData.Post(
                id=2,
                caption="dva",
                media_urls=[
                    InstagramData.Media(
                        type=InstagramData.Media.Type.IMAGE,
                        media_url="dva-1",
                    )
                ],
            ),
        ],
    )


async def test_edit_settings(api, factory):
    assert not await factory.fetch_biz_state(biz_id=281)

    settings = Settings(name="Peter", description="The Great", logo="http://logo.jpg")
    got = await edit_landing(api, biz_id=281, instagram=INSTA_DATA_MIN, settings=settings)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert not saved.preferences.HasField("cta_button")
    assert saved.name == "Peter"
    assert saved.description == "The Great"
    assert saved.logo.template_url == "http://logo.jpg"

    new_name = Settings(name="Alexander")
    got = await edit_landing(api, biz_id=281, settings=new_name)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.name == "Alexander"
    assert saved.description == "The Great"
    assert saved.logo.template_url == "http://logo.jpg"

    new_description = Settings(description="The Second")
    got = await edit_landing(api, biz_id=281, settings=new_description)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.name == "Alexander"
    assert saved.description == "The Second"
    assert saved.logo.template_url == "http://logo.jpg"

    new_logo = Settings(logo="http://avatar.png")
    got = await edit_landing(api, biz_id=281, settings=new_logo)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert saved.name == "Alexander"
    assert saved.description == "The Second"
    assert saved.logo.template_url == "http://avatar.png"


async def test_edit_social_buttons(api, factory):
    assert not await factory.fetch_biz_state(biz_id=297)

    got = await edit_landing(api, biz_id=297, instagram=INSTA_DATA_MIN, social_buttons=SOCIAL_BUTTONS)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert list(saved.preferences.social_buttons) == SOCIAL_BUTTONS

    new = []
    got = await edit_landing(api, biz_id=297, social_buttons=new)
    assert got == EditInstagramLandingOutput(slug="mangal")

    saved = await fetch_landing(api, "mangal")
    assert list(saved.preferences.social_buttons) == new


async def test_edit_doesnt_mess_social_buttons(api, factory):
    await edit_landing(api, biz_id=298, instagram=INSTA_DATA_MIN, social_buttons=SOCIAL_BUTTONS)

    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_MIN
    assert list(saved.preferences.social_buttons) == SOCIAL_BUTTONS

    await edit_landing(api, biz_id=298, instagram=INSTA_DATA_FULL)
    saved = await fetch_landing(api, "mangal")
    assert saved.instagram == INSTA_DATA_FULL
    assert list(saved.preferences.social_buttons) == SOCIAL_BUTTONS


async def test_edit_permalink(api, factory):
    assert not await factory.fetch_biz_state(biz_id=329)

    got = await edit_landing(api, biz_id=329, instagram=INSTA_DATA_MIN, cta=CTA_BUTTON, contacts=CONTACTS_INPUT, permalink=None)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.permalink == "0"

    got = await edit_landing(api, biz_id=329, permalink=0)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.permalink == "0"

    got = await edit_landing(api, biz_id=329, permalink=184)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.permalink == "184"

    got = await edit_landing(api, biz_id=329, permalink=None)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.permalink == "184"

    got = await edit_landing(api, biz_id=329, permalink=0)
    assert got == EditInstagramLandingOutput(slug="mangal")
    saved = await fetch_landing(api, "mangal")
    assert saved.permalink == "0"

    assert saved.instagram == INSTA_DATA_MIN
    assert saved.contacts == CONTACTS
    assert saved.preferences.cta_button == CTA_BUTTON

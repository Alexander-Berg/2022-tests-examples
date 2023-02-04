import pytest

from maps_adv.geosmb.landlord.proto.internal import suggests_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

URL = "/v1/suggests/color_presets/"


async def test_returns_all_available_presets(api):
    got = await api.get(
        URL,
        decode_as=suggests_pb2.ColorPresetsSuggest,
        expected_status=200,
    )

    assert got == suggests_pb2.ColorPresetsSuggest(
        options=[
            suggests_pb2.ColorPreset(preset="YELLOW", main_color_hex="FFD353"),
            suggests_pb2.ColorPreset(preset="GREEN", main_color_hex="00E087"),
            suggests_pb2.ColorPreset(preset="VIOLET", main_color_hex="6951FF"),
            suggests_pb2.ColorPreset(preset="RED", main_color_hex="FB524F"),
            suggests_pb2.ColorPreset(preset="BLUE", main_color_hex="3083FF"),
        ]
    )

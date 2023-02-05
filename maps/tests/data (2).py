from nile.api.v1 import Record


# Input
DWELLPLACES = [
    Record(
        ad_id=1,
        ds_lon=37.1,
        ds_lat=55.1,
        CDeviceID=b"devid1",
        ghash6=b"ghash1",
        total_time=86400,  # 24 hours
    ),
    Record(
        ad_id=2,
        ds_lon=37.2,
        ds_lat=55.2,
        CDeviceID=b"devid2",
        ghash6=b"ghash2",
        total_time=43200,  # 12 hours
    ),
    Record(
        ad_id=3,
        ds_lon=37.3,
        ds_lat=55.3,
        CDeviceID=b"devid3",
        ghash6=b"ghash3",
        total_time=36000,  # 10 hours
    ),
]
BUILDINGS = [
    Record(
        bld_area=1.,
        bld_id=1,
        bld_shape=b"bld1_shape",
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=True,
        bld_lon=37.1000001,
        bld_lat=55.1000001,
    ),
    Record(
        bld_area=2.,
        bld_id=2,
        bld_shape=b"bld2_shape",
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        bld_lon=37.1000002,
        bld_lat=55.1000002,
    ),
    Record(
        bld_area=2.,
        bld_id=2,
        bld_shape=b"bld2_shape",
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        bld_lon=37.2,
        bld_lat=55.2,  # will be filtered by distance
    ),
    Record(
        bld_area=1.,
        bld_id=3,
        bld_shape=b"bld3_shape",
        ghash6=b"ghash3",
        ghash9=b"ghash3_",
        has_addr=False,
        bld_lon=37.1,
        bld_lat=55.1,
    ),
]
GAID_CRYPTAID = [
    Record(
        id=b"devid1",
        target_id=b"cryptaid1",
    ),
    Record(
        id=b"devid2",
        target_id=b"cryptaid2",
    ),
    Record(
        id=b"devidX",
        target_id=b"cryptaid30",
    ),
]
IDFA_CRYPTAID = [
    Record(
        id=b"devidY",
        target_id=b"cryptaid10",
    ),
    Record(
        id=b"devidZ",
        target_id=b"cryptaid2",
    ),
    Record(
        id=b"devid3",
        target_id=b"cryptaid3",
    ),
]
MMDEVICE_CRYPTAID = [
    Record(
        device_id=b"mmdevid1",
        crypta_id=b"cryptaid1",
    ),
    Record(
        device_id=b"mmdevid1",
        crypta_id=b"cryptaid2",
    ),
    Record(
        device_id=b"mmdevid3",
        crypta_id=b"cryptaid3",
    ),
]
PUID_CRYPTAID = [
    Record(
        uid=b"uid1",
        crypta_id=b"cryptaid1",
    ),
    Record(
        crypta_id=b"cryptaid2",
    ),
    Record(
        uid=b"uid3",
        crypta_id=b"cryptaid3",
    ),
]
UUIDS = [
    Record(
        application=b"ru.yandex.yandexnavi",
        device_id=b"mmdevid1",
        uuid=b"uuid1",
    ),
    Record(
        application=b"ru.yandex.yandexnavi",
        device_id=b"mmdevid3",
        uuid=b"uuid3",
    ),
]


# Results
DWELLPLACES_WITH_BLD = [
    Record(
        ad_id=1,
        CDeviceID=b"devid1",
        ds_lat=55.1,
        ds_lon=37.1,
        bld_area=1.0,
        bld_id=1,
        bld_shape=b"bld1_shape",
        distance=0.0128251983,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=True,
        bld_lat=55.1000001,
        bld_lon=37.1000001,
        total_time=86400,  # 24 hours
    ),
    Record(
        ad_id=1,
        CDeviceID=b"devid1",
        ds_lat=55.1,
        ds_lon=37.1,
        bld_area=2.0,
        bld_id=2,
        bld_shape=b"bld2_shape",
        distance=0.0256503958,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        bld_lat=55.1000002,
        bld_lon=37.1000002,
        total_time=86400,  # 24 hours
    ),
    Record(
        ad_id=1,
        CDeviceID=b"devid1",
        ds_lat=55.1,  # will be filtered by distance
        ds_lon=37.1,
        bld_area=2.0,
        bld_id=2,
        bld_shape=b"bld2_shape",
        distance=12821.242222464247,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        bld_lat=55.2,
        bld_lon=37.2,
        total_time=86400,  # 24 hours
    ),
    Record(
        ad_id=3,
        CDeviceID=b"devid3",
        ds_lat=55.3,
        ds_lon=37.3,
        bld_area=1.0,
        bld_id=3,
        bld_shape=b"bld3_shape",
        distance=25634.575152241614,
        ghash6=b"ghash3",
        ghash9=b"ghash3_",
        has_addr=False,
        bld_lat=55.1,
        bld_lon=37.1,
        total_time=36000,  # 10 hours
    ),
]
FILTERED = [
    Record(
        ad_id=1,
        CDeviceID=b"devid1",
        ds_lat=55.1,
        ds_lon=37.1,
        bld_area=1.0,
        bld_id=1,
        bld_shape=b"bld1_shape",
        distance=0.0128251983,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=True,
        bld_lat=55.1000001,
        bld_lon=37.1000001,
        total_time=86400,  # 24 hours
    ),
    Record(
        ad_id=1,
        CDeviceID=b"devid1",
        ds_lat=55.1,
        ds_lon=37.1,
        bld_area=2.0,
        bld_id=2,
        bld_shape=b"bld2_shape",
        distance=0.0256503958,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        bld_lat=55.1000002,
        bld_lon=37.1000002,
        total_time=86400,  # 24 hours
    ),
]

DWELLPLACES_WITH_DEVIDS = [
    Record(
        CDeviceID=b"devid1",
        ad_id=1,
        bld_area=1.0,
        bld_id=1,
        bld_lat=55.1000001,
        bld_lon=37.1000001,
        bld_shape=b"bld1_shape",
        crypta_id=b"cryptaid1",
        device_id=b"mmdevid1",
        distance=0.0128251983,
        ds_lat=55.1,
        ds_lon=37.1,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=True,
        total_time=86400,  # 24 hours
        uid=b"uid1",
    ),
    Record(
        CDeviceID=b"devid1",
        ad_id=1,
        bld_area=2.0,
        bld_id=2,
        bld_lat=55.1000002,
        bld_lon=37.1000002,
        bld_shape=b"bld2_shape",
        crypta_id=b"cryptaid1",
        device_id=b"mmdevid1",
        distance=0.0256503958,
        ds_lat=55.1,
        ds_lon=37.1,
        ghash6=b"ghash1",
        ghash9=b"ghash1_",
        has_addr=False,
        total_time=86400,
        uid=b"uid1",
    ),
]

RESULT = [
    Record(
        ad_id=b"1",
        assignment_id=b"address_add:ghash1_",
        assignment_type="address_add",
        bld_lat=55.1000001,
        bld_lon=37.1000001,
        ds_lat=55.1,
        ds_lon=37.1,
        lat=55.1000001,
        lon=37.1000001,
        bld_area=1.0,
        application=b"ru.yandex.yandexnavi",
        data={},
        device_id=b"mmdevid1",
        distance=0.0128251983,
        ghash9=b"ghash1_",
        has_addr=True,
        uid=b"uid1",
        features={b"total_time": 86400, b"distance": 0.0128251983},
    ),
    Record(
        ad_id=b"1",
        assignment_id=b"address_add:ghash1_",
        assignment_type="address_add",
        bld_lat=55.1000002,
        bld_lon=37.1000002,
        ds_lat=55.1,
        ds_lon=37.1,
        lat=55.1000002,
        lon=37.1000002,
        bld_area=2.0,
        application=b"ru.yandex.yandexnavi",
        data={},
        device_id=b"mmdevid1",
        distance=0.0256503958,
        ghash9=b"ghash1_",
        has_addr=False,
        uid=b"uid1",
        features={b"total_time": 86400, b"distance": 0.0256503958},
    ),
]

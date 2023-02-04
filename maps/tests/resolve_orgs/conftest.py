from base64 import b64encode

import pytest
import snappy
from maps.doc.proto.yandex.maps.proto.photos import photos2_pb2
from yandex.maps.proto.atom import atom_pb2
from yandex.maps.proto.common2 import (
    attribution_pb2,
    geo_object_pb2,
    geometry_pb2,
    response_pb2,
)
from yandex.maps.proto.search import (
    address_pb2,
    business_images_pb2,
    business_pb2,
    business_rating_pb2,
    experimental_pb2,
    hours_pb2,
    kind_pb2,
    metrika_pb2,
    photos_2x_pb2,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def business_go_meta_multi():
    return [
        business_pb2.GeoObjectMetadata(
            id="54321",
            name="Кафе",
            address=address_pb2.Address(
                formatted_address="Улица, 1",
                component=[
                    address_pb2.Component(name="Улица", kind=[kind_pb2.Kind.STREET]),
                    address_pb2.Component(name="1", kind=[kind_pb2.Kind.HOUSE]),
                    address_pb2.Component(
                        name="Неизведанное", kind=[kind_pb2.Kind.UNKNOWN]
                    ),
                    address_pb2.Component(name="Россия", kind=[kind_pb2.Kind.COUNTRY]),
                    address_pb2.Component(name="Регион", kind=[kind_pb2.Kind.REGION]),
                    address_pb2.Component(
                        name="Провинция", kind=[kind_pb2.Kind.PROVINCE]
                    ),
                    address_pb2.Component(name="Область", kind=[kind_pb2.Kind.AREA]),
                    address_pb2.Component(name="Город", kind=[kind_pb2.Kind.LOCALITY]),
                    address_pb2.Component(name="Район", kind=[kind_pb2.Kind.DISTRICT]),
                    address_pb2.Component(name="Тудой", kind=[kind_pb2.Kind.ROUTE]),
                    address_pb2.Component(
                        name="Парк Культуры", kind=[kind_pb2.Kind.STATION]
                    ),
                    address_pb2.Component(
                        name="Московская",
                        kind=[
                            kind_pb2.Kind.METRO_STATION,
                            kind_pb2.Kind.RAILWAY_STATION,
                        ],
                    ),
                    address_pb2.Component(
                        name="Что-то про растения", kind=[kind_pb2.Kind.VEGETATION]
                    ),
                    address_pb2.Component(name="SVO", kind=[kind_pb2.Kind.AIRPORT]),
                    address_pb2.Component(name="Другое", kind=[kind_pb2.Kind.OTHER]),
                    address_pb2.Component(
                        name="Вход здесь", kind=[kind_pb2.Kind.ENTRANCE]
                    ),
                ],
            ),
            category=[
                business_pb2.Category(**{"name": "Общепит", "class": "eat"}),
                business_pb2.Category(**{"name": "Ресторан"}),
            ],
            phone=[
                business_pb2.Phone(
                    type=business_pb2.Phone.Type.Value("PHONE"),
                    formatted="+7 (495) 739-70-00",
                    number=0,
                    info="секретарь",
                    details=business_pb2.Phone.Details(
                        country="7", prefix="495", number="7397000"
                    ),
                ),
                business_pb2.Phone(
                    type=business_pb2.Phone.Type.Value("FAX"),
                    formatted="+7 (495) 739-70-11",
                    number=0,
                    info="секретарь-факс",
                    details=business_pb2.Phone.Details(
                        country="7", prefix="495", number="7397011"
                    ),
                ),
            ],
            open_hours=hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("MONDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=10800,
            ),
            link=[
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://cafe.ru"),
                    type=business_pb2.Link.Type.SELF,
                    tag="self",
                ),
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://haircut.com"),
                    type=business_pb2.Link.Type.SELF,
                    tag="self",
                ),
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://cafe.livejournal.com"),
                    type=business_pb2.Link.Type.SOCIAL,
                    tag="social",
                    aref="#lj",
                ),
            ],
            feature=[
                business_pb2.Feature(
                    id="feature_11",
                    name="Оплата картой",
                    value=business_pb2.Feature.Value(boolean_value=True),
                    aref="#yes.ru",
                ),
                business_pb2.Feature(
                    id="feature_22",
                    value=business_pb2.Feature.Value(boolean_value=False),
                ),
            ],
            snippet=business_pb2.Snippet(feature_ref=["feature_11"]),
            properties=business_pb2.Properties(
                item=[
                    business_pb2.Properties.Item(key="prop_key1", value="prop_value1"),
                    business_pb2.Properties.Item(key="prop_key2", value="prop_value2"),
                ]
            ),
            geocode_result=business_pb2.GeocodeResult(),
        ),
        business_pb2.GeoObjectMetadata(
            id="65432",
            name="Парикмахерская",
            address=address_pb2.Address(
                formatted_address="Проспект, 2",
                component=[
                    address_pb2.Component(name="Проспект", kind=[kind_pb2.Kind.STREET]),
                    address_pb2.Component(name="2", kind=[kind_pb2.Kind.HOUSE]),
                    address_pb2.Component(
                        name="Неизведанное", kind=[kind_pb2.Kind.UNKNOWN]
                    ),
                    address_pb2.Component(name="Россия", kind=[kind_pb2.Kind.COUNTRY]),
                    address_pb2.Component(name="Регион", kind=[kind_pb2.Kind.REGION]),
                    address_pb2.Component(
                        name="Провинция", kind=[kind_pb2.Kind.PROVINCE]
                    ),
                    address_pb2.Component(name="Область", kind=[kind_pb2.Kind.AREA]),
                    address_pb2.Component(name="Город", kind=[kind_pb2.Kind.LOCALITY]),
                    address_pb2.Component(name="Район", kind=[kind_pb2.Kind.DISTRICT]),
                    address_pb2.Component(name="Сюдой", kind=[kind_pb2.Kind.ROUTE]),
                    address_pb2.Component(name="Центр", kind=[kind_pb2.Kind.STATION]),
                    address_pb2.Component(
                        name="Питерская",
                        kind=[
                            kind_pb2.Kind.METRO_STATION,
                            kind_pb2.Kind.RAILWAY_STATION,
                        ],
                    ),
                    address_pb2.Component(
                        name="Трава", kind=[kind_pb2.Kind.VEGETATION]
                    ),
                    address_pb2.Component(name="PUL", kind=[kind_pb2.Kind.AIRPORT]),
                    address_pb2.Component(name="Другое", kind=[kind_pb2.Kind.OTHER]),
                    address_pb2.Component(
                        name="Вход там", kind=[kind_pb2.Kind.ENTRANCE]
                    ),
                ],
            ),
            category=[
                business_pb2.Category(**{"name": "Парикмахерская", "class": "hair"}),
                business_pb2.Category(**{"name": "Маникюр"}),
            ],
            phone=[
                business_pb2.Phone(
                    type=business_pb2.Phone.Type.Value("PHONE"),
                    formatted="+7 (833) 111-22-33",
                    number=0,
                    info="администратор",
                    details=business_pb2.Phone.Details(
                        country="7", prefix="833", number="1112233"
                    ),
                ),
                business_pb2.Phone(
                    type=business_pb2.Phone.Type.Value("FAX"),
                    formatted="+7 (833) 111-22-33",
                    number=0,
                    info="админ-факс",
                    details=business_pb2.Phone.Details(
                        country="7", prefix="833", number="1112233"
                    ),
                ),
            ],
            open_hours=hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("MONDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=5400,
            ),
            link=[
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://haircut.ru"),
                    type=business_pb2.Link.Type.SELF,
                    tag="self",
                ),
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://haircut.com"),
                    type=business_pb2.Link.Type.SELF,
                    tag="self",
                ),
                business_pb2.Link(
                    link=attribution_pb2.Link(href="http://haircut.livejournal.com"),
                    type=business_pb2.Link.Type.SOCIAL,
                    tag="social",
                    aref="#lj",
                ),
            ],
            feature=[
                business_pb2.Feature(
                    id="feature_11",
                    name="Оплата картой",
                    value=business_pb2.Feature.Value(boolean_value=True),
                    aref="#yes.ru",
                ),
                business_pb2.Feature(
                    id="feature_22",
                    value=business_pb2.Feature.Value(boolean_value=False),
                ),
            ],
            snippet=business_pb2.Snippet(feature_ref=["feature_11"]),
            properties=business_pb2.Properties(
                item=[
                    business_pb2.Properties.Item(key="prop_key1", value="prop_value1"),
                    business_pb2.Properties.Item(key="prop_key2", value="prop_value2"),
                ]
            ),
            geocode_result=business_pb2.GeocodeResult(),
        ),
    ]


@pytest.fixture
def photos_2x_go_meta_multi():
    return [
        photos_2x_pb2.GeoObjectMetadata(
            count=15,
            photo=[
                photos_2x_pb2.Photo(url_template="https://images.ru/tpl1/%s"),
                photos_2x_pb2.Photo(url_template="https://images.ru/tpl4/%s"),
            ],
        ),
        photos_2x_pb2.GeoObjectMetadata(
            count=25,
            photo=[
                photos_2x_pb2.Photo(url_template="https://images.ru/abc6/%s"),
                photos_2x_pb2.Photo(url_template="https://images.ru/abc4/%s"),
            ],
        ),
    ]


@pytest.fixture
def business_images_go_meta_multi():
    return [
        business_images_pb2.GeoObjectMetadata(
            logo=business_images_pb2.Logo(url_template="https://images.ru/logo/%s")
        ),
        business_images_pb2.GeoObjectMetadata(
            logo=business_images_pb2.Logo(url_template="https://images.ru/my_logo/%s")
        ),
    ]


@pytest.fixture
def sprav_proto_photos_exp_item():
    photos = [
        photos2_pb2.Entry(
            url_template="https://images.ru/tpl1/%s",
            tag=["Panorama"],
        ),
        photos2_pb2.Entry(url_template="http://url2", tag=["Panorama"], pending=True),
        photos2_pb2.Entry(
            url_template="http://url3",
            tag=["Logo"],
        ),
        photos2_pb2.Entry(
            url_template="https://images.ru/tpl4/%s",
            tag=[],
        ),
    ]
    entries = []
    for i, photo in enumerate(photos):
        entry = atom_pb2.Entry(id=f"id{i + 1}", author=atom_pb2.Author(name="fake"))
        entry.Extensions[photos2_pb2.ATOM_ENTRY].url_template = photo.url_template
        entry.Extensions[photos2_pb2.ATOM_ENTRY].tag.extend(photo.tag)
        entry.Extensions[photos2_pb2.ATOM_ENTRY].pending = photo.pending
        entries.append(entry)
    feed = atom_pb2.Feed(entry=entries)

    return experimental_pb2.ExperimentalStorage.Item(
        key="sprav_proto_photos",
        value=b64encode(snappy.compress(feed.SerializeToString())),
    )


@pytest.fixture
def business_rating_go_meta_multi():
    return [
        business_rating_pb2.GeoObjectMetadata(ratings=10, reviews=5, score=3.5),
        business_rating_pb2.GeoObjectMetadata(ratings=100, reviews=50, score=1.5),
    ]


@pytest.fixture
def metrika_go_meta_multi():
    return [
        metrika_pb2.MetrikaMetadata(counter="cnt322"),
        metrika_pb2.MetrikaMetadata(counter="cnt776"),
    ]


@pytest.fixture
def experimental_go_meta_multi(sprav_proto_photos_exp_item):
    return [
        experimental_pb2.ExperimentalMetadata(
            experimental_storage=experimental_pb2.ExperimentalStorage(
                item=[
                    experimental_pb2.ExperimentalStorage.Item(
                        key="online_snippets/1.x",
                        value='{"is_online":true,"hide_address":true,"service_radius_km":700,"description":[]}',  # noqa
                    ),
                    experimental_pb2.ExperimentalStorage.Item(
                        key="bookings/1.x",
                        value="{\"originalId\":\"yclients__328680\",\"standaloneWidgetPath\":\"/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta\",\"topServices\":[{\"id\":\"9799130\",\"title\":\"Подология\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017fe01130ea41e3bfce0aced61f45/smart-md\",\"price\":{\"currencyCode\":\"RUB\",\"range\":[500,8000]},\"description\":\"До конца Мая первичная консультация подолога в подарок!\",\"nearestDate\":\"2022-05-21\"},{\"id\":\"9658643\",\"title\":\"-20% На знакомство с NAIL-Мастером\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017fe424f74680fcddb237f0c2cc24/smart-md\",\"price\":{\"currencyCode\":\"RUB\",\"range\":[2640,4160]},\"description\":\"До конца месяца -20% на знакомство с мастерами маникюра. Успей записаться в MONE на Новокузнецкой!\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"9722128\",\"title\":\"-20% на первое посещение\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6161324/2a0000017fb0a4ed1fa0e416fd22bb66d54b/smart-md\",\"description\":\"Дорогие гости, мы дарим скидку -20% на первое посещение нашего салона!\\nДо встречи в MONE на Новокузнецкой!\",\"nearestDate\":\"2022-05-20\"}],\"partner\":{\"logoUrl\":\"https://avatars.mds.yandex.net/get-bunker/135516/07a49cbfc62fc2c5c6dba35b266da10e24eff809/svg\",\"linkUrl\":\"https://www.yclients.com/?utm_source=yandex&utm_medium=map\",\"legalName\":\"ООО «УАЙКЛАЕНТС»\"},\"nearestDate\":\"2022-05-27T10:00:00+0300\",\"topResources\":[{\"id\":\"1063231\",\"name\":\"Карине Кесоян\",\"rating\":5,\"reviewsCount\":128,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017f3eeac96098d26ca58d51dcbfcc/orig\",\"description\":\"Ведущий мастер маникюра\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"1939503\",\"name\":\"Журавлева Наталья\",\"rating\":5,\"reviewsCount\":4,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017f9c0b9d4ecd3e065bfa4ce1cdf6/orig\",\"description\":\"Мастер маникюра\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"1765566\",\"name\":\"Савинова Екатерина\",\"rating\":5,\"reviewsCount\":5,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6218691/2a0000017f5015f0cffb60d01031e61a7b52/orig\",\"description\":\"Мастер маникюра\",\"nearestDate\":\"2022-05-21\"}]}", # noqa
                    ),
                    sprav_proto_photos_exp_item,
                ]
            )
        ),
        experimental_pb2.ExperimentalMetadata(
            experimental_storage=experimental_pb2.ExperimentalStorage(
                item=[
                    experimental_pb2.ExperimentalStorage.Item(
                        key="online_snippets/1.x",
                        value='{"is_online":false,"hide_address":false,"service_radius_km":100,"description":["Описание"]}',  # noqa
                    ),
                    experimental_pb2.ExperimentalStorage.Item(
                        key="bookings/1.x",
                        value="{\"originalId\":\"yclients__328680\",\"standaloneWidgetPath\":\"/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta\",\"topServices\":[{\"id\":\"9799130\",\"title\":\"Подология\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017fe01130ea41e3bfce0aced61f45/smart-md\",\"price\":{\"currencyCode\":\"RUB\",\"range\":[500,8000]},\"description\":\"До конца Мая первичная консультация подолога в подарок!\",\"nearestDate\":\"2022-05-21\"},{\"id\":\"9658643\",\"title\":\"-20% На знакомство с NAIL-Мастером\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017fe424f74680fcddb237f0c2cc24/smart-md\",\"price\":{\"currencyCode\":\"RUB\",\"range\":[2640,4160]},\"description\":\"До конца месяца -20% на знакомство с мастерами маникюра. Успей записаться в MONE на Новокузнецкой!\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"9722128\",\"title\":\"-20% на первое посещение\",\"category\":\"Акции\",\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6161324/2a0000017fb0a4ed1fa0e416fd22bb66d54b/smart-md\",\"description\":\"Дорогие гости, мы дарим скидку -20% на первое посещение нашего салона!\\nДо встречи в MONE на Новокузнецкой!\",\"nearestDate\":\"2022-05-20\"}],\"partner\":{\"logoUrl\":\"https://avatars.mds.yandex.net/get-bunker/135516/07a49cbfc62fc2c5c6dba35b266da10e24eff809/svg\",\"linkUrl\":\"https://www.yclients.com/?utm_source=yandex&utm_medium=map\",\"legalName\":\"ООО «УАЙКЛАЕНТС»\"},\"nearestDate\":\"2022-05-27T10:00:00+0300\",\"topResources\":[{\"id\":\"1063231\",\"name\":\"Карине Кесоян\",\"rating\":5,\"reviewsCount\":128,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017f3eeac96098d26ca58d51dcbfcc/orig\",\"description\":\"Ведущий мастер маникюра\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"1939503\",\"name\":\"Журавлева Наталья\",\"rating\":5,\"reviewsCount\":4,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6221863/2a0000017f9c0b9d4ecd3e065bfa4ce1cdf6/orig\",\"description\":\"Мастер маникюра\",\"nearestDate\":\"2022-05-20\"},{\"id\":\"1765566\",\"name\":\"Савинова Екатерина\",\"rating\":5,\"reviewsCount\":5,\"image\":\"https://avatars.mds.yandex.net/get-yandex-bookings/6218691/2a0000017f5015f0cffb60d01031e61a7b52/orig\",\"description\":\"Мастер маникюра\",\"nearestDate\":\"2022-05-21\"}]}", # noqa
                    ),
                    sprav_proto_photos_exp_item,
                ]
            )
        ),
    ]


@pytest.fixture
def make_multi_response(
    business_go_meta_multi,
    photos_2x_go_meta_multi,
    business_images_go_meta_multi,
    business_rating_go_meta_multi,
    metrika_go_meta_multi,
    experimental_go_meta_multi,
):
    def response_generator() -> response_pb2.Response:
        resp = response_pb2.Response(
            reply=geo_object_pb2.GeoObject(
                geo_object=[
                    geo_object_pb2.GeoObject(
                        geometry=[
                            geometry_pb2.Geometry(
                                point=geometry_pb2.Point(lat=11.22, lon=22.33)
                            )
                        ]
                    ),
                    geo_object_pb2.GeoObject(
                        geometry=[
                            geometry_pb2.Geometry(
                                point=geometry_pb2.Point(lat=22.33, lon=44.55)
                            )
                        ]
                    ),
                ]
            )
        )

        for i in range(0, 2):
            metas = {
                business_pb2.GEO_OBJECT_METADATA: business_go_meta_multi[i],
                photos_2x_pb2.GEO_OBJECT_METADATA: photos_2x_go_meta_multi[i],
                business_images_pb2.GEO_OBJECT_METADATA: business_images_go_meta_multi[
                    i
                ],
                business_rating_pb2.GEO_OBJECT_METADATA: business_rating_go_meta_multi[
                    i
                ],
                metrika_pb2.GEO_OBJECT_METADATA: metrika_go_meta_multi[i],
                experimental_pb2.GEO_OBJECT_METADATA: experimental_go_meta_multi[i],
            }

            for key, value in metas.items():
                metadata = resp.reply.geo_object[i].metadata.add()
                metadata.Extensions[key].CopyFrom(value)

        return resp

    return response_generator

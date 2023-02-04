from sprav.protos import export_pb2
from sprav.protos import language_pb2

from yandex.maps import geolib3

from maps.garden.sdk.core import Version

from maps.garden.sdk import test_utils
from maps.garden.modules.altay.lib import altay
from maps.garden.modules.altay import defs

SHIPPING_DATE = "20190723"

BASE_INPUT_YT_PATH = "//home/altay/export"

COMPANY_FEATURES = [
    {
        "Id": "delivery",
        "ExportedValue": [
            {
                "TextValue": "1"
            }
        ]
    },
    {
        "Id": "consumer_services",
        "ExportedValue": [
            {
                "TextValue": "air_train_tickets_post"
            },
            {
                "TextValue": "lottery_post"
            }
        ]
    },
    {
        "Id": "pickup",
        "ExportedValue": [
            {
                "TextValue": "0"
            }
        ]
    },
    {
        "Id": "payment_by_credit_card",
        "ExportedValue": [
            {
                "TextValue": "1"
            }
        ]
    },
    {
        "Id": "rating",
        "ExportedValue": [
            {
                "TextValue": "4.2",
                "NumericValue": 4.2
            }
        ]
    },
    {
        "Id": "beer_price",
        "ExportedValue": [
            {
                "TextValue": "100–150 ₽",
                "MinValue": 100,
                "MaxValue": 150
            }
        ]
    },
    {
        "Id": "per_first_hour",
        "ExportedValue": [
            {
                "TextValue": "150 ₽"
            }
        ]
    },
    {
        "Id": "per_every_next",
        "ExportedValue": [
            {
                "TextValue": "100 ₽"
            }
        ]
    },
    {
        "Id": "height",
        "ExportedValue": [
            {
                "TextValue": "1500 m",
                "NumericValue": 1500
            }
        ]
    }
]
COMPANY_POINT = geolib3.Point2(10.0, 20.0)

company = export_pb2.TExportedCompany()
company.Id = 111
company.Source = "backa"
company.CommitId = 1675887888409037667
company.UnixTime = 1560792223000
company.Unreliable = False

icon_tag = company.Tag.add()
icon_tag.Id = "icon"
icon_tag.Value = "some_icon"
closed_for_visitors_tag = company.Tag.add()
closed_for_visitors_tag.Id = "closed_for_visitors"

name = company.Name.add()
name.Lang = language_pb2.RU
name.Value = "Roga i Copyta"

rubric = company.Rubric.add()
rubric.Id = 555

reference = company.Reference.add()
reference.Scope = "nyak"
reference.Id = "666"

company.Geo.Location.Pos.Lon = COMPANY_POINT.x
company.Geo.Location.Pos.Lat = COMPANY_POINT.y
company.Geo.Location.GeoId = 777

for feature in COMPANY_FEATURES:
    company_feature = company.Feature.add()
    company_feature.Id = feature["Id"]
    values = feature["ExportedValue"]
    for value in values:
        company_value = company_feature.ExportedValue.add()
        for k, v in value.items():
            setattr(company_value, k, v)

COMPANY = [
    {
        "id": 111,
        "duplicate_company_id": None,
        "export_proto": company.SerializeToString(),
        "is_exported": True,
        "providers": [
            {
                "provider_id": 3501752496
            }
        ]
    },
    {
        "id": 222,
        "duplicate_company_id": 111,
        "export_proto": None,
        "is_exported": False,
    },
]

RUBRIC = [
    {
        "id": 111,
        "permalink": "1000001",
        "commit_id" : 1,
        "parent_rubric_id": None,
        "rubric_class": "medicine",
        "nyak_ft_type_id": 1,
        "nyak_disp_class": 5
    },
    {
        "id": 222,
        "permalink": "1000002",
        "commit_id" : 2,
        "parent_rubric_id": 111,
        "rubric_class": None,
        "nyak_ft_type_id": 2,
        "nyak_disp_class": 5
    },
]

NYAK_MAPPING_UNKNOWN = [
    {
        "original_id": "1"
    },
    {
        "original_id": "2"
    },
]

COMPANIES_OUTPUT = [
    {
        "source_id": "111",
        "rubric_id": "555",
        "icon_class": "some_icon",
        "shape": COMPANY_POINT.to_EWKB(geolib3.SpatialReference.Epsg4326).hex().upper(),
        "x": 10.0,
        "y": 20.0,
        "features": [
            "delivery",
            "payment_by_credit_card"
        ],
        "parking_price": {
            "per_first_hour": "150 ₽",
            "per_every_next": "100 ₽"
        },
        "height": 1500,
        "is_closed": False,
        "is_closed_for_visitors": True,
        "is_temporarily_closed": False,
        "is_reliable": True
    },
]

NAMES_OUTPUT = [
    {
        "source_id": "111",
        "lang": "ru",
        "name": "Roga i Copyta"
    },
]

DUPLICATES_OUTPUT = [
    {
        "head_source_id": "111",
        "original_source_id": "111"
    },
    {
        "head_source_id": "111",
        "original_source_id": "222"
    }
]

REFERENCES_OUTPUT = [
    {
        "source_id": "111",
        "ft_id": 666
    }
]

RUBRICS_OUTPUT = [
    {
        "rubric_id": "1000001",
        "p_rubric_id": None,
        "icon_class": "medicine"
    },
    {
        "rubric_id": "1000002",
        "p_rubric_id": "1000001",
        "icon_class": None
    }
]

COMPANIES_UNKNOWN_OUTPUT = [
    {"ft_id": 1},
    {"ft_id": 2}
]

RUBRICS_FOR_OSM_OUTPUT = [
    {
        "rubric_id": "1000001",
        "p_rubric_id": None,
        "icon_class": "medicine",
        "ft_type_id": 1,
        "disp_class": 5
    },
    {
        "rubric_id": "1000002",
        "p_rubric_id": "1000001",
        "icon_class": None,
        "ft_type_id": 2,
        "disp_class": 5
    },
]

RESOURCE_NAME_TO_DATA = {
    defs.COMPANIES: COMPANIES_OUTPUT,
    defs.NAMES: NAMES_OUTPUT,
    defs.DUPLICATES: DUPLICATES_OUTPUT,
    defs.REFERENCES: REFERENCES_OUTPUT,
    defs.RUBRICS: RUBRICS_OUTPUT,
    defs.COMPANIES_UNKNOWN: COMPANIES_UNKNOWN_OUTPUT,
    defs.RUBRICS_FOR_OSM: RUBRICS_FOR_OSM_OUTPUT,
}


def create_cook_and_source_resource(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    altay.fill_graph(cook.target_builder())

    src_resource = cook.create_input_resource(defs.SRC_ALTAY)
    src_resource.version = Version(properties={
        "shipping_date": SHIPPING_DATE,
        "yt_path": BASE_INPUT_YT_PATH,
        "release_name": "release_name",
        "sandbox_resource_id": "sandbox_resource_id"
    })

    return cook, src_resource


def validate_yt_table_resources(resources):
    for resource in resources:
        if resource.name == defs.SRC_ALTAY:
            continue

        yt_client = resource.get_yt_client()
        result_data = list(yt_client.read_table(resource.path))

        expected_data = RESOURCE_NAME_TO_DATA[resource.name]

        assert result_data == expected_data,\
            "result={}; expected={}".format(result_data, expected_data)


def check_run(task, input_resources, output_resources, environment_settings):
    for resources in (input_resources, output_resources):
        for resource in resources.values():
            resource.load_environment_settings(environment_settings)

    task.load_environment_settings(environment_settings)
    task(**input_resources, **output_resources)

    validate_yt_table_resources(output_resources.values())


SANDBOX_REPLY = {
    "items": [
        {
            "skynet_id": "rbtorrent:4dc140d291b71c4bc1bd89a5a55d4bcfa8669dde",
            "task": {
                "status": "SUCCESS",
                "url": "http://sandbox.yandex-team.ru/api/v1.0/task/480693607",
                "id": 480693607
            },
            "http": {
                "proxy": "https://proxy.sandbox.yandex-team.ru/1060018924",
                "links": [
                    "http://sandbox-storage10.search.yandex.net:13578/resource/1060018924/backa-export2.tar",
                    "http://sandbox868.search.yandex.net:13578/7/0/480693607/backa-export2.tar",
                    "http://sandbox808.search.yandex.net:13578/7/0/480693607/backa-export2.tar"
                ]
            },
            "description": "Export archive",
            "rights": "read",
            "url": "http://sandbox.yandex-team.ru/api/v1.0/resource/1060018924",
            "file_name": "backa-export2.tar",
            "multifile": False,
            "state": "READY",
            "arch": "any",
            "time": {
                "accessed": "2019-08-05T10:48:55.737000Z",
                "updated": "2019-08-05T03:53:29.582000Z",
                "expires": "2019-08-19T10:48:55.737000Z",
                "created": "2019-08-05T03:32:59Z"
            },
            "owner": "ROBOT-ADMINS",
            "mds": None,
            "attributes": {
                "released": "stable",
                "state": "//home/altay/db/export/state-2019-08-04T18:05:35+0300",
                "backup_task": 480700336,
                "source": "new_altay/export"
            },
            "type": "MAPS_DATABASE_BUSINESS_SOURCE_ARCHIVE",
            "id": 1060018924,
            "md5": "24627346cf1c37a561d5f471d4190d34",
            "size": 8614860800
        }
    ],
    "total": 996,
    "limit": 1,
    "offset": 0
}

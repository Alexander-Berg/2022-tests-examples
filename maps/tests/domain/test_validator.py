import pytest

from maps_adv.geosmb.harmonist.server.lib.domain.validator import Validator
from maps_adv.geosmb.harmonist.server.lib.enums import ColumnType


@pytest.fixture
def validator():
    return Validator()


def test_validates_as_expected(validator):
    input_params = {
        "parsed_data": [
            [
                "Василий",
                "Кеков",
                "+7 (800) 200-06-00",
                "пёс какой-то",
                "kekov@ya.ru",
            ],
            ["Пётр", "Винигретов", "12", "не пёс", "vinigretov"],
            ["Иван", "Чебуреков", "", "", "cheburekov@ya.ru"],
        ],
        "markup": [
            {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
            {"column_type": ColumnType.LAST_NAME, "column_number": 1},
            {"column_type": ColumnType.PHONE, "column_number": 2},
            {"column_type": ColumnType.COMMENT, "column_number": 3},
            {"column_type": ColumnType.EMAIL, "column_number": 4},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert valid_clients == [
        {
            "first_name": "Василий",
            "last_name": "Кеков",
            "phone": 78002000600,
            "comment": "пёс какой-то",
            "email": "kekov@ya.ru",
        },
        {
            "first_name": "Иван",
            "last_name": "Чебуреков",
            "phone": None,
            "comment": None,
            "email": "cheburekov@ya.ru",
        },
    ]
    assert invalid_clients == [
        {
            "row": "Пётр;Винигретов;12;не пёс;vinigretov",
            "reason": {
                "email": ["Некорректный адрес электронной почты."],
                "contacts": ["Нужен как минимум один контакт: почта или телефон."],
                "phone": ["Номер телефона должен содержать от 3 до 16 цифр."],
            },
        }
    ]


@pytest.mark.parametrize(
    "column_type, value",
    [
        (ColumnType.FIRST_NAME, "Some valid last_name"),
        (ColumnType.LAST_NAME, "Some valid name"),
        (ColumnType.COMMENT, "Some valid name"),
        (ColumnType.PHONE, ""),  # empty phone
        (ColumnType.EMAIL, ""),  # empty email
    ],
)
def test_must_have_at_least_one_identity_field(column_type, value, validator):
    input_params = {
        "parsed_data": [[value]],
        "markup": [
            {"column_type": column_type, "column_number": 0},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert invalid_clients == [
        {
            "row": value,
            "reason": {
                "contacts": ["Нужен как минимум один контакт: почта или телефон."],
            },
        }
    ]
    assert valid_clients == []


@pytest.mark.parametrize(
    "column_type", [cl for cl in ColumnType if cl != ColumnType.DO_NOT_IMPORT]
)
def test_considers_value_as_none_if_column_number_out_of_index(column_type, validator):
    markup = [
        {"column_type": ColumnType.PHONE, "column_number": 0},
        {"column_type": column_type, "column_number": 5},
    ]
    expected_clients_data = {"phone": 880020006000, column_type.value: None}
    if column_type == ColumnType.PHONE:
        markup[0] = {"column_type": ColumnType.EMAIL, "column_number": 1}
        expected_clients_data = {"phone": None, "email": "valid@email.ru"}

    input_params = {
        "parsed_data": [["880020006000", "valid@email.ru", "some text"]],
        "markup": markup,
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert valid_clients == [expected_clients_data]
    assert invalid_clients == []


@pytest.mark.parametrize("column_type", [ColumnType.FIRST_NAME, ColumnType.LAST_NAME])
@pytest.mark.parametrize(
    "value, validated_value",
    [
        ("Машка", "Машка"),
        ("В" * 256, "В" * 256),
        ("", None),  # empty str converts to None
    ],
)
def test_validates_valid_names_fields_as_expected(
    value, validated_value, column_type, validator
):
    input_params = {
        "parsed_data": [[value, "88002000600"]],
        "markup": [
            {"column_type": column_type, "column_number": 0},
            {"column_type": ColumnType.PHONE, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert [item[column_type.value] for item in valid_clients] == [validated_value]
    assert invalid_clients == []


@pytest.mark.parametrize(
    "column_type, expected_reason",
    [
        (ColumnType.FIRST_NAME, "Имя не должно превышать 256 символов."),
        (ColumnType.LAST_NAME, "Фамилия не должна превышать 256 символов."),
    ],
)
def test_validates_invalid_names_fields_as_expected(
    column_type, expected_reason, validator
):
    input_params = {
        "parsed_data": [["В" * 257, "88002000600"]],
        "markup": [
            {"column_type": column_type, "column_number": 0},
            {"column_type": ColumnType.PHONE, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert invalid_clients == [
        {
            "row": f"{'В' * 257};88002000600",
            "reason": {column_type.value: [expected_reason]},
        }
    ]
    assert valid_clients == []


@pytest.mark.parametrize(
    "value, validated_value",
    [
        ("perfect@email.ru", "perfect@email.ru"),
        (f"{'i'*58}@ya.ru", f"{'i'*58}@ya.ru"),
        ("", None),  # empty str converts to None
    ],
)
def test_validates_valid_email_field_as_expected(value, validated_value, validator):
    input_params = {
        "parsed_data": [[value, "88002000600"]],
        "markup": [
            {"column_type": ColumnType.EMAIL, "column_number": 0},
            {"column_type": ColumnType.PHONE, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert [item["email"] for item in valid_clients] == [validated_value]
    assert invalid_clients == []


@pytest.mark.parametrize(
    "value, expected_reason",
    [
        ("@email.ru", "Некорректный адрес электронной почты."),
        ("only words", "Некорректный адрес электронной почты."),
        ("     another.perfect@", "Некорректный адрес электронной почты."),
        (f"{'i'*59}@ya.ru", "Адрес электронной почты не должен превышать 64 символа."),
    ],
)
def test_validates_invalid_email_field_as_expected(value, expected_reason, validator):
    input_params = {
        "parsed_data": [[value, "88002000600"]],
        "markup": [
            {"column_type": ColumnType.EMAIL, "column_number": 0},
            {"column_type": ColumnType.PHONE, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert invalid_clients == [
        {
            "row": f"{value};88002000600",
            "reason": {"email": [expected_reason]},
        }
    ]
    assert valid_clients == []


@pytest.mark.parametrize(
    "value, validated_value",
    [
        ("Some perfect comment", "Some perfect comment"),
        ("", None),  # empty str converts to None
    ],
)
def test_validates_comment_field_as_expected(value, validated_value, validator):
    input_params = {
        "parsed_data": [[value, "88002000600"]],
        "markup": [
            {"column_type": ColumnType.COMMENT, "column_number": 0},
            {"column_type": ColumnType.PHONE, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert [item["comment"] for item in valid_clients] == [validated_value]
    assert invalid_clients == []  # no reason to mark comment as invalid


@pytest.mark.parametrize(
    "value, validated_value",
    [
        ("+7 (906) 888-22-11", 79068882211),
        ("", None),  # empty str converts to None
    ],
)
def test_validates_valid_phone_field_as_expected(value, validated_value, validator):
    input_params = {
        "parsed_data": [[value, "email@ya.ru"]],
        "markup": [
            {"column_type": ColumnType.PHONE, "column_number": 0},
            {"column_type": ColumnType.EMAIL, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert [item["phone"] for item in valid_clients] == [validated_value]
    assert invalid_clients == []


@pytest.mark.parametrize(
    "value, expected_reason",
    [
        ("11", "Номер телефона должен содержать от 3 до 16 цифр."),
        ("1" * 17, "Номер телефона должен содержать от 3 до 16 цифр."),
        ("+ ( ) -", "Номер телефона должен содержать от 3 до 16 цифр."),
        ("abbc88002000600bd", "Номер телефона не должен содержать буквы."),
    ],
)
def test_validates_invalid_phone_field_as_expected(value, expected_reason, validator):
    input_params = {
        "parsed_data": [[value, "email@ya.ru"]],
        "markup": [
            {"column_type": ColumnType.PHONE, "column_number": 0},
            {"column_type": ColumnType.EMAIL, "column_number": 1},
        ],
    }

    valid_clients, invalid_clients = validator.validate_data(**input_params)

    assert invalid_clients == [
        {"reason": {"phone": [expected_reason]}, "row": f"{value};email@ya.ru"}
    ]
    assert valid_clients == []

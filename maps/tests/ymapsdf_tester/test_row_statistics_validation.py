import pytest

from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import row_statistics_validation


_REGION = "cis1"
_OLD_SHIPPING_DATE = "20211003_665826_2397_232801228"
_NEW_SHIPPING_DATE = "20211004_665826_2397_232801228"


@pytest.fixture(
    scope="function",
    params=[
        ({"ad": 1000}, {"ad": 9000}),       # new row number is less than the row validation limit
        ({"ft": {"transport-bus-stop": 1000}}, {"ft": {"transport-bus-stop": 11000}}),
        ({"ft": {}}, {"ft": 0, "ad": 0}),
        ({"ad": 0}, {"ft": {}}),
        ({"ad": 10000}, {"ad": 8001}),
        ({"ad": 10000}, {"ad": 19999}),
        (
            {
                "ft": {"some-type": 10000},
                "extra_poi": {"some-type": 4000},
            },
            {
                "ft": {"some-type": 8001},
                "extra_poi": {"some-type": 4000},
            },
        ),
        (
            {
                "ft": {"some-type": 10000},
                "extra_poi": {"some-type": 4000},
            },
            {
                "ft": {"some-type": 19999},
                "extra_poi": {"some-type": 7000},
            },
        ),
    ],
    ids=[
        "small_table",
        "bus_stop_exclusion",
        "zero_stats_validation",
        "zero_stats_validation2",
        "regular_table_significant_loss",
        "regular_table_significant_grow",
        "ft_table_significant_loss",
        "ft_table_significant_grow",
    ]
)
def valid_data(request):
    return request.param


def test_validation_pass(valid_data):
    (old_data, new_data) = valid_data
    errors = row_statistics_validation.get_validation_errors(
        old_data=old_data,
        new_data=new_data,
        )
    assert errors.empty


@pytest.fixture(
    scope="function",
    params=[
        ({"ad": 10000}, {"ad": 7999}),
        ({"ad": 10000}, {"ad": 20001}),
        (
            {
                "ft": {"some-type": 10000},
                "ft_in": {"some-type": 6000},
                "extra_poi": {"some-type": 4000},
            },
            {
                "ft": {"some-type": 7999},
                "ft_in": {"some-type": 5999},
                "extra_poi": {"some-type": 2000},
            },
        ),
        (
            {
                "ft": {"some-type": 10000},
                "ft_in": {"some-type": 6000},
                "extra_poi": {"some-type": 4000},
            },
            {
                "ft": {"some-type": 20001},
                "ft_in": {"some-type": 6001},
                "extra_poi": {"some-type": 14000},
            },
        ),
        (
            {
                "ft_nm": {"some-type": 10000},
                "extra_poi": {"some-type": 4000},
            },
            {
                "ft_nm": {"some-type": 20001},
                "extra_poi": {"some-type": 14000},
            },
        ),
    ],
    ids=[
        "regular_table_significant_loss",
        "regular_table_significant_grow",
        "ft_table_significant_loss",
        "ft_table_significant_grow",
        "ft_nm_table_significant_grow",
    ]
)
def broken_data(request):
    return request.param


def test_validation_fail_short_report(broken_data):
    (old_data, new_data) = broken_data
    errors = row_statistics_validation.get_validation_errors(
        old_data=old_data,
        new_data=new_data,
        )
    assert not errors.empty
    report = row_statistics_validation.Report(
        region=_REGION,
        old_shipping_date=_OLD_SHIPPING_DATE,
        new_shipping_date=_NEW_SHIPPING_DATE,
        errors=errors.errors,
    )
    return report.get_short_report()


def test_validation_fail_full_report(broken_data):
    (old_data, new_data) = broken_data
    errors = row_statistics_validation.get_validation_errors(
        old_data=old_data,
        new_data=new_data,
        )
    assert not errors.empty
    report = row_statistics_validation.Report(
        region=_REGION,
        old_shipping_date=_OLD_SHIPPING_DATE,
        new_shipping_date=_NEW_SHIPPING_DATE,
        errors=errors.errors,
    )
    return report.get_full_html_report()


def test_validation_fail_full_report_with_nmap_links():
    errors = row_statistics_validation.get_validation_errors(
        old_data={"ad": 10000},
        new_data={"ad": 20001},
        )
    assert not errors.empty
    report = row_statistics_validation.Report(
        region=_REGION,
        old_shipping_date=_OLD_SHIPPING_DATE,
        new_shipping_date=_NEW_SHIPPING_DATE,
        errors=errors.errors,
    )
    report.nmap_links[errors.errors[0].id] = [f"https://n.maps/#!/objects/{id}" for id in ["123", "456", "789"]]
    return report.get_full_html_report()


def test_validation_fail_full_report_with_altay_links():
    errors = row_statistics_validation.get_validation_errors(
        old_data={
            "ft": {"some-type": 10000},
            "ft_in": {"some-type": 6000},
            "extra_poi": {"some-type": 4000},
        },
        new_data={
            "ft": {"some-type": 20001},
            "ft_in": {"some-type": 6001},
            "extra_poi": {"some-type": 14000},
        },
        )
    assert not errors.empty
    report = row_statistics_validation.Report(
        region=_REGION,
        old_shipping_date=_OLD_SHIPPING_DATE,
        new_shipping_date=_NEW_SHIPPING_DATE,
        errors=errors.errors,
    )
    report.altay_links[errors.errors[0].id] = [f"https://altay/cards/{id}" for id in ["1", "22", "333", "4444"]]
    return report.get_full_html_report()

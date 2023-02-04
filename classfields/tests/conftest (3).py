from typing import Iterable
from xml.etree import ElementTree as ET

import pytest

from tests import rnd

T = ET.ElementTree


def E(tag, text=None, attrib=None, **extra):
    _ = ET.Element(tag, attrib=attrib or {}, **extra)
    if text:
        _.text = text
    return _


@pytest.fixture(scope="package")
def xml() -> ET.ElementTree:
    return T(E("Ads", attrib=dict(target="Avito.ru", formatVersion="3")))


# YYYY-MM-DD
# YYYY-MM-DDTHH:mm:ss+hh:mm
@pytest.fixture(scope="package", params=[("%Y-%m-%d",), ("%Y-%m-%d%H:%M:%S%z",)])
def avito_dates(request):
    dts = [rnd.date(), rnd.date()]
    dts.sort()
    return [d.strftime(*request.param) for d in dts]


@pytest.fixture(scope="package")
def avito_id() -> E:
    return E("AvitoId", text="some")


@pytest.fixture(params=[(rnd.str_(256),), (rnd.float_(180), rnd.float_(180))])
def address_or_lt_lg(request) -> Iterable[E]:
    if len(request.param) == 1:
        e1 = E("Address", text=request.param[0])
        return [e1]
    else:
        ltd, lgt = request.param
        return [E("Latitude", data=str(ltd)), E("Longitude", data=str(lgt))]


@pytest.fixture(params=[(), ("Package",), ("PackageSingle",), ("Single",)])
def listing_fee(request):
    if request.param:
        return E("ListingFee", text=request.param[0])


@pytest.fixture(params=[("Да",), ("Нет",)])
def allow_email(request) -> E:
    return E("AllowEmail", text=request.param[0])


@pytest.fixture
def manager_name():
    return E("ManagerName", text=rnd.str_())


@pytest.fixture(
    params=["+7 (495) 777-10-66", "(81374) 4-55-75", "8 905 207 04 90", "+7 905 2070490", "88123855085", "9052070490",]
)
def contact_phone(request):
    return E("ContactPhone", text=request.param)


@pytest.fixture(params=["Товар приобретен на продажу", "Товар от производителя"])
def ad_type(request) -> E:
    return E("AdType", text=request.param)

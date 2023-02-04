from datetime import date

import pytest

from staff.departments.models import Department, DepartmentKind
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentKindFactory,
    OfficeFactory,
    OrganizationFactory,
    CityFactory,
    CountryFactory,
)
from staff.map.models import City, Country, Office
from staff.person.models import Organization


class DepartmentsAndOffices:
    kind_root: DepartmentKind
    kind_shulgin: DepartmentKindFactory
    kind_direction: DepartmentKindFactory
    kind_division: DepartmentKindFactory
    kind_regular: DepartmentKindFactory
    yandex: Department
    subyandex: Department
    infra: Department
    extra: Department
    intranet: Department
    matrixnet: Department
    staffnet: Department
    shmyandex: Department
    robots: Department
    xenomorphs: Department
    zombies: Department
    robots: Department
    plants: Department
    this_country: Country
    moscow: City
    yandex_org: Organization
    red_rose: Office
    that_country: Country
    palo_alto: City
    yandex_labs: Organization
    palo_alto_office: Office
    someday: date


@pytest.fixture
def departments_and_offices(settings) -> DepartmentsAndOffices:
    r"""
    Department trees: (lft name rght)
    +--------------------------------------------------------------------+
    |          (1 Yandex 14)                   (1 Shmyandex 12)          |
    |               |                             /      \               |
    |         (2 subyandex 13 )                  /        \              |
    |            /      \                       /          \             |
    |    (3 Infra 10)  (11 Extra 12)       (2 Humans 7) (8 Xenomorphs 11)|
    |        /                           /       \             \         |
    | (4 Intranet 9)           (3 Zombies 4) (5 Robots 6)  (9 Plants 10) |
    |       |                                                            |
    | (5 MatrixNet 8)                                                    |
    |       |                                                            |
    | (6 StaffNet 7)                                                     |
    +--------------------------------------------------------------------+

                      +--------------------------------------------------+
    Countries:        |  "This country"               "That country"     |
                      |         |                         |              |
    Cities:           |      Moscow                   Palo Alto          |
                      |      /   \                      /     \          |
    Organizations:    |  Yandex   \             Yandex Labs    \         |
    Offices:          |         Red Rose                      Palo Alto  |
                      +--------------------------------------------------+
    """
    result = DepartmentsAndOffices()
    result.kind_root = DepartmentKindFactory(rank=10)
    result.kind_shulgin = DepartmentKindFactory(rank=20)
    result.kind_direction = DepartmentKindFactory(rank=30)
    result.kind_division = DepartmentKindFactory(rank=40)
    result.kind_regular = DepartmentKindFactory(rank=50)

    settings.DIS_ROOT_KIND_ID = result.kind_root.id
    settings.DIS_DIRECTION_KIND_ID = result.kind_direction.id
    settings.DIS_DIVISION_KIND_ID = result.kind_division.id

    result.yandex = DepartmentFactory(id=1, parent=None, name='Yandex', kind=result.kind_root)
    result.subyandex = DepartmentFactory(id=2, parent=result.yandex, name='Yandex', kind=result.kind_shulgin)
    result.infra = DepartmentFactory(id=3, parent=result.subyandex, name='Infra', kind=result.kind_direction)
    result.extra = DepartmentFactory(id=4, parent=result.subyandex, name='Extra', kind=result.kind_direction)
    result.intranet = DepartmentFactory(id=5, parent=result.infra, name='Intranet', kind=result.kind_division)
    result.matrixnet = DepartmentFactory(id=6, parent=result.intranet, name='Intranet', kind=result.kind_regular)
    result.staffnet = DepartmentFactory(id=7, parent=result.matrixnet, name='Intranet', kind=result.kind_regular)
    result.shmyandex = DepartmentFactory(id=8, name='Shmyandex', parent=None, kind=result.kind_root)
    result.humans = DepartmentFactory(id=9, parent=result.shmyandex, name='Humans', kind=result.kind_direction)
    result.xenomorphs = DepartmentFactory(id=10, parent=result.shmyandex, kind=result.kind_direction, name='Xenomorphs')
    result.zombies = DepartmentFactory(id=11, parent=result.humans, name='Zombies', kind=result.kind_division)
    result.robots = DepartmentFactory(id=12, parent=result.humans, name='Robots', kind=result.kind_division)
    result.plants = DepartmentFactory(id=13, parent=result.xenomorphs, name='Plants', kind=result.kind_division)

    Department.tree.rebuild()

    result.this_country = CountryFactory(name='This country')
    result.moscow = CityFactory(name='Moscow', country=result.this_country)
    result.yandex_org = OrganizationFactory(name='Yandex', city=result.moscow)
    result.red_rose = OfficeFactory(name='Red Rose', city=result.moscow)

    result.that_country = CountryFactory(name='That country')
    result.palo_alto = CityFactory(name='Palo Alto', country=result.that_country)
    result.yandex_labs = OrganizationFactory(name='Yandex Labs', city=result.palo_alto)
    result.palo_alto_office = OfficeFactory(name='Palo Alto', city=result.palo_alto)

    result.someday = date(2014, 2, 1)

    return result

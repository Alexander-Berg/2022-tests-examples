from datetime import timedelta

import pytest

from staff.lib.testing import (
    CityFactory,
    CountryFactory,
    DepartmentFactory,
    OfficeFactory,
    OrganizationFactory,
    StaffFactory,
)

from staff.lenta.models import LentaLog, ACTION_CHOICES


@pytest.mark.django_db
def test_handling_changes(settings):
    settings.LENTA_UNFIRE_INTERVAL = 1
    # given
    yandex = DepartmentFactory(name='Яндекс', parent=None)
    shmyandex = DepartmentFactory(name='Шмяндекс', parent=None)

    this_country = CountryFactory(name='Эта страна')
    that_country = CountryFactory(name='Та страна')
    moscow = CityFactory(name='Moscow', country=this_country)
    palo_alto_city = CityFactory(name='Palo Alto', country=that_country)

    red_rose = OfficeFactory(name='Red Rose', city=moscow)
    palo_alto = OfficeFactory(name='Palo Alto', city=palo_alto_city)

    yandex_org = OrganizationFactory(name='Yandex', city=moscow)
    yandex_labs = OrganizationFactory(name='Yandex Labs', city=palo_alto_city)

    # when
    # """
    # Проверка сигналов для сбора данных в ленте
    # и кооректности записываемых значений. В ленталоге появляется запись:
    # * при создании пользователя
    # * при смене должности
    # * при смене департамента
    # * при смене департамента и должности
    # * при увольнении
    # * при возврате после увольнения
    # * при изменении офиса
    # """

    # мышь приняли в Яндекс. Мышь не умеет программировать компьютеры,
    # именно поэтому мы и выбрали мышь, потому что все остальнуе умеют.

    from staff.person.controllers import Person

    mouse = StaffFactory(
        login='mouse',
        login_ld='mouse',
        department=yandex,
        position='Программист компьютеров',
        is_dismissed=False,
        office=red_rose,
        organization=yandex_org,
    )

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 1
    assert record.action == ACTION_CHOICES.HIRED
    assert record.staff == mouse
    assert record.department_new == yandex
    assert record.position_new == mouse.position
    assert record.office_new == mouse.office
    assert record.office_old is None
    assert record.organization_new == mouse.organization
    assert record.organization_old is None

    # мышь повысили в тот же день
    mouse.position = 'Старший программист компьютеров'
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 2
    assert record.action == ACTION_CHOICES.CHANGED_POSITION
    assert record.staff == mouse
    assert record.department_old == yandex
    assert record.department_new == yandex
    assert record.position_new == mouse.position
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # мышь перевели
    mouse.department = shmyandex
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 3
    assert record.action == ACTION_CHOICES.TRANSFERRED
    assert record.staff == mouse
    assert record.department_old == yandex
    assert record.department_new == shmyandex
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # мышь переманили обратно, т.к. ценный сотрудник и повысили
    mouse.department = yandex
    mouse.position = 'Руководитель группы программистов компьютеров'
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 4
    assert record.action == ACTION_CHOICES.TRANSFERRED_AND_CHANGED_POSITION
    assert record.staff == mouse
    assert record.position_old == 'Старший программист компьютеров'
    assert record.position_new == mouse.position
    assert record.department_old == shmyandex
    assert record.department_new == yandex
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # мышь уволилась
    mouse.is_dismissed = True
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 5
    assert record.action == ACTION_CHOICES.DISMISSED
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.department_old == yandex
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # история продолжается: мышь вернулась в новом качестве
    # убедимся, что уволилась она достаточно давно
    record.created_at -= timedelta(10)
    record.save()
    mouse.is_dismissed = False
    mouse.department = shmyandex
    mouse.position = 'Руководитель службы программистов компьютеров'
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 6
    assert record.action == ACTION_CHOICES.RETURNED
    assert record.staff == mouse
    assert record.position_old == 'Руководитель группы программистов компьютеров'
    assert record.position_new == mouse.position
    assert record.department_old == yandex
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # однажды её случано уволили
    mouse.is_dismissed = True
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 7
    assert record.action == ACTION_CHOICES.DISMISSED
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.position_new == mouse.position
    assert record.department_old == mouse.department
    assert record.office_new == mouse.office
    assert record.office_old == red_rose
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # хотя быстро поняли свою ошибку и восстановили. никто и не заметил.
    mouse.is_dismissed = False
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 6
    assert record.action == ACTION_CHOICES.RETURNED
    assert record.staff == mouse
    assert record.position_old == 'Руководитель группы программистов компьютеров'
    assert record.position_new == mouse.position
    assert record.department_old == yandex
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # мышке дали headcount
    p_mouse = Person(mouse)
    p_mouse.oebs_headcount = True
    p_mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 7
    assert record.action == ACTION_CHOICES.CHANGED_HEADCOUNT
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.position_new == mouse.position
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization
    assert record.headcount_new
    assert not record.headcount_old

    # а потом отобрали headcount
    p_mouse.oebs_headcount = False
    p_mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 8
    assert record.action == ACTION_CHOICES.CHANGED_HEADCOUNT
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.position_new == mouse.position
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization
    assert not record.headcount_new
    assert record.headcount_old

    # после такого мышь хрюкнула и решила изменить страну
    mouse.office = palo_alto
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 9
    assert record.action == ACTION_CHOICES.MOVED
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.position_new == mouse.position
    assert record.department_old == mouse.department
    assert record.office_new == mouse.office
    assert record.office_old == red_rose
    assert record.organization_new == mouse.organization
    assert record.organization_old == mouse.organization

    # и организацию
    mouse.organization = yandex_labs
    mouse.save()

    record = LentaLog.objects.order_by('-id')[0]
    assert LentaLog.objects.count() == 10
    assert record.action == ACTION_CHOICES.CHANGED_ORGANIZATION
    assert record.staff == mouse
    assert record.position_old == mouse.position
    assert record.position_new == mouse.position
    assert record.department_old == mouse.department
    assert record.office_new == mouse.office
    assert record.office_old == mouse.office
    assert record.organization_new == mouse.organization
    assert record.organization_old == yandex_org

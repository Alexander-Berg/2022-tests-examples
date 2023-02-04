from django.conf import settings

from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    DepartmentKindFactory,
    DepartmentStaffFactory,
)


def create_department_infrastructure(obj):
    """          yandex
                    |
               subyandex
               /        \
        direction_1    direction_2
            |               |     \
        division_1     division_2  division_3
            |               |
        regular_1      regular_2
    """
    from staff.departments.models import Department, DepartmentRoles

    obj.dep_kind_lvl_0 = DepartmentKindFactory(rank=10, slug=settings.TOP_DEPARTMENT_TYPES[0])
    obj.dep_kind_lvl_1 = DepartmentKindFactory(rank=20, slug=settings.TOP_DEPARTMENT_TYPES[1])
    obj.dep_kind_lvl_2 = DepartmentKindFactory(rank=30)
    obj.dep_kind_lvl_3 = DepartmentKindFactory(rank=40)
    obj.dep_kind_lvl_4 = DepartmentKindFactory(rank=50)

    # Departments
    obj.dep_yandex = DepartmentFactory(
        id=settings.YANDEX_DEPARTMENT_ID,
        url='dep_yandex',
        parent=None,
        name='Яндекс',
        kind=obj.dep_kind_lvl_0,
    )
    obj.dep_subyandex = DepartmentFactory(
        url='dep_subyandex',
        parent=obj.dep_yandex,
        name='СубЯндекс',
        kind=obj.dep_kind_lvl_1,
    )
    obj.dep_direction_1 = DepartmentFactory(
        parent=obj.dep_subyandex,
        url='dep_direction_1',
        kind=obj.dep_kind_lvl_2,
    )
    obj.dep_division_1 = DepartmentFactory(
        parent=obj.dep_direction_1,
        url='dep_division_1',
        kind=obj.dep_kind_lvl_3,
    )
    obj.dep_regular_1 = DepartmentFactory(
        parent=obj.dep_division_1,
        url='dep_regular_1',
        kind=obj.dep_kind_lvl_4,
    )
    obj.dep_direction_2 = DepartmentFactory(
        parent=obj.dep_subyandex,
        url='dep_direction_2',
        kind=obj.dep_kind_lvl_2,
    )
    obj.dep_division_2 = DepartmentFactory(
        parent=obj.dep_direction_2,
        url='dep_division_2',
        kind=obj.dep_kind_lvl_3,
    )
    obj.dep_regular_2 = DepartmentFactory(
        parent=obj.dep_division_2,
        url='dep_regular_2',
        kind=obj.dep_kind_lvl_4,
    )
    obj.dep_division_3 = DepartmentFactory(
        parent=obj.dep_direction_2,
        url='dep_division_3',
        kind=obj.dep_kind_lvl_3,
    )

    Department.tree.rebuild()

    # Chiefs (except for dep_regular_1)
    obj.chief_yandex = StaffFactory(
        department=obj.dep_yandex,
        login='chief_yandex'
    )
    obj.chief_subyandex = StaffFactory(
        department=obj.dep_subyandex,
        login='chief_subyandex'
    )
    obj.chief_direction_1 = StaffFactory(
        department=obj.dep_direction_1,
        login='chief_direction_1'
    )
    obj.chief_division_1 = StaffFactory(
        department=obj.dep_division_1,
        login='chief_division_1'
    )
    obj.chief_direction_2 = StaffFactory(
        department=obj.dep_direction_2,
        login='chief_direction_2'
    )
    obj.chief_division_2 = StaffFactory(
        department=obj.dep_division_2,
        login='chief_division_2'
    )
    obj.chief_regular_2 = StaffFactory(
        department=obj.dep_regular_2,
        login='chief_regular_2'
    )
    obj.chief_division_3 = StaffFactory(
        department=obj.dep_division_3,
        login='chief_division_3'
    )

    # Deputies (except for dep_regular_1)
    obj.deputy_yandex = StaffFactory(
        department=obj.dep_yandex,
        login='deputy_yandex'
    )
    obj.deputy_subyandex = StaffFactory(
        department=obj.dep_subyandex,
        login='deputy_subyandex'
    )
    obj.deputy_direction_1 = StaffFactory(
        department=obj.dep_direction_1,
        login='deputy_direction_1'
    )
    obj.deputy_division_1 = StaffFactory(
        department=obj.dep_division_1,
        login='deputy_division_1'
    )
    obj.deputy_direction_2 = StaffFactory(
        department=obj.dep_direction_2,
        login='deputy_direction_2'
    )
    obj.deputy_division_2 = StaffFactory(
        department=obj.dep_division_2,
        login='deputy_division_2'
    )
    obj.deputy_regular_2 = StaffFactory(
        department=obj.dep_regular_2,
        login='deputy_regular_2'
    )
    obj.deputy_division_3 = StaffFactory(
        department=obj.dep_division_3,
        login='deputy_division_3'
    )

    # Employees
    obj.person_yandex = StaffFactory(
        department=obj.dep_yandex,
        login='person_yandex'
    )
    obj.person_subyandex = StaffFactory(
        department=obj.dep_subyandex,
        login='person_subyandex'
    )
    obj.person_direction_1 = StaffFactory(
        department=obj.dep_direction_1,
        login='person_direction_1'
    )
    obj.person_division_1 = StaffFactory(
        department=obj.dep_division_1,
        login='person_division_1'
    )
    obj.person_regular_1 = StaffFactory(
        department=obj.dep_regular_1,
        login='person_regular_1'
    )
    obj.person_direction_2 = StaffFactory(
        department=obj.dep_direction_2,
        login='person_direction_2'
    )
    obj.person_division_2 = StaffFactory(
        department=obj.dep_division_2,
        login='person_division_2'
    )
    obj.person_regular_2 = StaffFactory(
        department=obj.dep_regular_2,
        login='person_regular_2'
    )
    obj.person_division_3 = StaffFactory(
        department=obj.dep_division_3,
        login='person_division_3'
    )

    obj.chief_yandex_role = DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_yandex,
        department=obj.dep_yandex,
    )

    obj.chief_subyandex_role = DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_subyandex,
        department=obj.dep_subyandex,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_direction_1,
        department=obj.dep_direction_1,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_division_1,
        department=obj.dep_division_1,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_direction_2,
        department=obj.dep_direction_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_division_2,
        department=obj.dep_division_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_regular_2,
        department=obj.dep_regular_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.CHIEF.value,
        staff=obj.chief_division_3,
        department=obj.dep_division_3,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_yandex,
        department=obj.dep_yandex,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_subyandex,
        department=obj.dep_subyandex,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_direction_1,
        department=obj.dep_direction_1,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_division_1,
        department=obj.dep_division_1,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_direction_2,
        department=obj.dep_direction_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_division_2,
        department=obj.dep_division_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_regular_2,
        department=obj.dep_regular_2,
    )

    DepartmentStaffFactory(
        role_id=DepartmentRoles.DEPUTY.value,
        staff=obj.deputy_division_2,
        department=obj.dep_division_3,
    )

    # пересохраняем струтуру
    for subobj, data in obj.__dict__.items():
        if hasattr(data, 'id'):
            setattr(obj, subobj, data.__class__.objects.get(id=data.id))

from collections import defaultdict
from typing import Dict, List, Optional

from django.db.models import signals

from staff.lib.testing import (
    CityFactory,
    CountryFactory,
    DepartmentAttrsFactory,
    DepartmentFactory,
    ValueStreamFactory,
    GeographyDepartmentFactory,
    DepartmentKindFactory,
    DepartmentStaffFactory,
    DepartmentInterfaceSectionFactory,
    factory,
    GroupFactory,
    OfficeFactory,
    OrganizationFactory,
    StaffFactory,
    UserFactory,
)

from staff.departments.controllers.tickets.helpers import department_attrs as dep_attrs_getter
from staff.departments.models import Department, DepartmentRoles, DepartmentKind, DepartmentInterfaceSection, \
    ValuestreamRoles
from staff.departments.tests.factories import VacancyFactory
from staff.groups.models import Group, GROUP_TYPE_CHOICES
from staff.map.models import Office
from staff.oebs.tests.factories import JobFactory, OrganizationFactory as OebsOrganizationFactory
from staff.oebs.tests.sync_test import EmployeeFactory
from staff.person.models import AFFILIATION, Organization, Staff
from staff.proposal.models import CityAttrs


class AttrDict(dict):
    def __getattr__(self, key):
        if key in self:
            return self[key]
        return super(AttrDict, self).__getattr__(key)

    def __setattr__(self, key, value):
        self[key] = value
        return super(AttrDict, self).__setattr__(key, value)

    def reload(self):
        """
        Temp kludge to make tests independent when using common fixture with module or session scopes
        """
        for subobj, instance in self.__dict__.items():
            if hasattr(instance, 'id'):
                instance_class_manager = getattr(instance, 'instance_class_manager', instance.__class__.objects)
                setattr(self, subobj, instance_class_manager.get(id=instance.id))


def create_kinds():
    """Список kind'ов подразделений"""
    return list([DepartmentKind.objects.get(id=x.id) for x in [
        DepartmentKindFactory(name='Бизнес-группа', name_en='Бизнес-группа', slug='5'),
        DepartmentKindFactory(name='Поднаправление', name_en='Поднаправление', slug='22'),
        DepartmentKindFactory(name='Департамент', name_en='Департамент (1)', slug='11'),
        DepartmentKindFactory(name='Дивизион', name_en='Дивизион (1)', slug='21'),
        DepartmentKindFactory(name='Бригада', name_en='Бригада (1)', slug='61'),
        DepartmentKindFactory(name='Отдел', name_en='Отдел (1)', slug='31'),
        DepartmentKindFactory(name='Служба', name_en='Служба (1)', slug='41'),
        DepartmentKindFactory(name='Группа', name_en='Группа (1)', slug='51'),
    ]])


def create_map_models() -> AttrDict:
    """
    :return:  AttrDict object with:
        countries, offices, cities, cityattrs
    """
    result = AttrDict()

    ru = CountryFactory(name='Россия', name_en='Russia', yandex_domain='yandex.ru', code='ru')
    ua = CountryFactory(name='Украина', name_en='Ukraine', yandex_domain='yandex.ua', code='ua')
    ge = CountryFactory(name='Германия', name_en='Germany', yandex_domain='yandex.com', code='ge')
    by = CountryFactory(name='Беларусь', name_en='Belarus', yandex_domain='yandex.by', code='by')
    countries = {ru.id: ru, ua.id: ua, ge.id: ge, by.id: by}

    moscow = CityFactory(name='Москва', name_en='Moscow', geo_id=1, country=ru)
    kyiv = CityFactory(name='Киев', name_en='Kyiv', geo_id=2, country=ua)
    berlin = CityFactory(name='Берлин', name_en='Berlin', geo_id=3, country=ge)
    minsk = CityFactory(name='Минск', name_en='Minsk', geo_id=4, country=by)
    cities = {moscow.id: moscow, kyiv.id: kyiv, berlin.id: berlin, minsk.id: minsk}

    offices = {
        'KR': OfficeFactory(name='Красная роза', name_en='Red Rose', code='kr', city=moscow),
        'virtual': OfficeFactory(name='Виртуальный офис', name_en='Virtual office', code='virt'),
        'homie': OfficeFactory(name='Надомник', name_en='Home office', code='home', city=moscow),
        'MRP': OfficeFactory(name='Рубин Плаза', name_en='Rubin Plaza', code='mrp', city=minsk)
    }

    testing_cityattrs_components = {
        berlin: 25665,
        kyiv: 25667,
        minsk: 25668,
        moscow: 25483,
        None: 44620,
    }
    cityattrs = {}
    for city, attr_component in testing_cityattrs_components.items():
        attr = CityAttrs.objects.create(city=city, component=attr_component)
        cityattrs[attr.id] = attr

    result.update(
        {
            'countries': countries,
            'offices': offices,
            'cities': cities,
            'cityattrs': cityattrs,
        }
    )
    return result


def fill_departmentattrs_with_roles(company: AttrDict) -> None:
    """
    Default analyst: yandex-person
    Default budget_owner: yandex-chief
    Default budget_notify: [yandex-person]

    login\role        |   budget_holder  |  budget_owner  |    analyst    |  budget_notify               ticket_access
    ====================================================================================================================
    yandex-chief      |    yandex            yandex_dep1     yandex_dep2   yandex_dep1_dep12
    yandex-person     |        -             yandex_dep2         -         yandex_dep2 None
    dep2-person       |        -                 -           yandex_dep1            -
    dep111-person     |        -                 -         yandex_dep1_dep12        -
    dep12-chief       |        -                 -               -        yandex_dep1_dep12 yandex_dep2

    dep11-chief       |                                                                                yandex_dep1_dep12
    dep1-hr-analyst   |                                                                                  yandex_dep1
    dep12-person      |                                                                                  yandex_dep1
    dep2-hr-partner   |                                                                                  yandex_dep2
    dep1-chief        |                                                                                  yandex_dep2
    """

    dep = company.departments
    pers = company.persons

    department_attrs = {}
    budget_owners = defaultdict(list)

    default_attrs = DepartmentAttrsFactory(
        department=None,
        analyst=pers['yandex-person'],
        budget_owner=pers['yandex-chief'],
        budget_tag='Тэг0',
    )
    default_attrs.ticket_access.add(company.persons['dep1-hr-analyst'])
    department_attrs[None] = default_attrs

    dep_attrs1 = DepartmentAttrsFactory(
        department=dep['yandex_dep1'],
        analyst=pers['dep2-person'],
        budget_owner=pers['yandex-chief'],
        budget_tag='Тэг1',
    )
    # new code should use role provided through IDM instead of department attrs
    DepartmentStaffFactory(
        staff=pers['dep2-person'],
        department=dep['yandex_dep1'],
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )

    dep_attrs1.ticket_access.add(company.persons['dep1-hr-partner'])
    dep_attrs1.ticket_access.add(company.persons['dep12-person'])
    department_attrs['yandex_dep1'] = dep_attrs1
    budget_owners['dep2-person'].append(dep['yandex_dep1'])

    dep_attrs2 = DepartmentAttrsFactory(
        department=dep['yandex_dep2'],
        analyst=pers['yandex-chief'],
        budget_owner=pers['yandex-person'],
        budget_tag='Тэг2',
    )
    DepartmentStaffFactory(
        staff=pers['yandex-chief'],
        department=dep['yandex_dep2'],
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )
    dep_attrs2.ticket_access.add(company.persons['dep2-hr-partner'])
    dep_attrs2.ticket_access.add(company.persons['dep1-chief'])
    department_attrs['yandex_dep2'] = dep_attrs2
    budget_owners['yandex-person'].append(dep['yandex_dep2'])

    dep_attrs3 = DepartmentAttrsFactory(
        department=dep['yandex_dep1_dep12'],
        analyst=pers['dep111-person'],
        budget_owner=None,
        budget_tag='Тэг3',
    )
    DepartmentStaffFactory(
        staff=pers['dep111-person'],
        department=dep['yandex_dep1_dep12'],
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )

    dep_attrs3.ticket_access.add(pers['dep11-chief'])
    department_attrs['yandex_dep1_dep12'] = dep_attrs3

    default_attrs.budget_notify.add(pers['yandex-person'])
    dep_attrs2.budget_notify.add(pers['yandex-person'])
    dep_attrs2.budget_notify.add(pers['dep12-chief'])
    dep_attrs3.budget_notify.add(pers['dep12-chief'])

    dep_attrs3.budget_notify.add(pers['yandex-chief'])

    dep_attrs_getter.cache_one = {}
    dep_attrs_getter.cache_all = {}
    dep_attrs_getter.cached_attrs = None
    dep_attrs_getter.cached_default = None

    company['department_attrs'] = department_attrs
    company['budget_owners'] = dict(budget_owners)


def _create_department_roled_person(login: str, lives_in: Department, oves: List[Department], role_id: str, **kwargs):
    person = StaffFactory(
        login=login,
        user=UserFactory(username=login),
        department=lives_in,
        work_email=login + '@yandex-team.ru',
        **kwargs
    )
    for department in oves:
        DepartmentStaffFactory(staff=person, department=department, role_id=role_id)
    return person


def create_hr_partner(login: str, lives_in: Department, oves: List[Department], **kwargs) -> 'Staff':
    person_attrs = {
        'first_name': 'Лилия',
        'last_name': 'Партнёрова',
        'affiliation': AFFILIATION.YANDEX,
    }
    person_attrs.update(**kwargs)
    return _create_department_roled_person(
        login=login,
        lives_in=lives_in,
        oves=oves,
        role_id=DepartmentRoles.HR_PARTNER.value,
        **person_attrs
    )


def create_hr_analyst(login: str, lives_in: Department, oves: List[Department], **kwargs) -> 'Staff':
    person_attrs = {
        'first_name': 'Татьяна',
        'last_name': 'Баранова',
        'affiliation': AFFILIATION.YANDEX,
    }
    person_attrs.update(**kwargs)
    return _create_department_roled_person(
        login=login,
        lives_in=lives_in,
        oves=oves,
        role_id=DepartmentRoles.HR_ANALYST.value,
        **person_attrs
    )


def create_company(kinds, map_models, settings):
    """
    Creates minimal initial structure similar to real yandex departments tree:
    Codes of departments:
           yandex        outstaff         ext          yamoney      virtual               vs
          /     |         |    |           |           /     |         |                 /   \
        dep1    dep2    out1   out2       ext1       yam1   yam2      virtual_robot   vs_1   vs_2
        |   |             |                |          |       |                       /       \
     dep11  dep12      out11             ext11      yam11  removed3                vs_11      vs_21
        |      |                           |                                      /    |
     dep111   removed1                  removed2                             vs_111    vs_112

    And Groups corresponding to this structure

    Every department have
     - chief inside with login <dep_code>-chief
     - regular person with login <dep_code>-person
     + few other persons: dep1-hr-partner, dep2-hr-partner, dep1-hr-analyst, dep2-hr-analyst

    """
    result = AttrDict()

    departments = {}
    valuestreams = {}
    geographies = {}
    groups = {}
    persons = {}
    vacancies = {}
    headcounts = {}
    _kinds = {k.slug: k for k in kinds}
    organizations = {
        'yandex': OrganizationFactory(name='Яндекс', name_en='Yandex', country_code='RU'),
        'yandex_tech': OrganizationFactory(name='Яндекс.Технологии', name_en='Yandex.Technology', country_code='RU'),
        'taxi': OrganizationFactory(name='Яндекс.Такси', name_en='Yandex.Taxi', country_code='RU'),
        'robots': OrganizationFactory(name='Вне оргаизаций', name_en='No organization'),
    }

    # Сейчас OEBS управляет организациями в стаффе
    # Eсли нет OEBS организации с staff_usage=Y, то нашей проставляется intranet_status=0
    for organization in organizations.values():
        OebsOrganizationFactory(dis_organization_id=organization.id, country_code='RU')

    settings.ROBOTS_ORGANIZATION_ID = organizations['robots'].id
    Organization._OUTSTAFF_ORG = organizations['robots']
    settings.VIRTUAL_OFFICE_ID = map_models.offices['virtual'].id
    settings.HOMIE_OFFICE_ID = map_models.offices['homie'].id
    Office._HOMIE_OFFICE = map_models.offices['homie']

    settings.DIS_ROOT_KIND_ID = kinds[0].id
    settings.DEBUG = True

    def new_department(
        name: str,
        name_en: str,
        parent: Optional[Department],
        code: str,
        kind: int,
        section_caption: Optional[DepartmentInterfaceSection] = None,
        section_group: Optional[DepartmentInterfaceSection] = None,
    ):
        url = '%s%s' % ((parent.url + '_') if parent else '', code)
        dep = DepartmentFactory(
            name=name,
            name_en=name_en,
            parent=parent,
            code=code,
            url=url,
            kind_id=kind,
            group=None,
            section_caption=section_caption,
            section_group=section_group,
        )
        group = GroupFactory(url=url, type=GROUP_TYPE_CHOICES.DEPARTMENT, department=dep)

        groups[dep.url] = group
        departments[dep.url] = Department.objects.get(id=dep.id)
        return dep

    def new_valuestream(name, name_en, parent: Optional[Department], code):
        url = '%s%s' % ((parent.url + '_') if parent else '', code)
        dep = ValueStreamFactory(
            name=name,
            name_en=name_en,
            parent=parent,
            code=code,
            url=url,
            kind_id=kinds[0].id,
        )

        valuestreams[dep.url] = Department.valuestreams.get(id=dep.id)
        return dep

    def new_geo(name, name_en, parent: Optional[Department], code: str) -> Department:
        prefix = (parent.url + '_') if parent else 'geo_'
        url = f'{prefix}{code}'
        dep = GeographyDepartmentFactory(
            name=name,
            name_en=name_en,
            parent=parent,
            code=code,
            url=url,
        )

        geographies[dep.url] = Department.geography.get(id=dep.id)
        return dep

    result.section_caption = DepartmentInterfaceSectionFactory(name='БЮ', name_en='BU')
    result.section_group = DepartmentInterfaceSectionFactory()

    # yandex tree
    result.yandex = new_department(name='Яндекс', name_en='Yandex', parent=None, code='yandex', kind=kinds[0].id)
    result.dep1 = new_department(
        name='Главный бизнес-юнит',
        name_en='dep1',
        parent=result.yandex,
        code='dep1',
        kind=kinds[1].id,
        section_caption=result.section_caption,
    )
    result.dep2 = new_department(
        name='Бизнес юниты',
        name_en='dep2',
        parent=result.yandex,
        code='dep2',
        kind=kinds[1].id,
        section_group=result.section_group,
    )
    result.dep11 = new_department(name='dep11', name_en='dep11', parent=result.dep1, code='dep11', kind=kinds[2].id)
    result.dep12 = new_department(name='dep12', name_en='dep12', parent=result.dep1, code='dep12', kind=kinds[2].id)
    result.dep111 = new_department(
        name='dep111',
        name_en='dep111',
        parent=result.dep11,
        code='dep111',
        kind=kinds[3].id,
    )
    settings.YANDEX_DEPARTMENT_ID = result.yandex.id

    # outstaff tree
    result.outstaff = new_department(
        name='Поддержка бизнеса',
        name_en='busyness support',
        parent=None,
        code='outstaff',
        kind=kinds[0].id,
    )
    result.out1 = new_department(
        name='Поддержка бизнеса 1',
        name_en='outstaff subdep 1',
        parent=result.outstaff,
        code='out1',
        kind=kinds[1].id,
    )
    result.out2 = new_department(
        name='Поддержка бизнеса 2',
        name_en='outstaff subdep 2',
        parent=result.outstaff,
        code='out2',
        kind=kinds[1].id,
    )
    result.out11 = new_department(name='out11', name_en='out11', parent=result.out1, code='out11', kind=kinds[2].id)
    settings.OUTSTAFF_DEPARTMENT_ID = result.outstaff.id

    # external tree
    result.ext = new_department(
        name='Внешнее подразделение',
        name_en='external department',
        parent=None,
        code='ext',
        kind=kinds[0].id,
    )
    result.ext1 = new_department(name='ext1', name_en='ext1', parent=result.ext, code='ext1', kind=kinds[1].id)
    result.ext11 = new_department(name='ext11', name_en='ext11', parent=result.ext1, code='ext11', kind=kinds[2].id)
    settings.EXT_DEPARTMENT_ID = result.ext.id

    # yamoney tree
    result.yamoney = new_department(
        name='Яндекс.Деньги',
        name_en='Yandex.Money',
        parent=None,
        code='yandex_money',
        kind=kinds[0].id,
    )
    result.yam1 = new_department(name='yam1', name_en='yam1', parent=result.yamoney, code='yam1', kind=kinds[1].id)
    result.yam2 = new_department(name='yam2', name_en='yam2', parent=result.yamoney, code='yam2', kind=kinds[1].id)
    result.yam11 = new_department(name='yam1', name_en='yam1', parent=result.yam1, code='yam11', kind=kinds[4].id)
    settings.YAMONEY_DEPARTMENT_ID = result.yamoney.id

    # virtual tree
    result.virtual = new_department(name='Virtual', name_en='Virtual', parent=None, code='virtual', kind=kinds[0].id)
    result.virtual_robot = new_department(
        name='virtual_robot',
        name_en='virtual robot',
        parent=result.virtual,
        code='robot',
        kind=kinds[4].id,
    )
    settings.ROBOT_DEPARTMENT_ID = result.virtual.id
    settings.ZOMBIE_DEPARTMENT_ID = result.virtual_robot.id

    # valuestreams tree
    result.vs_root = new_valuestream(
        name='Вэлью Стримз',
        name_en='Value Streams',
        parent=None,
        code='vs',
    )

    result.vs_1 = new_valuestream(
        name='Вэлью Стримз 1',
        name_en='VS 1',
        parent=result.vs_root,
        code='vs1',
    )

    result.vs_2 = new_valuestream(
        name='Вэлью Стримз 2',
        name_en='VS 2',
        parent=result.vs_root,
        code='vs2',
    )

    result.vs_11 = new_valuestream(
        name='Вэлью Стримз 11',
        name_en='VS 11',
        parent=result.vs_1,
        code='vs11',
    )

    result.vs_21 = new_valuestream(
        name='Вэлью Стримз 21',
        name_en='VS 21',
        parent=result.vs_2,
        code='vs21',
    )

    result.vs_111 = new_valuestream(
        name='Вэлью Стримз 111',
        name_en='VS 111',
        parent=result.vs_11,
        code='vs111',
    )

    result.vs_112 = new_valuestream(
        name='Вэлью Стримз 112',
        name_en='VS 112',
        parent=result.vs_11,
        code='vs112',
    )

    # geo
    result.geo_russia = new_geo('Россия', 'Russia', parent=None, code='rusc')
    result.geo_wor = new_geo('Весь Мир', 'World', parent=None, code='wor')

    # couple of removed departments (intranet_status=0)
    result.removed1 = new_department(
        name='removed1',
        name_en='removed1',
        parent=result.dep12,
        code='removed1',
        kind=kinds[3].id,
    )
    result.removed2 = new_department(
        name='removed2',
        name_en='removed2',
        parent=result.ext11,
        code='removed2',
        kind=kinds[4].id,
    )
    result.removed3 = new_department(
        name='removed3',
        name_en='removed3',
        parent=result.yam2,
        code='removed3',
        kind=kinds[2].id,
    )
    result.removed2.intranet_status = 0
    result.removed1.intranet_status = 0
    result.removed3.intranet_status = 0
    result.removed1.save()
    result.removed2.save()
    result.removed3.save()

    for url, department in departments.items():
        departments[url] = Department.objects.get(id=department.id)

    for url, group in groups.items():
        groups[url] = Group.objects.get(id=group.id)

    # creating persons
    ext_tree_id = Department.objects.values_list('tree_id', flat=True).get(id=settings.EXT_DEPARTMENT_ID)
    yamoney_tree_id = Department.objects.values_list('tree_id', flat=True).get(id=settings.YAMONEY_DEPARTMENT_ID)
    affiliations = {
        ext_tree_id: AFFILIATION.EXTERNAL,
        yamoney_tree_id: AFFILIATION.YAMONEY,
    }
    with factory.django.mute_signals(signals.pre_save, signals.post_save):
        persons['general'] = StaffFactory(
            login='general',
            user=UserFactory(username='general'),
            first_name='Аркадий',
            department=result.yandex,
            affiliation=AFFILIATION.YANDEX,
            work_email='general@yandex-team.ru',
            office=map_models.offices['KR'],
            organization=organizations['yandex'],
            is_robot=False,
        )
        EmployeeFactory(dis_staff_id=persons['general'].id, job_name='Генерал')
        DepartmentStaffFactory(
            staff=persons['general'],
            department=result.yandex,
            role_id=DepartmentRoles.GENERAL_DIRECTOR.value,
        )

        for dep in Department.objects.exclude(code='').exclude(code__startswith='removed'):
            chief_login = dep.code + '-chief'
            regular_login = dep.code + '-person'
            affiliation = affiliations.get(dep.tree_id, AFFILIATION.YANDEX)
            persons[chief_login] = StaffFactory(
                login=chief_login,
                user=UserFactory(username=chief_login),
                first_name='Аркадий',
                department=dep,
                affiliation=affiliation,
                work_email=chief_login + '@yandex-team.ru',
                office=map_models.offices['KR'],
                organization=organizations['yandex'],
                is_robot=dep.url.startswith(result.virtual.url),
            )
            EmployeeFactory(dis_staff_id=persons[chief_login].id, job_name='Работник')

            DepartmentStaffFactory(staff=persons[chief_login], department=dep, role_id=DepartmentRoles.CHIEF.value)
            persons[regular_login] = StaffFactory(
                login=regular_login,
                user=UserFactory(username=regular_login),
                first_name='Владимир',
                department=dep,
                affiliation=affiliation,
                work_email=regular_login + '@yandex-team.ru',
                office=map_models.offices['KR'],
                organization=organizations['yandex_tech'],
                is_robot=dep.url.startswith(result.virtual.url),
            )
            EmployeeFactory(dis_staff_id=persons[regular_login].id, job_name='Трудяга')

            setattr(result, chief_login, persons[chief_login])
            setattr(result, regular_login, persons[regular_login])

            vacancy = dep.code + '-vac'
            vacancies[vacancy] = VacancyFactory(name=vacancy, department=dep, is_published=True)
            setattr(result, vacancy, vacancies[vacancy])

        for vs_dep in Department.valuestreams.exclude(code='').exclude(code__startswith='removed'):
            chief_login = vs_dep.code + '-chief'
            affiliation = affiliations.get(vs_dep.tree_id, AFFILIATION.YANDEX)
            persons[chief_login] = StaffFactory(
                login=chief_login,
                user=UserFactory(username=chief_login),
                first_name='VS Аркадий',
                department=vs_dep,
                affiliation=affiliation,
                work_email=chief_login + '@yandex-team.ru',
                office=map_models.offices['KR'],
                organization=organizations['yandex'],
                is_robot=dep.url.startswith(result.virtual.url),
            )
            EmployeeFactory(dis_staff_id=persons[chief_login].id, job_name='Работник')

            DepartmentStaffFactory(
                staff=persons[chief_login],
                department=vs_dep,
                role_id=ValuestreamRoles.HEAD.value,
            )

            setattr(result, chief_login, persons[chief_login])

        hr_partners: Dict[str, List[Department]] = defaultdict(list)
        hr_analysts: Dict[str, List[Department]] = defaultdict(list)

        hr_login = 'dep1-hr-partner'
        persons[hr_login] = create_hr_partner(
            login=hr_login,
            lives_in=result.dep1,
            oves=[result.dep1],
        )
        setattr(result, hr_login, persons[hr_login])
        hr_partners[hr_login].append(result.dep1)

        hr_login = 'dep2-hr-partner'
        persons[hr_login] = create_hr_partner(
            login=hr_login,
            lives_in=result.dep11,
            oves=[result.dep2, result.dep12],
        )
        setattr(result, hr_login, persons[hr_login])
        hr_partners[hr_login].extend([result.dep2, result.dep12])

        hr_analyst_login = 'dep1-hr-analyst'
        persons[hr_analyst_login] = create_hr_analyst(
            login=hr_analyst_login,
            lives_in=result.dep1,
            oves=[result.dep1],
        )
        setattr(result, hr_analyst_login, persons[hr_login])
        hr_analysts[hr_analyst_login].append(result.dep1)

        hr_analyst_login = 'dep2-hr-analyst'
        persons[hr_analyst_login] = create_hr_analyst(
            login=hr_analyst_login,
            lives_in=result.dep12,
            oves=[result.dep2],
        )
        setattr(result, hr_analyst_login, persons[hr_login])
        hr_analysts[hr_analyst_login].append(result.dep2)

    positions = {
        123: JobFactory(code=123, name='Уборщик', name_en='Cleaner'),
        321: JobFactory(code=321, name='Уборщица', name_en='Cleaner'),
        456: JobFactory(code=456, name='Флорист', name_en='Florer'),
        789: JobFactory(code=789, name='Зам по ничегонеделанию', name_en='Ex president'),
        314: JobFactory(code=314, name='Лежальщица', name_en='Lezhalshchitsa'),
    }

    result.update(
        {
            'departments': departments,
            'valuestreams': valuestreams,
            'groups': groups,
            'persons': persons,
            'hr_partners': dict(hr_partners),
            'hr_analysts': dict(hr_analysts),
            'kinds': _kinds,
            'offices': map_models.offices,
            'organizations': organizations,
            'positions': positions,
            'vacancies': vacancies,
            'headcounts': headcounts,
            'settings': settings,
        }
    )
    fill_departmentattrs_with_roles(result)

    result.reload()
    return result

from typing import Set, Union
import copy
import csv
import os

from django.contrib.auth.models import Permission

from staff.departments.models import DepartmentRole, DepartmentStaff
from staff.lib.testing import StaffFactory
from staff.person.models import Staff, AFFILIATION

from staff.person_profile.permissions.base import Block, LoadableBlock, Link, Pencil
from staff.person_profile.permissions.registry import BlockRegistry
from staff.person_profile.permissions.properties import Properties

csv.register_dialect('staff', delimiter=str(';'))

AnyBlock = Union[Block, LoadableBlock, Link, Pencil]

DIR = os.path.dirname(os.path.abspath(__file__))

FIELDNAMES = (
    'name',
    'type',
    'employee',
    'robot',
    'agreed_dismissed',
    'dismissed',
    'memorial',
    'chief',
    'owner',
)

BLOCK_CLASSES = [
    ('b', Block),
    ('lb', LoadableBlock),
    ('l', Link),
    ('p', Pencil),
]


def test_block_permissions():
    f = open(DIR + '/permissions.csv')
    try:
        reader = csv.DictReader(f, dialect='staff', fieldnames=FIELDNAMES)
        correct = {row['name']: row for row in reader}
    finally:
        f.close()

    # собираем блоки из BlockRegistry
    blocks = {}
    for b_type, b_cls in BLOCK_CLASSES:
        blocks.update(_get_blocks(b_type, _get_by_block_class(b_cls)))

    errors = []
    _correct = copy.copy(correct)
    for name in correct:
        # нужный блок отсутствует
        if name not in blocks:
            errors.append('absent %s' % name)
        diff = _cmp_blocks(_correct.pop(name), blocks.pop(name))
        # блоки отличаются
        if diff:
            errors.append('%s:%s' % (name, diff))
    # присутствуют лишние блоки
    for name in blocks:
        errors.append('needless %s' % name)

    assert not errors


def _get_by_block_class(cls):
    return {
        name: inst for name, inst in BlockRegistry.__dict__.items()
        if isinstance(inst, cls)
    }


def _get_blocks(block_type, blocks):
    _blocks = {}
    for key in blocks.keys():
        block = blocks[key]
        _block = {'name': key, 'type': block_type}
        _block.update(_get_show_on(block.show_on))
        _block.update(_get_checkers(block.checkers))
        _blocks[key] = _block
    return _blocks


def _get_show_on(show_on):
    return {
        'employee': '+' if 'employee' in show_on else '',
        'robot': '+' if 'robot' in show_on else '',
        'agreed_dismissed': '+' if 'agreed_dismissed' in show_on else '',
        'dismissed': '+' if 'dismissed' in show_on else '',
        'memorial': '+' if 'memorial' in show_on else '',
    }


def _get_checkers(checkers):
    return {
        'chief': '+' if _checker_in_checkers(checkers, '_check_chief') else '',
        'owner': '+' if _checker_in_checkers(checkers, 'check_owner') else '',
    }


def _checker_in_checkers(checkers, name):
    for checker in checkers:
        if checker.__name__ == name:
            return True
    return False


def _cmp_blocks(block_a, block_b):
    # считаем, что количество и название пар в блоках равное
    diff = []
    for key in block_a:
        if block_a[key].strip() != block_b[key].strip():
            diff.append(key)
    return diff


def _get_block_names(login_to_watch: str, watching_person: Staff) -> Set[AnyBlock]:
    props = Properties([login_to_watch], watching_person, False)
    blocks = BlockRegistry(props)
    block_names = set()
    for method in ('get_blocks', 'get_loadable_blocks', 'get_links', 'get_pencils'):
        block_names |= set(getattr(blocks, method)(login_to_watch).keys())
    return block_names


EXTERNAL_OTHER_BLOCKS = {
    block_name
    for block_name, block in BlockRegistry.__dict__.items()
    if isinstance(block, (Block, Link, LoadableBlock, Pencil)) and block.external_other
}

EXTERNAL_OWNER_BLOCKS = {
    block_name
    for block_name, block in BlockRegistry.__dict__.items()
    if isinstance(block, (Block, Link, LoadableBlock, Pencil)) and block.external_owner
}

WITHOUT_EXTERNAL_OTHER_BLOCKS = {
    block_name
    for block_name, block in BlockRegistry.__dict__.items()
    if isinstance(block, (Block, Link, LoadableBlock, Pencil)) and not block.external_other
}

ALL_BLOCKS = EXTERNAL_OTHER_BLOCKS | WITHOUT_EXTERNAL_OTHER_BLOCKS

# Блоки, показываемые только на уволенных, мемориальных и роботах
WEIRD_BLOCKS = {'quit_at', 'responsible_for_robot', 'quit_at_short', 'memorial_info'}


def test_meta_external_owner(company):
    tester = StaffFactory(
        department=company.ext,
        affiliation=AFFILIATION.EXTERNAL,
    )

    block_names = _get_block_names(tester.login, watching_person=tester)
    assert all(it in block_names for it in EXTERNAL_OWNER_BLOCKS), block_names


def test_external_other_permissions(company):
    """
    Тест проверяет доступность блоков с external_other=True внешнему сотруднику
    на выданную на подразделение роль с правом can_view_departments
    И недоступность всех блоков на сотруднике из другого подразделения
    """
    external_person = company.persons['ext-person']
    permission = Permission.objects.get(codename='can_view_departments')
    role = DepartmentRole(id='TEST_ROLE_NAME')
    role.save()
    role.permissions.add(permission)
    DepartmentStaff.objects.create(
        department=company.dep1,
        staff=external_person,
        role=role,
    )
    internal_person = company.persons['dep1-person']
    internal_person_from_other_department = company.persons['dep2-person']

    block_names = _get_block_names(internal_person.login, watching_person=external_person)
    assert all(it in block_names for it in EXTERNAL_OTHER_BLOCKS), block_names
    assert all(it not in block_names for it in WITHOUT_EXTERNAL_OTHER_BLOCKS), block_names

    block_names = _get_block_names(internal_person_from_other_department.login, watching_person=external_person)
    assert all(it not in block_names for it in ALL_BLOCKS - WEIRD_BLOCKS), block_names


def test_can_view_all_blocks_permission(company):
    """
    Тест проверяет доступность ВСЕХ блоков внешнему сотруднику с правом can_view_all_blocks
    на выданную на подразделение роль с правом can_view_departments
    И недоступность всех блоков на сотруднике из другого подразделения
    """
    external_person = company.persons['ext-person']
    permission = Permission.objects.get(codename='can_view_departments')
    role = DepartmentRole(id='TEST_ROLE_NAME')
    role.save()
    role.permissions.add(permission)
    DepartmentStaff.objects.create(
        department=company.dep1,
        staff=external_person,
        role=role,
    )

    external_person.user.user_permissions.add(Permission.objects.get(codename='can_view_all_blocks'))

    internal_person = company.persons['dep1-person']
    internal_person_from_other_department = company.persons['dep2-person']

    block_names = _get_block_names(internal_person.login, watching_person=external_person)
    assert block_names == {
        'achievements',
        'activity',
        'bicycles',
        'birthday',
        'calendar',
        'calendar_gaps',
        'calendar_holidays',
        'candidate_info',
        'cars',
        'chief',
        'contacts',
        'curators',
        'departments',
        'duties',
        'education',
        'external_login',
        'feedback',
        'fio',
        'goals',
        'gpg_keys',
        'hr_partners',
        'is_calendar_vertical',
        'location_office',
        'location_room',
        'location_table',
        'official_base',
        'official_organization',
        'personal',
        'phones',
        'photos',
        'services',
        'slack',
        'slack_status',
        'ssh_keys',
        'table_books',
        'value_streams',
        'value_stream_chief',
        'work_mode',
        'work_phone',
        'yamb',
    }

    block_names = _get_block_names(external_person.login, watching_person=external_person)
    assert block_names == {
        'achievements',
        'bicycles',
        'birthday',
        'calendar',
        'calendar_holidays',
        'cars',
        'chief',
        'contacts',
        'curators',
        'delete_all_photos',
        'departments',
        'digital_sign',
        'digital_sign_status',
        'digital_sign_certification_status',
        'documents',
        'duties',
        'edit_bicycles',
        'edit_cars',
        'edit_contacts',
        'edit_digital_sign',
        'edit_documents',
        'edit_duties',
        'edit_gpg_keys',
        'edit_head',
        'edit_location',
        'edit_phones',
        'edit_settings',
        'edit_ssh_keys',
        'edoc',
        'education',
        'external_login',
        'fio',
        'goals',
        'gpg_keys',
        'hardware',
        'hr_partners',
        'is_calendar_vertical',
        'location_office',
        'location_room',
        'location_table',
        'official_organization',
        'paid_day_off',
        'password',
        'personal',
        'phones',
        'photos',
        'services',
        'slack',
        'slack_status',
        'software',
        'ssh_keys',
        'survey',
        'table_books',
        'umbrellas',
        'upload_photo',
        'vacation',
        'value_streams',
        'value_stream_chief',
        'work_mode',
        'work_phone',
        'yamb',
    }

    block_names = _get_block_names(internal_person_from_other_department.login, watching_person=external_person)
    assert block_names == set()

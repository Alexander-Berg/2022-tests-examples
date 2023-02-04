from datetime import datetime, date

from bson import ObjectId

from staff.lib.mongodb import mongo
from staff.lib.testing import DepartmentFactory
from staff.person.models import Staff

from staff.departments.models import DEPARTMENT_CATEGORY, Department, ProposalMetadata
from staff.departments.edit.constants import nearest_hr_dates
from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.departments.controllers.proposal_action import ordered_actions
from staff.departments.controllers.tickets.helpers import department_attrs


def _create_department(name, name_en, parent, kind, clubs=None, wiki_page=None):
    code = name_en.lower().replace(' ', '')
    now = datetime.now()
    return DepartmentFactory(
        intranet_status=1,
        name=name.capitalize(),
        short_name=name.capitalize(),
        name_en=name_en.capitalize(),
        short_name_en=name_en.capitalize(),
        code=code,
        url=code if parent is None else parent.url + '_' + code,
        bg_color='#333333' if parent is None else parent.bg_color,
        fg_color='#FFFFFF' if parent is None else parent.fg_color,
        created_at=now,
        modified_at=now,
        native_lang='ru',
        wiki_page=wiki_page or '',
        clubs=clubs or '',
        from_staff_id=0,
        category=DEPARTMENT_CATEGORY.NONTECHNICAL,
    )


def dep(url):
    return Department.objects.get(url=url)


def person(login):
    return Staff.objects.get(login=login)


def proposal(proposal_data, actions):
    proposal_data = {
        'link_to_ticket': '',
        'departments': {'actions': actions},
        'persons': {'actions': []},
        'description': '{proposal_description}'.format(**proposal_data),
        'apply_at': proposal_data.get('apply_at', date.today().strftime('%Y-%m-%d')),
        'apply_at_hr': proposal_data.get('apply_at', nearest_hr_dates()[1][0]),
        'tickets': {'department_linked_ticket': proposal_data.get('department_linked_ticket', '')}
    }
    return proposal_data


def apply_proposal_dict(obj):
    obj['_id'] = ObjectId('5b0289c618ddb2051409f49d')
    ProposalMetadata.objects.create(
        proposal_id=obj['_id'],
        applied_at='2017-12-31T01:23:45',
        applied_by=Staff.objects.filter(is_dismissed=False).first(),
        pushed_to_oebs=None,
    )
    obj['actions'] = list(ordered_actions(obj['actions']))
    obj['description'] = 'Заявка применена!'
    mongo.db[MONGO_COLLECTION_NAME].insert_one(obj)
    return obj


def reset_department_attrs():
    department_attrs.cache_one = {}
    department_attrs.cache_all = {}

    department_attrs.cached_attrs = None
    department_attrs.cached_default = None

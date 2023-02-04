import datetime
import json
import os


def get_meta():
    return {'last_message': {'id': 123456, 'creation_time': str(datetime.datetime.now())},
            'entities': ['django_intranet_stuff.staff',
                         'django_intranet_stuff.departmentkind',
                         'django_intranet_stuff.department',
                         'django_intranet_stuff.groupmembership',
                         'django_intranet_stuff.groupresponsible',
                         'django_intranet_stuff.group',
                         'django_intranet_stuff.departmentstaff',
                         'departments.departmentchain',
                         'django_intranet_stuff.staffcar',
                         'django_intranet_stuff.staffbicycle',
                         'keys.sshkey',
                         'keys.gpgkey',
                         'django_intranet_stuff.staffphone',
                         'emails.email',
                         'django_intranet_stuff.country',
                         'django_intranet_stuff.city',
                         'django_intranet_stuff.office',
                         'django_intranet_stuff.floor',
                         'django_intranet_stuff.room',
                         'django_intranet_stuff.printer',
                         'django_intranet_stuff.organization',
                         'django_intranet_stuff.table',
                         'person.staffextrafields']}


def get_entity_data(entity):
    with open(os.path.join(os.path.dirname(__file__), 'stub', entity + '.json')) as inp:
        yield json.loads(inp.read())

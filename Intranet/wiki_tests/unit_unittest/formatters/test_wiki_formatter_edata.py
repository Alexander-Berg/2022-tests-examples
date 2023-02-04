
from ids.exceptions import BackendError
from pretend import stub

FAKE_REQUEST = stub(user_auth=None)

STAFF_PERSON_DATA = {
    'somename': {
        'personal': {'gender': 'male'},
        'login': 'somename',
        'memorial': {'death_date': '2015-15-05', 'id': 2},
        'official': {'is_dismissed': True},
        'name': {
            'middle': '',
            'has_namesake': False,
            'last': {'ru': 'Петров', 'en': 'Petroff'},
            'first': {'ru': 'Иван', 'en': 'Ivan'},
            'hidden_middle': True,
        },
    },
    'elisei': {
        'personal': {'gender': 'male'},
        'login': 'elisei',
        'memorial': None,
        'official': {'is_dismissed': False},
        'name': {
            'middle': '',
            'has_namesake': False,
            'last': {'ru': 'Микерин', 'en': 'Mikerin'},
            'first': {'ru': 'Алексей', 'en': 'Alexey'},
            'hidden_middle': True,
        },
    },
    'corso': {
        'personal': {'gender': 'male'},
        'login': 'corso',
        'memorial': None,
        'official': {'is_dismissed': True},
        'name': {
            'middle': '',
            'has_namesake': False,
            'last': {'ru': 'Филиппов', 'en': 'Filippov'},
            'first': {'ru': 'Максим', 'en': 'Maksim'},
            'hidden_middle': True,
        },
    },
}

STAFF_INFLECTION_DATA = {
    'Иван Петров': {
        'именительный': {
            'first_name': 'Иван',
            'last_name': 'Петров',
        },
        'родительный': {
            'first_name': 'Ивана',
            'last_name': 'Петрова',
        },
        'дательный': {
            'first_name': 'Ивану',
            'last_name': 'Петрову',
        },
        'винительный': {
            'first_name': 'Ивана',
            'last_name': 'Петрова',
        },
        'творительный': {
            'first_name': 'Иваном',
            'last_name': 'Петровым',
        },
        'предложный': {
            'first_name': 'Иване',
            'last_name': 'Петрове',
        },
    },
    'Алексей Микерин': {
        'именительный': {
            'first_name': 'Алексей',
            'last_name': 'Микерин',
        },
        'родительный': {
            'first_name': 'Алексея',
            'last_name': 'Микерина',
        },
        'дательный': {
            'first_name': 'Алексею',
            'last_name': 'Микерину',
        },
        'винительный': {
            'first_name': 'Алексея',
            'last_name': 'Микерина',
        },
        'творительный': {
            'first_name': 'Алексеем',
            'last_name': 'Микериным',
        },
        'предложный': {
            'first_name': 'Алексее',
            'last_name': 'Микерине',
        },
    },
    'Максим Филиппов': {
        'именительный': {
            'first_name': 'Максим',
            'last_name': 'Филиппов',
        },
        'родительный': {
            'first_name': 'Максима',
            'last_name': 'Филиппова',
        },
        'дательный': {
            'first_name': 'Максиму',
            'last_name': 'Филиппову',
        },
        'винительный': {
            'first_name': 'Максима',
            'last_name': 'Филиппова',
        },
        'творительный': {
            'first_name': 'Максимом',
            'last_name': 'Филипповым',
        },
        'предложный': {
            'first_name': 'Максиме',
            'last_name': 'Филиппове',
        },
    },
}


class StaffMockRepository(object):
    def getiter(self, lookup, **kwargs):
        result = []
        for login in lookup.get('login', '').split(','):
            if login in STAFF_PERSON_DATA:
                result.append(STAFF_PERSON_DATA[login])
        return iter(result)


class InflectorMockRepository(object):
    def get_person_inflections(self, person):
        first_name = person['first_name']
        last_name = person['last_name']
        return STAFF_INFLECTION_DATA.get('%s %s' % (first_name, last_name), {})


class StartrekQueuesMockRepository(object):
    def get(self, project_key):
        if project_key == 'STARTREK':
            return {'id': '4ff3009ce4b0d1fcead11017', 'key': 'STARTREK', 'name': 'Стартрек'}
        raise BackendError('Unknown mock queue', response=stub(status_code=404))

    def get_all(self, **parameters):
        return [{'id': '4ff3009ce4b0d1fcead11017', 'key': 'STARTREK', 'name': 'Стартрек'}]

class FakeCMSResponse:

    status_code = 200
    text = 'text'


class FakeCMSApi:

    def __init__(*args, **kwargs):
        pass

    def participant_delete(self, *args, **kwargs):
        return FakeCMSResponse()


class FakeParticipant:

    def __init__(self, data):
        self.data = data


class FakeHydrator:

    def __init__(self, *args, **kwargs):
        pass

    def add_to_fetch(self, participants):
        pass

    def hydrate(self, participant):
        return FakeParticipant(participant)


def get_fake_event(event_id, login):
    return {
        'id': event_id,
        'externalId': 'abcd' + str(event_id),
        'attendees': [{
            'login': login,
        }],
    }


def get_event_info_mock(event_id=None, *args, **kwargs):
    return {
        'id': 1,
        'master_id': 1,
        'description': 'some text',
        'externalId': 'abcd' + str(event_id),
        'name': 'Event',
        'startTs': '2020-01-24T12:00:00+03:00',
        'endTs': '2020-01-24T15:00:00+03:00',
        'attendees': [{
            'login': 'login',
        }],
        'resources': [{
            'resourceType': 'room',
            'email': 'slug1@yandex-team.ru',
        }, {
            'resourceType': 'room',
            'email': 'slug2@yandex-team.ru',
        }],
    }


def get_next_event_mock(event_id=None, master_id=1, *args, **kwargs):
    mock = get_event_info_mock(event_id, *args, **kwargs)
    mock['master_id'] = master_id
    return mock


def get_passcode_by_id_mock(conf_id):
    if conf_id == '7706330695':
        return '154827'
    else:
        return None

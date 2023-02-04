import pytest


@pytest.fixture
def get_track_req():

    def get_track_req_(service: int = 1234, env: int = 5678):
        data = {
            'infra': {
                'serviceId': service,
                'environmentId': env,

                'title': 'sometitle',
                'type': None,
                'severity': None,
            },

            'tracker': {
                'key': 'ISSUE-1',
                'type': 'Задача',
                'priority': 'Нормальный',
                'status': 'В работе',
            },

            'ift': {
                "statusStart": ["*"],
            }

        }

        return data

    return get_track_req_

from unittest.mock import MagicMock

from intranet.vconf.src.call.participant_ctl import PersonParticipant, RoomParticipant


def participants_hydrator_mock(lang):
    def get_pt(pt):
        if pt['type'] == 'person':
            return PersonParticipant(pt)
        else:
            return RoomParticipant(pt)

    return MagicMock(**{
        'hydrate.side_effect': get_pt,
        'add_to_fetch.return_value': None
    })

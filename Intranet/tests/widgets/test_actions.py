# coding: utf-8

from __future__ import unicode_literals

from cab.widgets import actions


def test_preprofiles_get(monkeypatch):
    expected = [
        {
            'id': i,
            'first_name': 'first_{}'.format(i),
            'last_name': 'last_1'.format(i),
        }
        for i in range(2)
    ]

    def api_request(*args, **kwargs):
        return {'preprofiles': expected}

    monkeypatch.setattr(actions.staff, 'get_preprofiles_to_confirm', api_request)
    received = actions.get_preprofile_data(None)
    assert {it['id'] for it in expected} == {it['id'] for it in received}


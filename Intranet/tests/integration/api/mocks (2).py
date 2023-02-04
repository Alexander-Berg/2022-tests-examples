import requests


def _mock_group_members(groups):
    logins = set()
    for group in groups:
        for membership in group.memberships.all():
            logins.add(membership.login)
    return logins


def _mock_table_flow(url, **kwargs):
    r = requests.Response()
    r.status_code = 200
    r.json = lambda: {'login': 'tester'}
    return r

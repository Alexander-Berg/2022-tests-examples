from django.utils.encoding import force_text

from infra.cauth.server.common.models import User


def test_passwd_list(users, client):
    response = client.get('/passwd/')

    rulelist = force_text(response.content).splitlines()
    rulelist = [rule for rule in rulelist if '########' not in rule]

    assert rulelist == [
        '{user.login}:*:{user.uid}:{user.gid}:{user.first_name} {user.last_name}:/home/{user.login}:{user.shell}'.format(user=user)
        for user in User.query.order_by(User.login)
    ]

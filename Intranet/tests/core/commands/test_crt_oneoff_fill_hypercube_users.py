# coding: utf-8
from __future__ import absolute_import, unicode_literals

from django.core.management import call_command

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate


def test_crt_oneoff_fill_hypercube_user(users, certificate_types):
    helpdesk_user, normal_user, tag_user = users['helpdesk_user'], users['normal_user'], users['tag_user']
    hypercube = certificate_types[CERT_TYPE.HYPERCUBE]

    pairs = [
        (helpdesk_user, helpdesk_user),
        (helpdesk_user, normal_user),
        (helpdesk_user, tag_user),
        (normal_user, tag_user),
    ]
    for requester, user in pairs:
        create_certificate(
            requester, requester=requester, type=hypercube, common_name='{}@pda-ld.yandex.ru'.format(user.username)
        )

    for cert, (requester, user) in zip(Certificate.objects.order_by('pk'), pairs):
        assert cert.user == cert.requester == requester

    call_command('crt_oneoff_fill_hypercube_user')
    for cert, (requester, user) in zip(Certificate.objects.order_by('pk'), pairs):
        assert cert.user == user
        assert cert.requester == requester

# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse
from django.conf import settings


@pytest.mark.django_db
def test_index_context(client):
    settings.YAUTH_TEST_USER = False
    context = client.get(reverse('main')).context
    assert context['feedback_url']

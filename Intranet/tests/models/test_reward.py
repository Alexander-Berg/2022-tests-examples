# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.db import models as django_models


def test_vulnerability_delete(reward, vulnerability):
    with pytest.raises(django_models.ProtectedError):
        vulnerability.delete()

# coding: utf-8

from tests.fixtures_for_bi import *
from tests.fixtures_for_compensations import *
from tests.fixtures_for_staff_models import *
from tests.fixtures_for_core_models import *
from tests.fixtures_for_oebs_models import *
from tests.fixtures_for_cases import *
from tests.fixtures_for_celery import *

# CELERY_ALWAYS_EAGER doesn't get configure without celery import in envconf
from review import envconf
envconf.configure('review.settings_test')

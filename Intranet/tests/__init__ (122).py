import os

from uhura import environment


environment.setup_environment()  # noqa: E402
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "uhura.settings")  # noqa: E402

import django
django.setup()  # noqa: E402

from vins_core.nlu.flow_nlu_factory.transition_model import register_transition_model
from uhura.lib.vins.transition.transition_model import create_transition_model

register_transition_model('uhura', create_transition_model)

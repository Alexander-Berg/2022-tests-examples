from .settings_tvm import *  # noqa
from .settings_conf import ConfApplier
from .settings_base import *  # noqa


conf_applier = ConfApplier(ENV_TYPE)  # noqa: F405
conf_applier.apply()

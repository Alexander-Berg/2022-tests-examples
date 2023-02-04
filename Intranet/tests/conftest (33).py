from typing import Type
import logging

import pytest


from staff.oebs.controllers import updaters, datagenerators
from staff.oebs.models import OEBSModelBaseT


logger = logging.getLogger(__name__)


@pytest.fixture
def build_updater():
    def wrapped(model: Type[OEBSModelBaseT], datasource=None, custom_logger=None):
        custom_logger = custom_logger or logger
        datasource = datasource or model.datasource_class(object_type=model.oebs_type, method=model.method)
        datagenerator_class = datagenerators.library[model]
        datagenerator = datagenerator_class(datasource)
        data_diff_merger = updaters.OEBSDataDiffMerger(datagenerator, custom_logger)
        result = updaters.OEBSUpdater(data_diff_merger, custom_logger)
        return result

    return wrapped

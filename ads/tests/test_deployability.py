from pclick_tsar4 import make_model
from pclick_tsar4.model import ProductionModel
from typing import Any, Optional, AsyncContextManager
import pytest
import tempfile
import contextlib
import datetime
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.core.disk_adapter import DiskSavePool
from deploy.callbacks.cpp_apply_processor import CppApplyProcessorCallback
from ads_pytorch.online_learning.production.uri import ProdURI, DatetimeURI
from ads_pytorch.online_learning.production.artifact.storage import AbstractArtifactStorage, NewValueCtx


def test_make_model(model_config):
    model = make_model(model_conf=model_config)
    assert isinstance(model, ProductionModel)


class FakeStorage(AbstractArtifactStorage):

    @contextlib.asynccontextmanager
    async def new_artifact_path(self, artifact_name: str, uri: Any, parent_tx: Optional[Any] = None) -> \
    AsyncContextManager[NewValueCtx]:
        with tempfile.TemporaryDirectory() as tmp_dir:
            yield NewValueCtx(path=tmp_dir, tx=parent_tx)


@pytest.mark.asyncio
async def test_deploy(model_config):
    model = make_model(model_conf=model_config)
    cb = CppApplyProcessorCallback(
        file_system_adapter=CypressAdapterMock(),
        upload_pool=DiskSavePool(10, ),
        artifact_storage=FakeStorage(),
        artifact_name="tsar_processed_model",
        min_frequency=0,
    )

    await cb(
        model=model,
        optimizer=None,
        loss=None,
        uri=ProdURI(
            uri=DatetimeURI("//home/f1", date=datetime.datetime.now()),
            force_skip=False
        )
    )

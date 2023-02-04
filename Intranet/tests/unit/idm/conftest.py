import pytest

from tvm2 import TVM2


@pytest.fixture
def patch_tvm(monkeypatch):
    monkeypatch.setattr(TVM2, '_init_context', lambda *args, **kwargs: None)
    monkeypatch.setattr('plan.idm.manager.get_tvm_ticket', lambda *args, **kwargs: '1')

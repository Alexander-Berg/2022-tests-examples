import mock
import pytest


@pytest.fixture
def no_denormalization(monkeypatch):
    patch_targets = (
        'plan.denormalization.update.update_denormalized_field',
        'plan.denormalization.tasks.update_denormalized_field',
    )

    for target in patch_targets:
        monkeypatch.setattr(target, mock.Mock())

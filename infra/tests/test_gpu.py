import pytest

from infra.rtc.walle_validator.lib.filters import HasLabel
from infra.rtc.walle_validator.lib.coverage import create_coverage

label_gpu_coverage = create_coverage()


@pytest.mark.project_filter(HasLabel("gpu", "nvidia"), label_gpu_coverage)
def test_gpu_nvidia(project):
    parts = project.id.split("-")
    assert "gpu" in parts or "bert" in parts or "gpu2" in parts


@pytest.mark.project_filter(HasLabel("gpu", "vfio"), label_gpu_coverage)
def test_gpu_vfio(project):
    parts = project.id.split("-")
    assert "dev" in parts


@pytest.mark.project_filter(HasLabel("gpu", "none"), label_gpu_coverage)
def test_gpu_none(project):
    parts = project.id.split("-")
    assert "gpu" not in parts


def test_label_gpu_coverage(all_projects):
    label_gpu_coverage.check(all_projects)

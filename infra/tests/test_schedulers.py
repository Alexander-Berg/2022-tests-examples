import pytest

from infra.rtc.walle_validator.lib.filters import HasLabel
from infra.rtc.walle_validator.lib.coverage import create_coverage

scheduler_coverage = create_coverage()


@pytest.mark.project_filter(HasLabel("scheduler", "yp"), scheduler_coverage)
def test_yp_scheduler(project):
    if "reserve" in project.id:
        return
    assert "yp" in project.id.split("-")
    assert "rtc" not in project.id.split("-")


@pytest.mark.project_filter(HasLabel("scheduler", "qloud"), scheduler_coverage)
def test_qloud_scheduler(project):
    assert "qloud" in project.id.split("-")


@pytest.mark.project_filter(HasLabel("scheduler", "gencfg"), scheduler_coverage)
def test_gencfg_scheduler(project):
    if project.id in (
        "yp-iva", "yp-man", "yp-man-pre", "yp-msk", "yp-sas", "yp-sas-test", "yp-vla", "yp-xdc", "search-testing", "yp-prestable"
    ):
        return
    assert "rtc" in project.id.split("-")


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), scheduler_coverage)
def test_sandbox_scheduler(project):
    assert "sandbox" in project.id.split("-")


@pytest.mark.project_filter(HasLabel("scheduler", "none"), scheduler_coverage)
def test_none_scheduler(project):
    assert project.labels.get("scheduler") == "none"


def test_scheduler_coverage(all_projects):
    scheduler_coverage.check(all_projects)

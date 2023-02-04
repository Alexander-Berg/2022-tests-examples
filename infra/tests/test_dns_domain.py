import pytest

from infra.rtc.walle_validator.lib.filters import HasLabel, HasTag, HasNoTag, Not, And, Or
from infra.rtc.walle_validator.lib.coverage import create_coverage
from infra.rtc.walle_validator.lib.constants import YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG, YT_TAG

dns_domain_coverage = create_coverage()


@pytest.mark.project_filter(HasLabel("stage", "core"), dns_domain_coverage)
def test_core_dns_domain(project):
    # TODO: rename hosts
    assert project.dns_domain in ("search.yandex.net", "core-c.yandex.net")


@pytest.mark.project_filter(And([
    Not(HasLabel("stage", "core")),
    HasNoTag(YT_TAG)
]), dns_domain_coverage)
def test_other_dns_domain(project):
    if project.id in ("qloud-dns", "qloud-testing"):
        return
    assert project.dns_domain == "search.yandex.net"


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG)
]), dns_domain_coverage)
def test_yp_dns_domain(project):
    if project.labels.get("stage") == "core":
        assert project.dns_domain == "core-c.yandex.net"
    else:
        assert project.dns_domain == "yp.yandex.net"


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]), dns_domain_coverage)
def test_yt_dns_domain(project):
    if project.id == "rtc-yt-hume-masters":
        assert project.dns_domain == "hume.yt.yandex.net"
    elif project.id == "rtc-yt-landau-masters":
        assert project.dns_domain == "landau.yt.yandex.net"
    elif project.id == "rtc-yt-markov-masters":
        assert project.dns_domain == "markov.yt.yandex.net"
    elif project.id == "rtc-yt-pythia-masters":
        assert project.dns_domain == "pythia.yt.yandex.net"
    elif project.id == "rtc-yt-socrates-masters":
        assert project.dns_domain == "socrates.yt.yandex.net"
    elif project.id == "rtc-yt-zeno-masters":
        assert project.dns_domain == "zeno.yt.yandex.net"
    else:
        assert project.dns_domain == "search.yandex.net"


def test_dns_domain_coverage(all_projects):
    dns_domain_coverage.check(all_projects)

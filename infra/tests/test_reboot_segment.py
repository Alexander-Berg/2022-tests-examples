# coding: utf-8
import pytest


from infra.rtc.walle_validator.lib.filters import And, Or, HasTag, HasNoTag, HasLabel
from infra.rtc.walle_validator.lib.constants import (
    YT_TAG, YP_TAG, YABS_TAG, MARKET_TAG, SPECIAL_REBOOT_TAG, YP_MASTERS_TAG
)
from infra.rtc.walle_validator.lib.coverage import create_coverage
from infra.rtc.walle_validator.lib.transform import is_yp_master, is_yp_nonprod_master
from infra.rtc.walle_validator.lib.transform import get_yt_cluster_name

reboot_segment_coverage = create_coverage()


def not_special_reboot(cond):
    return And([cond, HasNoTag(SPECIAL_REBOOT_TAG)])


def is_prestable(project):
    return project.labels.get('stage') in ('prestable', 'experiment')


@pytest.mark.project_filter(HasTag(SPECIAL_REBOOT_TAG), reboot_segment_coverage)
def test_special_reboot_segment(project):
    assert not project.labels.get('reboot_segment'), "`reboot_segment` label on project with #special_reboot {}".format(project.id)


@pytest.mark.project_filter(not_special_reboot(HasLabel("scheduler", "qloud")), reboot_segment_coverage)
def test_reboot_segment_for_qloud_scheduler(project):
    reboot_segment = project.labels.get('reboot_segment')
    if is_prestable(project):
        assert reboot_segment == 'qloud-pre'
    else:
        assert reboot_segment == 'qloud'


@pytest.mark.project_filter(not_special_reboot(Or([HasLabel("scheduler", "gencfg"), HasLabel("scheduler", "yp")])), reboot_segment_coverage)
def test_reboot_segment_for_gencfg_scheduler(project):
    reboot_segment = project.labels.get('reboot_segment')
    gpu = project.labels.get('gpu')
    if project.labels.get('stage') == 'experiment':
        if "yp_master_testing" in project.tags:
            assert reboot_segment == 'yp_devmasters'
        else:
            assert reboot_segment == 'experiment'
    elif is_yp_master(project):
        if is_yp_nonprod_master(project):
            assert reboot_segment == 'yp_devmasters'
        else:
            assert reboot_segment == 'yp_masters'
    elif MARKET_TAG in project.tags and is_prestable(project):
        assert reboot_segment == 'market-pre'
    elif YT_TAG in project.tags:
        id_parts = project.id.split("-")
        if 'masters' in id_parts:
            assert reboot_segment == 'yt_masters'
        elif is_prestable(project):
            assert get_yt_cluster_name(project) in reboot_segment
            if any([x in id_parts for x in ('arnold', 'hahn')]):
                assert reboot_segment == 'yt-pre-%s' % get_yt_cluster_name(project)
        elif 'hahn' in id_parts:
            if gpu == 'nvidia':
                assert reboot_segment == 'yt-hahn-gpu'
            else:
                assert reboot_segment == 'yt-hahn'
        elif 'arnold' in id_parts:
            if gpu == 'nvidia':
                assert reboot_segment == 'yt-arnold-gpu'
            else:
                assert reboot_segment == 'yt-arnold'
        elif project.id in ('rtc-yt-mtn', 'rtc-yt-mtn-amd'):
            assert reboot_segment == 'yt-arnold'
        elif 'vanga' in id_parts:
            assert reboot_segment == 'yt-vanga'
        elif 'pythia' in id_parts:
            assert reboot_segment == 'yt-pythia'
        elif 'seneca' in id_parts:
            assert reboot_segment == 'yt-seneca'
        elif 'ofd' in id_parts and 'xdc' in id_parts:
            assert reboot_segment == 'yt-ofd-xdc'
        elif 'bohr' in id_parts:
            assert reboot_segment == 'yt-bohr'
        elif 'landau' in id_parts:
            assert reboot_segment == 'yt-landau'
        elif 'markov' in id_parts:
            assert reboot_segment == 'yt-markov'
        elif 'socrates' in id_parts:
            assert reboot_segment == 'yt-socrates'
        elif 'locke' in id_parts:
            assert reboot_segment == 'yt-locke'
        elif 'nash' in id_parts:
            assert reboot_segment == 'yt-nash'
        else:
            assert False
    elif is_prestable(project):
        assert reboot_segment == 'prestable-def'
    elif YABS_TAG in project.tags:
        assert reboot_segment == 'yabs'
    elif MARKET_TAG in project.tags:
        assert reboot_segment == 'market'
    elif YP_TAG in project.tags:
        if project.id.endswith('sandbox'):
            assert reboot_segment == 'sandbox'
        elif project.id.endswith('distbuild'):
            assert reboot_segment == 'distbuild'
        elif project.id.endswith('dbaas'):
            assert reboot_segment == 'dbaas'
        else:
            assert reboot_segment == 'yp'
    else:
        assert reboot_segment == 'gencfg'


@pytest.mark.project_filter(not_special_reboot(HasLabel("scheduler", "sandbox")), reboot_segment_coverage)
def test_reboot_segment_for_sandbox_scheduler(project):
    assert project.labels.get('reboot_segment') == 'sandbox'


@pytest.mark.project_filter(not_special_reboot(HasLabel("scheduler", "none")), reboot_segment_coverage)
def test_reboot_segment_for_none_scheduler(project):
    reboot_segment = project.labels.get('reboot_segment')
    if project.id == "rtc-yt-inbox":
        assert project.labels.get('reboot_segment') == 'incoming'
    elif YP_MASTERS_TAG in project.tags:
        assert project.labels.get('reboot_segment') == 'yp_masters'
    elif project.labels.get('stage') == 'experiment':
        assert reboot_segment == 'experiment'
    else:
        assert project.labels.get('reboot_segment') == 'prestable-def'


@pytest.mark.project_filter(HasNoTag(SPECIAL_REBOOT_TAG))
def test_maxwell_spec_for_reboot_segment(project, maxwell_specs):
    reboot_segment = project.labels['reboot_segment']
    maxwell_spec_name = "reboot-segment-{}".format(reboot_segment)
    assert maxwell_spec_name in maxwell_specs, "no maxwell spec {} found".format(maxwell_spec_name)
    maxwell_spec = maxwell_specs[maxwell_spec_name]
    assert maxwell_spec.config["source"]["walle"]["tags"] == ["rtc.reboot_segment-{}".format(reboot_segment)]


def test_reboot_segment_coverage(all_projects):
    reboot_segment_coverage.check(all_projects)

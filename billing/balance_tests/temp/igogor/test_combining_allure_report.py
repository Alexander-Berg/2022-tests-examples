# coding: utf-8
import pytest
from lxml import etree
from lxml import objectify

import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.shared import SharedBefore, SharedBlock


@pytest.mark.shared(block='Temp')
@reporter.feature('OloloFeature')
def test_combine_report(shared_data):
    # with reporter.step(u'Степ до before'):
    #     reporter.attach(u'Аттач до before', u'Аттач до before')
    with SharedBefore(shared_data=shared_data, cache_vars=['cache_var1', 'cache_var2']) as before:
        before.validate()
        cache_var1 = 1
        cache_var2 = "2"
        with reporter.step(u'Степ внутри before'):
            reporter.attach(u'Аттач внутри before', u'Cache in before: cache_var1 = {}  cache_var2 = {}'.format(
                cache_var1, cache_var2))
    # with reporter.step(u'Степ между before и block'):
    #     reporter.attach(u'Аттач между before и block', u'Cache: cache_var1 = {}  cache_var2 = {}'.format(cache_var1,
    #                                                                                                 cache_var2))
    with SharedBlock(shared_data=shared_data, before=before, block_name='Temp') as block:
        block.validate()
        with reporter.step(u'Степ внутри block'):
            reporter.attach(u'Аттач внутри block', u'Piu piu')
    with reporter.step(u'Степ после block'):
        reporter.attach(u'Аттач после block', u'Cache after block: cache_var1 = {}  cache_var2 = {}'.format(
            cache_var1, cache_var2))


def test_sutes_merge():
    beforepath = utils.project_file('allure-data-before/ca37931f-08e5-4fd3-89e0-f2a8a4aa075d-testsuite.xml')
    blockpath = utils.project_file('allure-data-block/d3182e0e-383f-47bd-9aaf-02610306cd0e-testsuite.xml')
    afterpath = utils.project_file('allure-data-after/95c42c1a-bb47-4d9a-b025-570c595db796-testsuite.xml')

    beforesuite, beforetests = _parse_test_suite(beforepath)
    blocksuite, blocktests = _parse_test_suite(blockpath)
    aftersuite, aftertests = _parse_test_suite(afterpath)

    beforetests['test_combine_report']['{}steps'].extend(blocktests['test_combine_report']['{}steps']['{}step'])
    beforetests['test_combine_report']['{}steps'].extend(aftertests['test_combine_report']['{}steps']['{}step'])

    aftertests['test_combine_report']['{}steps'] = beforetests['test_combine_report']['{}steps']
    result = etree.tostring(aftersuite, encoding='utf-8', pretty_print=True)

    with open(utils.project_file('allure-data-temp/95c42c1a-bb47-4d9a-b025-570c595db796-testsuite.xml'), 'wb') as f:
        f.write(result)

    # todo-igogor может пригодиться для подчистки неймспейсов, но лучше скопипастить как в аллюре делается кмк
    # objectify.deannotate(root, xsi_nil=True, cleanup_namespaces=True)
    pass


def _parse_test_suite(filepath):
    with open(filepath, 'rb') as f:
        xmlstr = f.read().decode(encoding='utf-8')
        # suite = etree.fromstring(xmlstr)
        suite = objectify.XML(xmlstr)
        tests = {test['{}name']: test for test in suite.xpath('//test-cases/test-case')}
        return suite, tests

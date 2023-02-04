import time

from teamcity.messages import TeamcityServiceMessages
from teamcity.pytest_plugin import EchoTeamCityMessages


def pytest_addoption(parser):
    group = parser.getgroup('terminal reporting', 'reporting', after='general')
    group._addoption(
        '--teamcity-ex', action='count', dest='teamcity_ex', default=0,
        help='output extended teamcity messages'
    )
    group._addoption(
        '--teamcity-flow-prefix', action='store', dest='teamcity_flow_prefix', default='pytest',
        help='teamcity flow prefix',
    )
    group._addoption(
        '--teamcity-global-suite', action='store', dest='teamcity_global_suite', default='',
        help='teamcity global suite'
    )


def pytest_configure(config):
    if config.option.teamcity_ex >= 1:
        if getattr(config.option, 'teamcity', 0) >= 1:
            raise Exception('--teamcity and --teamcity-ex can be specified at the same time')
        config._teamcityReporting = EchoTeamCityExMessages(
            flow_prefix=config.option.teamcity_flow_prefix,
            global_suite=config.option.teamcity_global_suite
        )
        config.pluginmanager.register(config._teamcityReporting)


def pytest_unconfigure(config):
    teamcityReporting = getattr(config, '_teamcityReporting', None)
    if teamcityReporting:
        del config._teamcityReporting
        config.pluginmanager.unregister(teamcityReporting)


class TeamcityExServiceMessages(TeamcityServiceMessages):
    def __init__(self, output, flow_prefix):
        self.output = output
        self.flow_prefix = flow_prefix

    def message(self, messageName, **properties):
        flowId = properties.get('flowId', '')
        properties['flowId'] = self.flow_prefix
        if flowId:
            properties['flowId'] += flowId
        return TeamcityServiceMessages.message(self, messageName, **properties)

    def testFinished(self, testName, duration=None):
        kwargs = {}
        if duration is not None:
            kwargs['duration'] = str(int(duration * 1000))
        self.message('testFinished', name=testName, **kwargs)


class EchoTeamCityExMessages(EchoTeamCityMessages):
    def __init__(self, *args, **kwargs):
        self.flow_prefix = kwargs.pop('flow_prefix', '')
        self.global_suite = kwargs.pop('global_suite', '')
        self.global_suite_started = False

        EchoTeamCityMessages.__init__(self, *args, **kwargs)
        self.teamcity = TeamcityExServiceMessages(self.tw, self.flow_prefix)
        self.times = {}

    def pytest_runtest_logstart(self, nodeid, location):
        if self.global_suite and not self.global_suite_started:
            self.teamcity.testSuiteStarted(self.global_suite)
            self.global_suite_started = True
        self.times.setdefault(nodeid, time.time())
        return EchoTeamCityMessages.pytest_runtest_logstart(self, nodeid, location)

    def pytest_runtest_logreport(self, report):
        file, testname = self.format_names(report.nodeid)

        if report.skipped:
            self.teamcity.testIgnored(testname, str(report.longrepr))
        elif report.failed:
            self.teamcity.testFailed(testname, str(report.location), str(report.longrepr))

        if report.skipped or report.when == 'teardown':
            self.teamcity.testFinished(testname, duration=time.time() - self.times[report.nodeid])

    def pytest_sessionfinish(self, session, exitstatus, __multicall__):
        EchoTeamCityMessages.pytest_sessionfinish(self, session, exitstatus, __multicall__)
        if self.global_suite_started:
            self.teamcity.testSuiteFinished(self.global_suite)

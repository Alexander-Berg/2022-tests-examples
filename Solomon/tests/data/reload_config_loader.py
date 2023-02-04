from __future__ import print_function

import sys

class TestPullModule:
    def __init__(self, logger, registry, **kwargs):
        self._metric = registry.counter({'sensor': 'call_count'})

    def pull(self, ts, consumer):
        self._metric.inc()


def python2_pull_config(file_path, module_name, class_name, params={}):
    config = {
        'FilePath': file_path,
        'ModuleName': module_name,
        'ClassName': class_name,
    }
    if params:
        config['Params'] = params
    return {'Python2': config}


def service_config(project, service, modules, labels=[], pull_interval='15s'):
    return {
        'Project': project,
        'Service': service,
        'PullInterval': pull_interval,
        'Labels': labels,
        'Modules': modules
    }

class TestConfigLoader(object):
    def __init__(self, logger, **kw):
        self.i = 0

    def load(self):
        if self.i == 0:
            modules = [
                python2_pull_config(__file__, "test", "TestPullModule", {"foo": "42"}),
                python2_pull_config(__file__, "test", "TestPullModule", {"foo": "43"}),
            ]
        else:
            modules = [
                python2_pull_config(__file__, "test", "TestPullModule", {"foo": "42"})
            ]

        self.i += 1

        return [
            service_config("solomon", "test", modules),
        ]

class TestPullModule:
    def __init__(self, logger, registry):
        self._metric = registry.counter({'sensor': 'call_count'})

    def pull(self, ts, consumer):
        self._metric.inc()


class TestHistPullModule:
    def __init__(self, logger, registry):
        self._hist_counter_explicit = registry.histogram_counter({'sensor': 'hist_counter_explicit'}, 'explicit', buckets=[10, 50, 100])
        self._hist_rate_linear = registry.histogram_rate({'sensor': 'hist_rate_linear'}, 'linear', bucket_count=6, start_value=5, bucket_width=15)
        self._hist_rate_exponential = registry.histogram_rate({'sensor': 'hist_rate_exponential'}, 'exponential', bucket_count=6, scale=3, base=2)

    def pull(self, ts, consumer):
        self._hist_counter_explicit.collect(10)
        self._hist_rate_linear.collect(100)
        self._hist_rate_exponential.collect(500, 5)


class TestPullModuleForPushMode:
    def __init__(self, logger, registry, **kw):
        self._metric = registry.gauge({'sensor': kw['start_value'] + 'x'})  # 3x, 5x, 7x
        self._metric.set(int(kw['start_value']))

    def pull(self, ts, consumer):
        self._metric.set(self._metric.get() * 2)


class TestPullModuleCancelling:
    def __init__(self, logger, registry):
        self._metric = registry.counter({'sensor': 'call_count'})

    def pull(self, ts, consumer):
        self._metric.inc()

        return 1


def python2_pull_config(file_path, module_name, class_name, params={}):
    config = {
        'FilePath': file_path,
        'ModuleName': module_name,
        'ClassName': class_name,
    }
    if params:
        config['Params'] = params
    return {'Python2': config}


def service_config(project, service, modules, labels=[], pull_interval='5s'):
    return {
        'Project': project,
        'Service': service,
        'PullInterval': pull_interval,
        'Labels': labels,
        'Modules': modules
    }


class TestConfigLoader(object):
    def __init__(self, logger, **kw):
        pass

    def load(self):
        return [
            service_config("solomon", "test", [
                python2_pull_config(__file__, "test", "TestPullModule"),
            ])
        ]


class TestCancellingConfigLoader(object):
    def __init__(self, logger, **kw):
        pass

    def load(self):
        return [
            service_config("solomon", "test", [
                python2_pull_config(__file__, "test", "TestPullModuleCancelling"),
            ])
        ]


class TestHistConfigLoader(object):
    def __init__(self, logger, **kw):
        pass

    def load(self):
        return [
            service_config("solomon", "test", [
                python2_pull_config(__file__, "test", "TestHistPullModule"),
            ])
        ]

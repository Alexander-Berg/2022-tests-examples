import yt.wrapper as yt
from datetime import datetime
from tests.base import BalanceTest
from cluster_tools.stat_aggregator import get_stager_project
from balance.processors.stager.config import BalanceConfigurationAdapter
from balance.utils import yt_helpers


class StagerTest(BalanceTest):
    project_name = 'default'
    purpose = 'main'

    def __init__(self, *args, **kwargs):
        self._mock_tables = {}
        self._config = None
        self._storage = None
        self.project = None
        self.today = datetime.now().strftime('%Y-%m-%d')
        super(StagerTest, self).__init__(*args, **kwargs)

    @property
    def config(self):
        return self._config

    @config.setter
    def config(self, value):
        self._config = value
        yt.update_config(self._config.yt_config)

    @property
    def storage(self):
        return self._storage

    @storage.setter
    def storage(self, value):
        if not yt.exists(value):
            yt.mkdir(value, recursive=True)
        self._storage = value
        self.config.path_substitute(self._storage)

    def mock_table(self, alias, data):
        assert len(data) > 1, 'Schema must be specified in the first row'
        assert alias not in self._mock_tables, 'Already mocked'

        schema = data[0]
        rows = data[1:]

        self._mock_tables[alias] = map(dict, [zip(schema, row) for row in rows])

        # create necessary table
        input_tables = {alias: '{}/{}'.format(self.storage, alias)}
        yt.write_table(input_tables[alias],
                       self._mock_tables[alias],
                       format=yt.JsonFormat())

    def read_table(self, alias):
        return yt.read_table(
            self.config.nodes[alias].path,
            format=yt.JsonFormat())

    def get_aggregation_results(self, *output_tables):
        return {alias: tuple(self.read_table(alias)) for alias in output_tables}

    def disable_actors(self, *actors):
        map(self.config.disable_actor, actors)

    def create_stager_project(self, meta=None):
        if meta:
            self.config._meta.update(meta)
        return get_stager_project(self.project_name)(self.config)

    def run_project(self, meta=None, imitate=False):
        if not imitate:
            self.project = self.create_stager_project(meta)
            self.project.start_maybe()
        return self.get_aggregation_results

    def get_output(self, *tables):
        return {alias: tuple([d for d in yt.read_table(
            self.config.nodes[alias].path, format=yt.JsonFormat())])
                for alias in tables}

    def setUp(self):
        from balance.processors.stager.defaults.module_filters import default_module_filter

        self.config = BalanceConfigurationAdapter(
            name=self.project_name,
            purpose=self.purpose,
            # yt_token_storage=lambda x: None,
            yt_token_storage=yt_helpers.get_token,
            balance_config=self.app.cfg,
            cfk={
                'date': self.today,
                'project': self.project_name
            },
            meta={
                'force_sync_aggregation': False,
            })

        self.config.yt_config.update({'pickling': {'module_filter': default_module_filter}})

        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=datetime.now().strftime('%Y-%m-%d'))

        super(StagerTest, self).setUp()

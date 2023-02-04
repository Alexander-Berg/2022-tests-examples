from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.nirvana_graph import Worker as NirvanaGraphWorker


class NirvanaGraphWorkerTestCase(BaseWorkerTestCase):
    worker_class = NirvanaGraphWorker
    worker_class_file = 'nirvana_graph'

    def test_nirvana_graph_parse_url(self):
        hostname_match = 'nirvana.yandex-team.ru'
        graph_id = '936e00e0-8f72-11e5-8ed5-0025909427cc'
        block_id = '944b1d29-8f72-11e5-8ed5-0025909427cc'
        instance_id = '944b1d29-8f72-11e5-8ed5-0025909427cc44'
        urls_data = (
            ('https://nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                              block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/options/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                 block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/options/operation/{}/'.format(graph_id,
                                                                                   block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                               block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                 block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                  block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/graph'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/graph/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/options'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/options/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}/graph'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}/graph/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}/options'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://nirvana.yandex-team.ru/flow/{}/{}/options/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),

            ('https://nirvana.yandex-team.ru/process/{}/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                     instance_id,
                                                                                                     block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://nirvana.yandex-team.ru/process/{}/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                    instance_id,
                                                                                                    block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://nirvana.yandex-team.ru/process/{}/{}/graph/operation/{}/'.format(graph_id,
                                                                                       instance_id,
                                                                                       block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://nirvana.yandex-team.ru/process/{}/{}/graph/operation/{}'.format(graph_id,
                                                                                      instance_id,
                                                                                      block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/options/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/options'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/graph/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://nirvana.yandex-team.ru/process/{}/graph'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
        )
        for url, path_data in urls_data:
            self.parse_url(url, hostname_match, path_data)

    def test_nirvana_graph_url_with_wrong_block_success(self):
        graph_id = '936e00e0-8f72-11e5-8ed5-0025909427cc'
        wrong_block_id = '944b1d29-8'
        url = 'https://nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                               wrong_block_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='split-by-threshold-test___2'),
                    User(login='kazah'),
                ]
            )
        }
        cassette_name = 'nirvana_graph_url_with_wrong_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_with_block_success(self):
        graph_id = '936e00e0-8f72-11e5-8ed5-0025909427cc'
        block_id = '944b1d29-8f72-11e5-8ed5-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                               block_id,
                                                                                               )
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='split-by-threshold-test___2 \u2192 Split By Last Column By Threshold'),
                    User(login='kazah')
                ],
            )
        }
        cassette_name = 'nirvana_graph_with_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_link_without_block_failed(self):
        wrong_graph_id = '936e00e0-8'
        url = 'https://nirvana.yandex-team.ru/flow/{}'.format(wrong_graph_id)
        cassette_name = 'nirvana_graph_without_block_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))

    def test_nirvana_graph_url_without_block_success(self):
        graph_id = '936e00e0-8f72-11e5-8ed5-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/flow/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='split-by-threshold-test___2'),
                    User(login='kazah'),
                ]
            )
        }

        cassette_name = 'nirvana_graph_without_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_without_name_success(self):
        graph_id = '9e7d4081-9e7a-4943-99ad-983c41d5fd7a'
        url = 'https://nirvana.yandex-team.ru/process/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='9e7d4081-9e7a-4943-99ad-983c41d5fd7a'),
                    User(login='alb82'),
                ]
            )
        }

        cassette_name = 'nirvana_graph_without_name_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_without_name_with_block_success(self):
        graph_id = '9e7d4081-9e7a-4943-99ad-983c41d5fd7a'
        block_id = '84cf80a7-4f12-475b-a33e-7cccdae31b33'
        url = 'https://nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                  block_id,
                                                                                                  )
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='9e7d4081-9e7a-4943-99ad-983c41d5fd7a → [yabs.tabtools] arbitrary mappers (inner)'),
                    User(login='alb82'),
                ]
            )
        }

        cassette_name = 'nirvana_graph_without_name_with_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_status_cancel(self):
        graph_id = '17bf8e8f-dbc4-11e6-a873-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/flow/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Canceled', color='#eabf3d'),
                    String(value='stop_words_1.4'),
                    User(login='adgval'),
                ]
            )
        }
        cassette_name = 'nirvana_graph_status_cancel.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_status_failure(self):
        graph_id = '66fe561d-83e0-11e4-9392-00259095835a'
        url = 'https://nirvana.yandex-team.ru/flow/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Failure', color='#ff0000'),
                    String(value='Test Train Mn clone'),
                    User(login='vpdelta'),
                ]
            )
        }
        cassette_name = 'nirvana_graph_status_failure.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_status_not_started(self):
        graph_id = '2f8f0af7-2d6c-11e6-a29e-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/flow/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana',
                    ),
                    String(value='Not Started', color='#999999'),
                    String(value='QA_sbs_kpi_desktop_ru'),
                    User(login='steiner'),
                ]
            )
        }

        cassette_name = 'nirvana_graph_status_not_started.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_process_with_block_success(self):
        graph_id = 'fb07be6e-02c0-11e7-a873-0025909427cc'
        block_id = '11ee09dd-ef6a-11e6-a873-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                  block_id,
                                                                                                  )
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='Videotop premoderation in toloka \u2192 get top 600, just to be safe'),
                    User(login='robot-videotop-back')
                ],
            )
        }

        cassette_name = 'nirvana_graph_process_with_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_process_with_wrong_block_success(self):
        graph_id = 'fb07be6e-02c0-11e7-a873-0025909427cc'
        block_id = '11ee09dd-ef6a-11e6-a873-0025909427cc22'
        url = 'https://nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                  block_id,
                                                                                                  )
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='Videotop premoderation in toloka'),
                    User(login='robot-videotop-back')
                ],
            )
        }

        cassette_name = 'nirvana_graph_process_with_wrong_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_process_without_block_success(self):
        graph_id = 'fb07be6e-02c0-11e7-a873-0025909427cc'
        url = 'https://nirvana.yandex-team.ru/process/{}'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='Videotop premoderation in toloka'),
                    User(login='robot-videotop-back')
                ],
            )
        }

        cassette_name = 'nirvana_graph_process_without_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_link_process_without_block_failed(self):
        wrong_graph_id = '936e00e0-8'
        url = 'https://nirvana.yandex-team.ru/process/{}'.format(wrong_graph_id)
        cassette_name = 'nirvana_graph_process_without_block_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))

    def test_nirvana_graph_url_process_without_block_with_options_success(self):
        graph_id = '9e7d4081-9e7a-4943-99ad-983c41d5fd7a'
        url = 'https://nirvana.yandex-team.ru/process/{}/options'.format(graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Success', color='#3dbf41'),
                    String(value='ArbitraryMappers'),
                    User(login='alb82')
                ],
            )
        }

        cassette_name = 'nirvana_graph_process_without_block_with_options_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_flow_with_instance_id_success(self):
        wf_id = '32c9bffc-3c20-44d5-b98c-acde2098762e'
        instance_id = '06858df4-b0a7-480a-b880-21b6636ed6ae'
        graph_id = 'f9f02286-7bca-4807-b94a-cb4747e32f06'
        url = 'https://nirvana.yandex-team.ru/flow/{}/{}/graph/operation/{}'.format(wf_id, instance_id, graph_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Canceled', color='#eabf3d'),
                    String(value='NIRVANA-6420 \u2192 ArbitraryMappers'),
                    User(login='finiriarh')
                ],
            )
        }

        cassette_name = 'nirvana_graph_flow_with_instance_id_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_url_flow_with_instance_id_without_block_success(self):
        wf_id = '32c9bffc-3c20-44d5-b98c-acde2098762e'
        instance_id = '06858df4-b0a7-480a-b880-21b6636ed6ae'
        url = 'https://nirvana.yandex-team.ru/flow/{}/{}/graph'.format(wf_id, instance_id)
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Nirvana'
                    ),
                    String(value='Canceled', color='#eabf3d'),
                    String(value='NIRVANA-6420'),
                    User(login='finiriarh')
                ],
            )
        }

        cassette_name = 'nirvana_graph_flow_with_instance_id_without_block_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nirvana_graph_parse_url_beta(self):
        hostname_match = 'beta.nirvana.yandex-team.ru'
        graph_id = '936e00e0-8f72-11e5-8ed5-0025909427cc'
        block_id = '944b1d29-8f72-11e5-8ed5-0025909427cc'
        instance_id = '944b1d29-8f72-11e5-8ed5-0025909427cc44'
        urls_data = (
            ('https://beta.nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                              block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/options/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                 block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/options/operation/{}/'.format(graph_id,
                                                                                   block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                               block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                 block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                  block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/graph'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/graph/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/options'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/options/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}/graph'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}/graph/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}/options'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),
            ('https://beta.nirvana.yandex-team.ru/flow/{}/{}/options/'.format(graph_id, instance_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'flow',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44'}
             ),

            ('https://beta.nirvana.yandex-team.ru/process/{}/{}/graph/FlowchartBlockOperation/{}/'.format(graph_id,
                                                                                                     instance_id,
                                                                                                     block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://beta.nirvana.yandex-team.ru/process/{}/{}/graph/FlowchartBlockOperation/{}'.format(graph_id,
                                                                                                    instance_id,
                                                                                                    block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://beta.nirvana.yandex-team.ru/process/{}/{}/graph/operation/{}/'.format(graph_id,
                                                                                       instance_id,
                                                                                       block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),

            ('https://beta.nirvana.yandex-team.ru/process/{}/{}/graph/operation/{}'.format(graph_id,
                                                                                      instance_id,
                                                                                      block_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'block_id': '944b1d29-8f72-11e5-8ed5-0025909427cc',
              'instance_id': '944b1d29-8f72-11e5-8ed5-0025909427cc44',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/options/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/options'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/graph/'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
            ('https://beta.nirvana.yandex-team.ru/process/{}/graph'.format(graph_id),
             {'id': '936e00e0-8f72-11e5-8ed5-0025909427cc',
              'type': 'process'}
             ),
        )
        for url, path_data in urls_data:
            self.parse_url(url, hostname_match, path_data)

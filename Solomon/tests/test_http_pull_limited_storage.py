import time

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig
from solomon.agent.protos.storage_config_pb2 import TStorageConfig

import misc


# Variables for pytest fixtures
CONF_PATH = yatest.common.test_source_path('data/test_limited_storage.conf')

# Variables for tests
FETCHER_ID = 'agent-storage-test'
FETCHER_HEADER = 'X-Solomon-FetcherId'
SEQ_NO_HEADER = 'X-Solomon-SequenceNumber'
NEXT_SEQ_NO_HEADER = 'X-Solomon-NextSequenceNumber'

SERVICES = ['default_limit', 'small_limit', 'big_limit']
SERVICES_LIMITS = {
    'default_limit': 5 * 1024,
    'small_limit': 10 * 1024,
    'big_limit': 20 * 1024,
}

HTTP_PUSH_CONF = """
    HttpPush {
        BindAddress: "127.0.0.1"
        BindPort: %(port)s
        Name: "httpPush"

        Handlers {
            Project: "solomon"
            Service: "big_limit"

            Endpoint: "/big_limit"
        }

        Handlers {
            Project: "solomon"
            Service: "small_limit"

            Endpoint: "/small_limit"
        }

        Handlers {
            Project: "solomon"
            Service: "default_limit"

            Endpoint: "/default_limit"
        }

        # Special shard for estimating size of data stored in it
        Handlers {
            Project: "solomon"
            Service: "misc"

            Endpoint: "/misc"
        }
    }
"""

STORAGE_CONF = """
    Limit {
        Total: "%(total_limit)s"
        ShardDefault: "5KiB"
    }

    # Overrides default
    Shard {
        Project: "solomon",
        Service: "small_limit",
        Limit: "10KiB"
    }

    Shard {
        Project: "solomon",
        Service: "big_limit",
        Limit: "20KiB"
    }
"""


def create_metrics(values):
    if not isinstance(values, list):
        values = [values]

    return {
        "sensors": [
            {
                "value": value,
                "labels": {
                    "metric": "test_metric" + "{:0>3}".format(num)
                },
                "kind": "GAUGE"
            }
            for (num, value) in enumerate(values)
        ]
    }


INIT_METRICS = create_metrics(-1)

AGENT_SERVER_PORT = None
KiB = 1024


class TestHttpPullLimitedStorage(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        with PortManager() as net:
            global AGENT_SERVER_PORT
            AGENT_SERVER_PORT = net.get_port()

            total_limit = ''

            if request.function.__name__ == 'test_solomon_pull_limited_storage':
                # So every shard has as much memory as it needs
                total_limit = '100KiB'
            elif request.function.__name__ == 'test_solomon_pull_limited_storage_total_lt_shards_sum':
                # Total usage will limit shard constraints
                total_limit = '20KiB'
            else:
                raise RuntimeError('unknown function')

            modules_conf = Parse(HTTP_PUSH_CONF % {'port': AGENT_SERVER_PORT}, TAgentModuleConfig())
            storage_conf = Parse(STORAGE_CONF % {'total_limit': total_limit}, TStorageConfig())

            self.endpoints = {
                service: ('http://127.0.0.1:' + str(AGENT_SERVER_PORT) + '/' + service)
                for service in SERVICES
            }

            self.endpoints.update({
                'misc': 'http://127.0.0.1:' + str(AGENT_SERVER_PORT) + '/misc'
            })

            TestHttpPullLimitedStorage.CONF_OVERRIDES = {
                "Modules": [modules_conf],
                "Storage": storage_conf,
            }

    def check_data_is_saved(self, agent):
        for service in SERVICES:
            misc.send_metrics(self.endpoints[service], INIT_METRICS)

            stored_metrics = agent.read(params={'project': 'solomon', 'service': service})
            assert 'sensors' in stored_metrics

            misc.remove_ts_from_data(stored_metrics)
            assert misc.are_metrics_equal(stored_metrics, INIT_METRICS)

    def check_never_goes_over_limit(self, agent):
        # At this point, exactly one chunk of data (and exactly one metric value) should be in every shard
        num_of_chunks = {
            service: 0
            for service in SERVICES
        }

        # Right now it's the same for every shard, but could differ in the future
        # if different shard types will be supported
        reserved_bytes = {
            service: 0
            for service in SERVICES
        }

        # Every chunk in this test consists of exactly one metric value. So every chunk is same in size
        chunk_size = 0

        agent_info_before = agent.management_request_json('/counters/json', None)

        for service in SERVICES:
            bytes_written = 0
            # Bytes that are allocated for a Storage object
            storage_size = 0

            for metric in agent_info_before['sensors']:
                if (metric['labels']['sensor'] == 'storage.bytesWritten' and
                        metric['labels']['storageShard'] == 'solomon/' + service):
                    bytes_written = metric['value']

                if (metric['labels']['sensor'] == 'storage.sizeBytes' and
                        metric['labels']['storageShard'] == 'solomon/' + service):
                    storage_size = metric['value']

            assert bytes_written > 0, service
            assert storage_size > 0, service

            if chunk_size == 0:
                chunk_size = bytes_written

            reserved_bytes[service] = storage_size - chunk_size
            actual_capacity = SERVICES_LIMITS[service] - reserved_bytes[service]
            assert actual_capacity > 0, service
            num_of_chunks[service] = actual_capacity / chunk_size

        for service in SERVICES:
            for i in range(num_of_chunks[service]):
                misc.send_metrics(self.endpoints[service], create_metrics(i))

        agent_info_after = agent.management_request_json('/counters/json', None)

        # At this point, one chunk should've already been evicted. Checking that resulting size <= limit
        for service in SERVICES:
            storage_size = 0

            for metric in agent_info_after['sensors']:
                if (metric['labels']['sensor'] == 'storage.sizeBytes' and
                        metric['labels']['storageShard'] == 'solomon/' + service):
                    storage_size = metric['value']

            assert 0 < storage_size <= SERVICES_LIMITS[service], service
            # It is greater, not equal to, for cases when capacity() of metrics collection is greater than size()
            assert storage_size >= num_of_chunks[service] * chunk_size + reserved_bytes[service], service

        assert len(num_of_chunks.keys()) == len(SERVICES)
        for service, chunks_num in num_of_chunks.items():
            assert chunks_num > 0

        return num_of_chunks, chunk_size

    def check_evicted_and_rewritten(self, agent, num_of_chunks, chunk_size):
        # At this point, exactly one chunk should've already been evicted and rewritten with a new one
        agent_info_before = agent.management_request_json('/counters/json', None)
        for service in SERVICES:
            bytes_evicted = 0
            for metric in agent_info_before['sensors']:
                if (metric['labels']['sensor'] == 'storage.bytesEvicted' and
                        metric['labels']['storageShard'] == 'solomon/' + service):
                    bytes_evicted = metric['value']

            assert bytes_evicted > 0, service
            assert bytes_evicted == chunk_size, service

        for service in SERVICES:
            first_value = 0
            last_value = num_of_chunks[service] - 1

            data_before = agent.read(params={'project': 'solomon', 'service': service})
            for metric in data_before['sensors']:
                assert len(metric['timeseries']) == num_of_chunks[service], service
                # Values: [0, ..., num_of_chunks - 1]
                assert metric['timeseries'][0]['value'] == first_value, service
                assert metric['timeseries'][-1]['value'] == last_value, service

            num_of_additional_chunks = 10
            for i in range(num_of_additional_chunks):
                misc.send_metrics(self.endpoints[service], create_metrics(num_of_chunks[service] + i))

            data_after = agent.read(params={'project': 'solomon', 'service': service})
            for metric in data_after['sensors']:
                assert len(metric['timeseries']) == num_of_chunks[service], service
                assert metric['timeseries'][0]['value'] == first_value + num_of_additional_chunks, service
                assert metric['timeseries'][-1]['value'] == last_value + num_of_additional_chunks, service

    def check_client_can_read_data_after_eviction(self, agent, num_of_chunks):
        for service in SERVICES:
            _, headers = agent.read_raw(params={'project': 'solomon', 'service': service})
            _, headers = agent.read_raw(params={'project': 'solomon', 'service': service},
                                        headers={FETCHER_HEADER: FETCHER_ID,
                                                 SEQ_NO_HEADER: headers[NEXT_SEQ_NO_HEADER]})
            next_seq_no = headers[NEXT_SEQ_NO_HEADER]

            new_value_start = 2000
            for i in range(num_of_chunks[service]):
                misc.send_metrics(self.endpoints[service], create_metrics(new_value_start + i))

            # read new data with old next_seq_no
            data, headers = agent.read_raw(params={'project': 'solomon', 'service': service},
                                           headers={FETCHER_HEADER: FETCHER_ID,
                                                    SEQ_NO_HEADER: next_seq_no})
            assert int(headers[NEXT_SEQ_NO_HEADER]) > int(next_seq_no), 'SeqNo has dropped'

            # check that read data is new and the number of metrics is correct
            assert 'sensors' in data
            assert len(data['sensors']) == 1
            assert len(data['sensors'][0]['timeseries']) == num_of_chunks[service]
            value_start = new_value_start
            for i, obj in enumerate(data['sensors'][0]['timeseries']):
                assert obj['value'] == value_start + i

    def test_solomon_pull_limited_storage(self, agent):
        time.sleep(2)

        self.check_data_is_saved(agent)
        num_of_chunks, chunk_size = self.check_never_goes_over_limit(agent)
        self.check_evicted_and_rewritten(agent, num_of_chunks, chunk_size)
        self.check_client_can_read_data_after_eviction(agent, num_of_chunks)

    def test_solomon_pull_limited_storage_total_lt_shards_sum(self, agent):
        """
        The case when there are several shards such that sum(shard_size) > Total
        """
        time.sleep(2)

        misc.send_metrics(self.endpoints['misc'], INIT_METRICS)

        agent_info = agent.management_request_json('/counters/json', None)
        for metric in agent_info['sensors']:
            if (metric['labels']['sensor'] == 'storage.bytesWritten' and
                    metric['labels']['storageShard'] == 'solomon/misc'):
                one_chunk_size = metric['value']

        assert one_chunk_size > 0

        num1 = 5 * KiB / one_chunk_size
        num2 = 12 * KiB / one_chunk_size

        for i in range(num1):
            misc.send_metrics(self.endpoints['small_limit'], create_metrics(i))

        for i in range(num2):
            misc.send_metrics(self.endpoints['big_limit'], create_metrics(i))

        # Now the size of shard1 + the size of shard2 is 5 + 12 == 17KiB < 20KiB. So everything is stored without loss
        num_of_values = {}

        stored_metrics = agent.read(params={'project': 'solomon', 'service': 'small_limit'})
        assert 'sensors' in stored_metrics

        values = stored_metrics['sensors'][0]['timeseries']
        num_of_values['small_limit'] = len(values)

        assert len(values) > 0
        assert int(values[0]['value']) == 0
        assert int(values[-1]['value']) == len(values) - 1

        stored_metrics = agent.read(params={'project': 'solomon', 'service': 'big_limit'})
        assert 'sensors' in stored_metrics

        values = stored_metrics['sensors'][0]['timeseries']
        num_of_values['big_limit'] = len(values)

        assert len(values) > 0
        assert int(values[0]['value']) == 0
        assert int(values[-1]['value']) == len(values) - 1

        # Sending additional 5KiB of data to shard1. Now it can only store 3 additional KiB,
        # since 17KiB of storage is already taken. So it will evict old data
        for i in range(num_of_values['small_limit'], num_of_values['small_limit'] + num1):
            misc.send_metrics(self.endpoints['small_limit'], create_metrics(i))

        # The same goes for shard 2
        for i in range(num_of_values['big_limit'], num_of_values['big_limit'] + num2):
            misc.send_metrics(self.endpoints['big_limit'], create_metrics(i))

        # Now:
        # 1. Some of old values should be evicted from every shard to acquire some free space
        # 2. For every shard its size should not be greater than its limit

        # 1
        stored_metrics = agent.read(params={'project': 'solomon', 'service': 'small_limit'})
        assert 'sensors' in stored_metrics

        values = stored_metrics['sensors'][0]['timeseries']
        assert len(values) > 0
        assert len(values) >= num_of_values['small_limit']
        assert int(values[0]['value']) > 0  # The very first value was evicted

        stored_metrics = agent.read(params={'project': 'solomon', 'service': 'big_limit'})
        assert 'sensors' in stored_metrics

        values = stored_metrics['sensors'][0]['timeseries']
        assert len(values) > 0
        assert len(values) >= num_of_values['big_limit']
        assert int(values[0]['value']) > 0  # The very first value was evicted

        # 2
        agent_info = agent.management_request_json('/counters/json', None)
        for metric in agent_info['sensors']:
            if (metric['labels']['sensor'] == 'storage.sizeBytes' and
                    metric['labels']['storageShard'] == 'solomon/small_limit'):
                small_shard_size = metric['value']
            if (metric['labels']['sensor'] == 'storage.sizeBytes' and
                    metric['labels']['storageShard'] == 'solomon/big_limit'):
                big_shard_size = metric['value']
            if (metric['labels']['sensor'] == 'storage.sizeBytes' and
                    metric['labels']['storageShard'] == 'total'):
                total_size = metric['value']

        assert small_shard_size > one_chunk_size
        assert big_shard_size > one_chunk_size

        assert 5 * KiB < small_shard_size <= 10 * KiB
        assert 12 * KiB < big_shard_size < 20 * KiB

        assert small_shard_size + big_shard_size <= total_size

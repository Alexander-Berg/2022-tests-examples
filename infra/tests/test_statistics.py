import json
import logging
import pytest
import re
import time

import dns.edns
import dns.exception
import dns.message
import dns.query

import helpers


class ProjectOutboundStats(object):
    def __init__(self, output):
        self.raw_data = {}
        self.requests = {}

        self._requests_lines = 0
        for line in output.split():
            key, value = line.split('=')
            try:
                value = int(value)
            except ValueError:
                pass
            self.raw_data[key] = value

            stat_key = self._parse_requests_stat_key(key)
            if stat_key:
                self._requests_lines += 1
                prj, authip, nsname = stat_key
                assert authip not in self.requests.setdefault(prj, {})
                self.requests[prj][authip] = value
                continue

        assert self._requests_lines == self.entries_number

    def _parse_requests_stat_key(self, key):
        r = re.fullmatch(
            r'total.prj.([a-f\d]+).authip.([\.:\d]+).nsname.([a-z\d\.]+)',
            key)
        if r is None:
            return None
        prj, authip, nsname = int(r[1], 16), r[2], r[3]
        return prj, authip, nsname

    @property
    def entries_number(self):
        return self.raw_data['total.entries.number']

    @property
    def lowest_timestamp(self):
        return self.raw_data['total.entries.lowest_timestamp']

    def get(self, key, default=None):
        return self.raw_data.get(key, default)

    def get_requests_number(self, project_id, auth_ip):
        return self.requests.get(project_id, {}).get(auth_ip, 0)

    def __len__(self):
        return len(self.raw_data)


@pytest.mark.parametrize("project_limiter_enable", [True, False])
@pytest.mark.usefixtures("unbound_env")
class TestProjectOutboundStatistics(object):
    # Unbound params
    PROJECT_ID = 0x10eac9a

    MAX_ENTRIES_NUM = 3

    AUTH_IPS = {
        f"{idx}.yandex.net": f"127.0.0.{idx}"
        for idx in range(MAX_ENTRIES_NUM * 2)
    }

    UNBOUND_CONFIG = {
        "server": {
            "project-limiter-enable": True,
            "project-limiter-max-entries-num": MAX_ENTRIES_NUM,
            "do-not-query-localhost": False,  # allows to set localhost in stub-addr
            "outbound-msg-retry": 1,  # it is too long to wait 5 (default) useless retries
        },
        "stub-zone": [
            {
                "name": zone,
                "stub-addr": f"{auth_ip}@12345",  # fake address, nothing is running there
                "stub-no-cache": True,  # do not use cache, send query to auth server every time
            } for zone, auth_ip in AUTH_IPS.items()
        ],
    }

    @classmethod
    @pytest.fixture(scope='function', autouse=True)
    def setup(cls, project_limiter_enable):
        cls.UNBOUND_CONFIG["server"]["project-limiter-enable"] = project_limiter_enable

    @pytest.mark.parametrize("use_subnet_client", [False, True])
    def test_statistics(self, unbound_env, project_limiter_enable, use_subnet_client):
        unbound_environment, _ = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        zone = next(iter(self.AUTH_IPS.keys()))

        if use_subnet_client:
            # Set client ip via EDNS subnet client.
            # Firstly, statistics module is looking for ip in ECS (if set).
            # If not found, it uses src ip.
            project_id = self.PROJECT_ID
            ecs = dns.edns.ECSOption(
                helpers.substitute_project_id("2a02:6b8:c08:3620:0:0:0:3b52", project_id),
                srclen=96)
            query_options = [ecs]
        else:
            project_id = 0  # client has ::1 ip address
            query_options = None

        q = dns.message.make_query(f"abcd.{zone}", "AAAA", options=query_options)
        logging.info(f"Query:\n{q.to_text()}")

        query_time = time.time()
        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        stats = ProjectOutboundStats(unbound_instance.unbound_control.prj_outbound_stats_noreset())
        logging.info(f"prj_outbound_stats:\n{json.dumps(stats.raw_data, indent=2)}")

        if project_limiter_enable:
            # There are 1 entry that corresponds our query
            assert len(stats) == 3
            assert stats.entries_number == 1
            assert int(query_time) <= stats.lowest_timestamp < time.time()
            assert stats.get_requests_number(project_id=project_id, auth_ip=self.AUTH_IPS[zone]) > 0
        else:
            # There are no entries
            assert len(stats) == 2
            assert stats.entries_number == 0
            assert stats.lowest_timestamp > time.time()

    def test_limit(self, unbound_env, project_limiter_enable):
        if not project_limiter_enable:
            pytest.skip()

        unbound_environment, _ = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        query_infos = []
        project_id = self.PROJECT_ID
        assert len(self.AUTH_IPS) > self.MAX_ENTRIES_NUM
        for zone, auth_ip in self.AUTH_IPS.items():
            project_id += 1
            ecs = dns.edns.ECSOption(
                helpers.substitute_project_id("2a02:6b8:c08:3620:0:0:0:3b52", project_id),
                srclen=96)

            q = dns.message.make_query(f"abcd.{zone}", "AAAA", options=[ecs])
            logging.info(f"Query:\n{q.to_text()}")

            query_time = time.time()
            try:
                # set timeout to speed up tests: we do not actually need an answer
                r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port, timeout=5)
                logging.info(f"Response:\n{r.to_text()}")
            except dns.exception.Timeout:
                logging.info("Query timed out")

            stats = ProjectOutboundStats(unbound_instance.unbound_control.prj_outbound_stats_noreset())
            logging.info(f"prj_outbound_stats:\n{json.dumps(stats.raw_data, indent=2)}")

            query_infos.append({
                'auth_ip': auth_ip,
                'project_id': project_id,
                'query_time': query_time,
            })

            expected_entries_num = min(self.MAX_ENTRIES_NUM, len(query_infos))

            assert len(stats) == 2 + expected_entries_num
            assert stats.entries_number == expected_entries_num
            assert int(query_infos[-expected_entries_num]['query_time']) <= stats.lowest_timestamp < time.time()
            for query_info in query_infos[-expected_entries_num:]:
                assert stats.get_requests_number(project_id=query_info['project_id'], auth_ip=query_info['auth_ip']) > 0

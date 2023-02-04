import itertools
import pytest

from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for
from maps.infra.ratelimiter2.tools.pyhelpers.make_counters import \
    client_id_hash, LimitSpec, LimitInfo


def test_agents_sync_through_server(run_nova_agents, run_server) -> None:
    """ Increments on different agents synchronized through server """
    INCREMENTS = 10
    resources = [f'res{r}' for r in range(2)]
    clients = [f'cli{c}' for c in range(2)]
    with run_server() as server:
        # NB: rate=0 to disable leaky bucket
        limit_info = LimitInfo(LimitSpec(unit=1, rate=0, gen=1), burst=100)
        with run_nova_agents(server, n=2) as agents:
            for i in range(INCREMENTS):
                for resource, client in itertools.product(resources, clients):
                    for agent in agents:
                        assert agent.rate_access(client_id=client,
                                                 resource=resource,
                                                 limit_info=limit_info)

            wait_for(timings.counter_synchronization_interval)

            expected_counters = {
                res: {client_id_hash(c): len(agents)*INCREMENTS for c in clients}
                for res in resources
            }
            expected_limits = {
                res: {client_id_hash(c): limit_info.spec for c in clients}
                for res in resources
            }
            # Expect same state on both agents and server
            assert agents[0].state() == (expected_counters, expected_limits)
            assert agents[1].state() == (expected_counters, expected_limits)
            server_state = server.current_counters_v2(resources=resources)
            assert server_state == (expected_counters, expected_limits)


def test_limits_version_split(run_nova_agents, run_server) -> None:
    """ Limits update is late for some agents """
    with run_server() as server:
        with run_nova_agents(server, n=2) as agents:
            agent_a, agent_b = agents
            # agent A has version=2 limits for 'res1'
            limit_x2 = LimitSpec(unit=1, rate=10, gen=2)
            limit_y2 = LimitSpec(unit=1, rate=20, gen=2)
            # clients X and Y access on agent A
            assert agent_a.rate_access(client_id='X', resource='res1',
                                       limit_info=LimitInfo(limit_x2, burst=100))
            assert agent_a.rate_access(client_id='Y', resource='res1',
                                       limit_info=LimitInfo(limit_y2, burst=100))

            # agent B has version=1 limits
            limit_x1 = LimitSpec(unit=1, rate=5, gen=1)
            limit_y1 = LimitSpec(unit=1, rate=15, gen=1)
            # client X access on agent B (but no client Y)
            assert agent_b.rate_access(client_id='X', resource='res1',
                                       limit_info=LimitInfo(limit_x1, burst=50))

            wait_for(timings.counter_synchronization_interval)
            _, a_limits = agent_a.state()
            assert a_limits == {
                'res1' : {  # Expect both limits version=2
                    client_id_hash('X'): limit_x2,
                    client_id_hash('Y'): limit_y2
                }
            }
            _, b_limits = agent_b.state()
            assert b_limits == {
                'res1' : {  # Expect version=1 for X, but version=2 for Y (got from server)
                    client_id_hash('X'): limit_x1,
                    client_id_hash('Y'): limit_y2
                }
            }

            # X access on agent B
            assert agent_b.rate_access(client_id='X', resource='res1',
                                       limit_info=LimitInfo(limit_x1, burst=50))
            # Y access on agent B (for the first time)
            # Limit downgrade allowed (counter has shard==0)
            assert agent_b.rate_access(client_id='Y', resource='res1',
                                       limit_info=LimitInfo(limit_y1, burst=50))
            _, b_limits = agent_b.state()
            assert b_limits == {
                'res1' : {
                    client_id_hash('X'): limit_x1,
                    client_id_hash('Y'): limit_y1  # NB: downgraded to version=1
                }
            }
            # Limit downgrade fails for X on agent A (counter has shard!=0)
            with pytest.raises(Exception, match='Got 500 from server'):
                agent_a.rate_access(client_id='X', resource='res1',
                                    limit_info=LimitInfo(limit_x1, burst=100))

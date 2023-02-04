import os
import signal
import time
from configs import ReloadConfig, WithPingerConfig
from balancer.test.util import process


def test_workers_kill_themselves_when_master_dies(ctx):
    """
    BALANCER-1980
    Если мастер умер - весь балансер должен умирать вместе с ним, включая workerы.
    """
    ctx.start_balancer(ReloadConfig(workers=5, response='aaa'))
    time.sleep(5)
    master = ctx.balancer.get_master_pid()
    workers = ctx.balancer.get_workers()
    os.kill(master, signal.SIGKILL)
    time.sleep(70)
    aliveWorkersCount = 0
    for pid in workers:
        try:
            os.kill(pid, 0)
            aliveWorkersCount += 1
        except:
            pass
    assert aliveWorkersCount == 0
    assert len(process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)) == 0
    assert not ctx.balancer.is_alive()
    ctx.balancer.set_finished()


def test_pingers_kill_themselves_when_master_dies(ctx):
    ctx.start_balancer(WithPingerConfig(workers=1, response='aaa'), debug=True)
    time.sleep(2)
    master = ctx.balancer.get_master_pid()
    children = process.get_children(master, ctx.logger, recursive=False)
    os.kill(master, signal.SIGKILL)
    time.sleep(70)
    aliveWorkersCount = 0
    for pid in children:
        try:
            os.kill(pid, 0)
            aliveWorkersCount += 1
        except:
            pass
    assert aliveWorkersCount == 0
    assert len(process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)) == 0
    assert not ctx.balancer.is_alive()
    ctx.balancer.set_finished()

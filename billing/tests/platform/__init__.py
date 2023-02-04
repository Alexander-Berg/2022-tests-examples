import datetime

from agency_rewards.rewards.utils.bunker import BunkerCalc


def create_bunker(
    cfg: dict, env: str = 'dev', node: str = 'testing', node_path: str = '/z/y/x', insert_dt=None
) -> BunkerCalc:
    if not insert_dt:
        insert_dt = datetime.datetime.now()
    cfg.update({'calendar': 'f', 'comm_type': ['7']})
    return BunkerCalc(cfg, env, insert_dt, node, node_path)

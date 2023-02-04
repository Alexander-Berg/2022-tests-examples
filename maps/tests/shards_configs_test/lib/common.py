from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Union
import xmltodict


@dataclass
class StagingConf:
    shards: int
    pods_in_shard: int
    osm_shards: int
    osm_pods_in_shard: int


class Staging:
    STABLE = StagingConf(16, 3, 0, 0)
    TESTING = StagingConf(8, 2, 1, 2)


def get_dict_from_xml(file: str) -> Dict[str, Any]:
    with open(file, 'r') as f:
        data = f.read()
        return xmltodict.parse(data)


def check_targets(
    targets: Union[dict, List[Dict[str, Any]]],
    shards: int,
    pods_in_shard: int,
    osm_shards: int,
    osm_pods_in_shard: int,
    allowed_classes: Optional[List[Optional[str]]] = None,
) -> None:
    if not isinstance(targets, list):
        return

    def check_group(targets: Union[dict, List[Dict[str, Any]]], shards: int, pods_in_shard: int) -> None:
        pods_used_in_shards = set()
        api_url = None

        assert len(targets) == shards, (
            f'Shards count not equal: {len(targets)} != {shards};',
            f'Look for the error around this lines: {targets[0]}'
        )

        for target in targets:
            nodes = target['node']
            assert len(nodes) == pods_in_shard, f'Some node in `{api_url}` has {len(nodes)} pods instead of {pods_in_shard}'
            if isinstance(nodes[0], dict):
                nodes = [node['#text'] for node in nodes]
            assert len(set(nodes)) == pods_in_shard, f'Config has equal nodes in one target: {nodes}'
            for pod_url in nodes:
                pod, current_api_url = pod_url.split('/')
                if api_url is None:
                    api_url = current_api_url
                assert api_url == current_api_url
                assert pod not in pods_used_in_shards, f'Pod {pod} was used in few shards for `{api_url}`'
                pods_used_in_shards.add(pod)

        assert len(pods_used_in_shards) == shards * pods_in_shard, 'Not all pods were used or some pod was used twice'

    if allowed_classes:
        class_targets = [target for target in targets if target.get('@class') in allowed_classes and target.get('@group') != 'osm']
        osm_targets = [target for target in targets if target.get('@class') in allowed_classes and target.get('@group') == 'osm']
        check_group(class_targets, shards, pods_in_shard)
        check_group(osm_targets, osm_shards, osm_pods_in_shard)
    else:
        check_group(targets, shards, pods_in_shard)

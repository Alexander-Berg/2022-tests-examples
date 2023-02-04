from mapreduce.yt.python.recipe_multi_cluster.lib import yt_cluster_factory
from yt.recipe.multi_cluster.lib import recipe as yt_recipe
from yt.recipe.multi_cluster.lib import get_yt_cluster

from library.python.testing import recipe

import yatest.common

import argparse


def _split(s):
    return [x.strip() for x in s.split(",")] if s else []


def start(args):
    parser = argparse.ArgumentParser()
    parser.add_argument("-G", "--acl-groups", type=_split, required=False, default=[], help="YT ACL groups, if needed")
    parsed_args, _ = parser.parse_known_args(args)

    # start cluster replicas
    # intentially select workdir in test-output, to avoid using ramdisk
    work_dir = yatest.common.output_path()
    yt_recipe.start(yt_cluster_factory, args, work_dir=work_dir)

    # add common acl groups to all clusters if needed
    if parsed_args.acl_groups:
        clusters = get_yt_cluster()
        for index in range(len(clusters)):
            yt_client = clusters[index].get_yt_client()
            yt_client.set("//sys/accounts/tmp/@resource_limits/tablet_count", 100500)
            for group in parsed_args.acl_groups:
                yt_client.create("group", attributes={"name": group})


def stop(args):
    yt_recipe.stop(args)


if __name__ == "__main__":
    recipe.declare_recipe(start, stop)

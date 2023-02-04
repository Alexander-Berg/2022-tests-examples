import argparse
import json
import os

from ads.bsyeti.caesar.tools.yt_sync.commands import tables as table_commands
from ads.bsyeti.caesar.tools.yt_sync.settings import tables as table_settings

from yt.recipe.multi_cluster.lib import get_yt_cluster

from library.python.testing import recipe


def _create_tables(clusters, args):
    """create model tables and save result to env variables and local file."""

    args.stand = "test"
    args.dry_run = False
    args.clean = True

    tables = {}
    for table in args.tables:
        path = "//test/%s" % table

        args.name = table
        args.path = path

        settings = args.tables_desc[table][args.stand]
        settings["master"]["cluster"] = clusters.primary_cluster.proxy_address
        settings["sync_replicas"]["cluster"] = ["%s#%s" % (x.yt_id, x.proxy_address) for x in clusters.replica_clusters]

        table_commands.ensure_table(args)

        recipe.set_env("YT_TABLE_%s" % table.upper(), path)
        tables[table] = path

    with open(os.path.join(recipe.ya.output_dir, "yt_table_info.json"), "w") as fd:
        json.dump(tables, fd)


def split(s):
    return [x.strip() for x in s.split(",")] if s else []


def start(args):
    """recipe entry point (start services)."""
    parser = argparse.ArgumentParser()
    table_commands.add_arguments(parser, table_settings.TABLES)
    parsed_args, _ = parser.parse_known_args(args)
    cluster = get_yt_cluster()
    if parsed_args.name == "all":
        parsed_args.tables = sorted(table_settings.TABLES)
    else:
        parsed_args.tables = [parsed_args.name]

    _create_tables(cluster, parsed_args)


def stop(_):
    pass


if __name__ == "__main__":
    recipe.declare_recipe(start, stop)

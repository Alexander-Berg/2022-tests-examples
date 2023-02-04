import argparse
import copy

from ads.bsyeti.caesar.tools.yt_sync.commands import tables as table_commands
from ads.bsyeti.caesar.tools.yt_sync.settings import tables as table_settings


def create_profile_table(yt_cluster, table, path, enable_balancer=False):

    # by default balancer are disabled in test tables,
    if enable_balancer:
        tables = {table: copy.deepcopy(table_settings.TABLES[table])}
        settings = tables[table]["test"]
        settings["master"]["attributes"].pop("tablet_balancer_config", None)
        settings["sync_replicas"]["attributes"].pop("tablet_balancer_config", None)
    else:
        tables = table_settings.TABLES

    settings = tables[table]["test"]
    settings["master"]["cluster"] = yt_cluster.primary_cluster.proxy_address
    settings["sync_replicas"]["cluster"] = ["%s#%s" % (x.yt_id, x.proxy_address) for x in yt_cluster.replica_clusters]

    # prepare arguments for table command
    parser = argparse.ArgumentParser()
    table_commands.add_arguments(parser, tables)
    options, _ = parser.parse_known_args(["-N", table, "-P", path])

    # set common options
    options.stand = "test"
    options.dry_run = False
    options.clean = True
    table_commands.ensure_table(options)

#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';

use FindBin;

use lib "$FindBin::Bin/../lib";
use lib "$FindBin::Bin/../wlib";

use Project;
use Cmds::DynBanners;

test_dyntable_proxy_bl_tasks_info();
exit(0);

sub test_dyntable_proxy_bl_tasks_info {
    my $proj = Project->new();
    for my $task_type ('perf', 'dyn') {

        my $yt_table = Cmds::DynBanners::get_tasks_info_yt_tablepath($task_type);

        my $res = $proj->dt_proxy_client->do_select(
            table => $yt_table,
            fields => [ 'value', 'pocket', 'update_time' ],
            limit => 1,
            condition => 'True',
        );

        die "result not found for task_type=$task_type" if !$res or !$res->[0];
    }
}

#!/usr/bin/perl

use strict;
use warnings;
use autodie;

use FindBin;
use lib "$FindBin::Bin/../lib";

use Utils::Common;
use Utils::Hosts qw(get_hosts get_curr_host);
use Utils::Sys qw(
    get_file_lock
    handle_errors
);
use Dates;
use Project;
use BM::SolomonClient;

my $ZFS_CMD = '/sbin/zfs';
my $ZFS_POOL = 'pool0/mysql';
my $SSH_CMD = 'ssh -o ConnectTimeout=30 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null';


get_file_lock("update_test_db") or die "another script already running";
handle_errors(DIE => {stack_trace => 1});

main();
exit(0);


# для каждого дня месяца возвращает true для одного хоста, для остальных false
# идея в том, чтобы никогда не запускать этот скрипт в один и тот же день на разных хостах
sub check_date {
    my $curr_host = shift // get_curr_host();
    my $all_hosts = shift // [ get_hosts(role => "catalogia-media-gen-test") ];

    my $host_no = undef;
    for my $i (0 .. $#$all_hosts) {
        if ($all_hosts->[$i] eq $curr_host) {
            $host_no = $i;
            last;
        }
    }

    die "Host '$curr_host' is not one of " . join(", ", map { "'$_'" } @$all_hosts ) unless defined $host_no;

    my $dates = new Dates;
    my $day_since_ =  $dates->delta_days('20190807',$dates->cur_date('direct'),'direct');
    return $day_since_ % scalar(@$all_hosts) == $host_no;
}

sub get_mysql_slaves {
    my $proj = shift;

    my @hosts = get_hosts(role => "catalogia-media-gen");
    my $master = $proj->catalogia_media_dbh->List_SQL('select @@hostname as Master')->[0]->{Master} or die "can't find master";
    
    return grep { $_ !~ m/^$master/ && $master !~ m/^$_/ } @hosts;
}

sub get_snapshots {
    my $proj = shift;
    my $host = shift // die "provide host";

    my $data = $proj->read_sys_cmd_bash_remote($host, "zfsctl list");
    my @snapshots = $data =~ m{($ZFS_POOL\@\d+_\d+)}gs;

    return @snapshots;
}

sub get_latest_snapshot {
    my $proj = shift;
    my $host = shift // die "provide host";

    my @snapshots = sort { $b cmp $a } get_snapshots($proj, $host);
    
    return shift @snapshots;
}

sub start_mysql {
    my $proj = shift;
    my $attempts = shift // 20;
    my $timeout = shift // 60;
    
    for (0 .. $attempts) {
        eval { $proj->do_sys_cmd("sudo /etc/init.d/mysql.catalogia-media start") };
        return unless $@;
        
        sleep $timeout;
    }

    die "could not start mysql";
}

sub stop_mysql {
    my $proj = shift;
    my $attempts = shift // 20;
    my $timeout = shift // 60;
    
    for (0 .. $attempts) {
        eval { $proj->do_sys_cmd("sudo /etc/init.d/mysql.catalogia-media stop") };
        my $ps = eval { $proj->do_sys_cmd("ps aux | grep mysqld.catalogia-media | grep -v grep") };
        return unless $ps;

        sleep $timeout;
    }

    die "could not stop mysql";
}

sub main {
    my $proj = shift // Project->new();

    $proj->log("Started");
    
    $proj->log("Check date...");
    unless (check_date()) {
        $proj->log("We will run this script at this host at some other day");
        return;
    }

    $proj->log("Choose host and snapshot...");
    
    my $curr_host = get_curr_host();

    my @slaves = get_mysql_slaves($proj);
    die "no slaves found" unless scalar @slaves;
    
    my ($chosen_slave, $latest_snapshot) = (undef, undef);
    for my $slave (@slaves) {
        my $snapshot = get_latest_snapshot($proj, $slave);
        if (
            $snapshot
            && (
                !defined($latest_snapshot)
                || $snapshot gt $latest_snapshot
            )
        ) {
            ($chosen_slave, $latest_snapshot) = ($slave, $snapshot);
        }
    }

    die "no snapshots found on slaves" unless $latest_snapshot;
    
    $proj->log("Will bring snapshot '$latest_snapshot' from '$chosen_slave' to '$curr_host'");
    

    $proj->log("Stop mysql...");
    stop_mysql($proj);
    $proj->log("Mysql stopped");


    $proj->log("Destroy local snapshots...");
    my @local_snapshots = get_snapshots($proj, $curr_host);
    for my $snapshot (@local_snapshots) {
        $proj->do_sys_cmd("sudo $ZFS_CMD destroy $snapshot");
    }
    

    $proj->log("Bring remote snapshot...");
    $proj->do_sys_cmd(qq{$SSH_CMD $chosen_slave 'sudo $ZFS_CMD send -v $latest_snapshot | $SSH_CMD $curr_host sudo $ZFS_CMD recv -F $ZFS_POOL'});
    

    $proj->log("Chown files...");
    $proj->do_sys_cmd("sudo chown -R mysql:mysql /opt/mysql.catalogia-media");

    
    $proj->log("Start mysql...");
    start_mysql($proj);
    $proj->log("Mysql started");
    utime(undef,undef,$Utils::Common::options->{Monitor}{update_test_db_mark_file})
        or $proj->do_sys_cmd("touch ".$Utils::Common::options->{Monitor}{update_test_db_mark_file});
    BM::SolomonClient->new()->set_success_script_finish("update_test_db");
    $proj->log("All done.");
}

#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

use Time::HiRes qw(gettimeofday tv_interval);
use Getopt::Long;
use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../lib";
#use lib "/opt/broadmatching/scripts/lib";

use Project;
use Utils::Common;
use JSON qw(from_json to_json);
use Utils::Sys qw(
    handle_errors
);
use QTests;


select STDERR; $|++;
select STDOUT; $|++;

handle_errors();
#handle_errors(DIE => {stack_trace => 1});

my %opt = (
);
GetOptions(
    \%opt,
    'help',
    'light',
);

if ($opt{help}) {
    print join("\n",
        "Usage: run-qtest.pl QTEST_NAMES [OPTIONS]",
        "QTEST_NAMES - comma-separated list",
        "Options:",
        "  --light    Project without load_dicts",
        "Examples:",
        "  run-qtest.pl qtest_banners --light",
        "  run-qtest.pl qtest_banners,qtest_sigpipe --light",
    ), "\n";
    exit(0);
}

my $qtest_names = shift @ARGV
    or die "Void qtest_name!";

my $proj = Project->new({
    ($opt{light} ? () : (load_dicts => 1)),
});

my @failed;

for my $qtest_name (split /,/, $qtest_names) {

    $proj->log("run_qtest($qtest_name)");
    my $res = eval { QTests::run_qtest($proj, $qtest_name) } // { status => "dead" };
    print Dumper($res);
    unless(lc($res->{status}) eq 'ok') { # TODO lc - ?
        push @failed, $qtest_name;
    }

}

if (@failed) {
    $proj->log("Done. failed: @failed");
} else {
    $proj->log("Done.");
}

exit( @failed ? 1 : 0 );

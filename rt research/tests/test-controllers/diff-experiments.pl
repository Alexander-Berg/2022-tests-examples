#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use FindBin;
use lib "$FindBin::Bin/../../lib";

use Time::HiRes qw(gettimeofday);
use Project;

my ($n1, $n2) = @ARGV;
if(!defined($n1) || !defined($n2)) {
    die("usage: diff-experiments.pl N1 N2");
}

my $proj = Project->new({
    load_dicts => 1,
});

my $dir = $proj->test_controller->temp_dir;

$proj->test_controller->diff_experiments(
        sprintf("$dir/exp%02d", $n1),
        sprintf("$dir/exp%02d", $n2),
        sprintf("$dir/diff%02d%02d", $n1, $n2)
);


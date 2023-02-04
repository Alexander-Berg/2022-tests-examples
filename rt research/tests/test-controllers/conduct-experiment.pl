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

my $proj = Project->new({
    load_dicts => 1,
    load_counts => 1,
    load_minicategs => 1,
    load_simpgraphs => 1,
    load_simpgraphs_stat => 1,
    loadadvqlog => 1,
});

my $file_name;
for (my $i = 0; ; $i++) {
    $file_name = $proj->test_controller->temp_dir . sprintf("/exp%02d", $i);
    last unless -e $file_name;
}

$proj->test_controller->conduct_experiment($file_name);


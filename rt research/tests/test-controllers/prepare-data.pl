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

my $proj = Project->new({});

$proj->test_controller->prepare_chunk(1000);

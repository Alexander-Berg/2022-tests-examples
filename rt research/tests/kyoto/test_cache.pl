#!/usr/bin/perl -w
use strict;
use utf8;

use FindBin;
use lib "$FindBin::Bin/../../lib";
use BaseProject;

my $proj = BaseProject->new({});

my $cache_name = shift @ARGV;
if (! $cache_name) {
    die "ERROR: Invalid cache_name!\n"
}

my $client;
if ($cache_name eq "broad_kyoto" or $cache_name eq "ktclient") {
    $client = $proj->$cache_name;
} else {
    die "ERROR: Unknown cache_name '$cache_name'!\n";
}

my $rand_key_part = int(rand() * 10000);
my $checks_count = 10;
my $value = join("", 1 .. 100, time);

for my $i (1..$checks_count) {
    $client->set("check_cache_${rand_key_part}_${i}", $value, 600);
}

sleep 1;

for my $i (1..$checks_count) {
    my $res = $client->get("check_cache_${rand_key_part}_${i}");
    if ($res ne $value) {
        die "Fail cache result!\n";
    }
}

1;

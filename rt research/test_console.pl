#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw(gettimeofday tv_interval);
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Data::Dumper;

if(!$ARGV[0]) {
    die("usage: test_console.pl name");
}

my $proj = Project->new({ });
my $client = $proj->{$ARGV[0]};

if(!$client) {
    die("bad name '$ARGV[0]'");
}

# если файла с данными нет, выходим
unless(-e $client->index_file) {
    $proj->log("ERROR: no data file, do exit");
    exit(2);
}

# запуск сервера
$proj->do_sys_cmd(join " ",
    $client->server_dir."/cdict", 
    "--mode=console",
    "--norm-config=".$client->norm_config_file,
    "--data-file=".$client->index_file,
    "--dict-file=".$client->dict_file
);

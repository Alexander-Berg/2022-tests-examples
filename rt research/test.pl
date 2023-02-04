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
    die("usage: start.pl name");
}

my $proj = Project->new({ });
my $client = $proj->{$ARGV[0]};

if(!$client) {
    die("bad name '$ARGV[0]'");
}

while(defined(my $query = STDIN->getline)) {
    chomp $query;
    print "$query\n";
    my $resp = $client->exec_command_once($query);

    print "$resp\n" if $resp;

    last if !$client->is_connected || $resp =~ /^BYE/;
}

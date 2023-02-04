#!/usr/bin/perl -w
use strict;
use utf8;
no warnings 'utf8';

use Getopt::Long;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

my $proj = Project->new({load_dicts   => 0});

my @urls = qw~
http://eldorado.ru/cat/detail/71116447/?category=219154535&a=1
http://eldorado.ru/cat/detail/71116461/?category=219154535&a=2
http://eldorado.ru/cat/detail/71116448/?category=219154535&a=3
http://eldorado.ru/cat/detail/71116491/?category=219154535&a=4
~;

my $start = [gettimeofday];

my $zora_client = $proj->zora_client;
$zora_client->{use_kyoto_cache} = "";

my $result = $zora_client->multi_get_hashref( [ @urls ], {timeout => 100} );

for my $url ( keys %$result ) {
    my $content = $result->{$url}{content};
    print join("\t",
        "url:" . $url,
        "httpcode:" . $result->{$url}{httpcode},
    ) . "\n";
}
$proj->log("rate:" . int(scalar(@urls)/tv_interval($start) + 0.5) . " urls per sec");
$proj->log("zora done");

exit(0);

1;

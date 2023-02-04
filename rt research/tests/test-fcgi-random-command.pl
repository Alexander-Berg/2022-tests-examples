#!/usr/bin/perl -w

use strict;
use utf8;
use Data::Dumper;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN, ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw/gettimeofday tv_interval usleep/;
use POSIX ":sys_wait_h";

use LWP::UserAgent;

my $tstart = [gettimeofday];
my $data = join("\n",
    "холодильник sony KD-123",
    "телевизор панасоник",
    "ваз 2114",
    "холодильник",
);

$data .= "#END";

my $ua = LWP::UserAgent->new;

my $t1 = [gettimeofday];
my $res = $ua->post( 'http://bmapi-dev01e.yandex.ru/fcgi-bin/', ['act' => 'add_search_categs_minuswords', 'data' => $data] )->decoded_content();
print STDERR "done in " . tv_interval($t1) . " sec\n";

print "RES BEGIN\n$res" . "RES END\n";

exit(0);

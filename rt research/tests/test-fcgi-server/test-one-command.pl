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
'как называеться не русские свечки стоя торте это',
'как называеться необычное что-то',
'как называеться нескольких произведение равных сомножетелей',
'как называеться нескольких сочетание цветов',
'как называеться несколько парней про фильм',
'как называеться новыми обливион персонажами',
'как называеться норковая турецком шуба',
'как называеться нормалиный секс',
'как называеться ну пожалуйсто скажи это',
'как называеться о рональдо фильм',
);

$data .= "#END";

my $ua = LWP::UserAgent->new;

my $t1 = [gettimeofday];
print STDERR "started...\n";
my $res = $ua->post( 'http://bmapi-dev01e.yandex.ru/fcgi-bin/', ['cmd' => 'getcatids', 'data' => $data] )->decoded_content();
print STDERR "done in " . tv_interval($t1) . " sec\n";

print "RES BEGIN\n$res" . "RES END\n";

exit(0);

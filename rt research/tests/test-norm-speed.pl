#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

use Digest::MD5 qw(md5_hex);
use Time::HiRes qw(gettimeofday tv_interval);

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Utils::Sys;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

my $start;
my $tm;

$start = [ gettimeofday() ];
my $proj = Project->new({load_dicts => 1});
$tm = tv_interval($start);
printf "loading dicts: %.2f sec\n", $tm;
printf "mem usage: %.1f Mb\n", mem_usage($$) / 2**20;


$start = [ gettimeofday() ];
my @texts;
open my $fh, 'zcat tests/data/phrases-100k.gz |';
while(<$fh>) {
    chomp;
    push @texts, $_;
}
close $fh;
$tm = tv_interval($start);
my $N = @texts;
printf "phrases count: %d\n", $N;
printf "reading phrases: %.2f sec (rps: %d)\n", $tm, int($N / $tm);


$start = [ gettimeofday() ];
my @phrases = map { $proj->phrase($_) } @texts;
$tm = tv_interval($start);
printf "phrases objects: %.2f sec (rps: %d)\n\n", $tm, int($N / $tm);


$start = [ gettimeofday() ];
my @norm;
for my $phr (@phrases) {
    push @norm, $phr->norm_phr;
}
$tm = tv_interval($start);
printf "norm_phr: %.2f sec (rps: %d)\n", $tm, int($N / $tm);
printf "md5: %s\n\n", md5_hex(Encode::encode('UTF-8', join(',', @norm)));


# пересоздаем Phrase Object, чтобы убрать кэширование norm_phr
my @phrases2 = map { $proj->phrase($_) } @texts;
$start = [ gettimeofday() ];
my @snorm;
for my $phr (@phrases2) {
    push @snorm, $phr->snorm_phr;
}
$tm = tv_interval($start);
printf "snorm_phr: %.2f sec (rps: %d)\n", $tm, int($N / $tm);
printf "md5: %s\n\n", md5_hex(Encode::encode('UTF-8', join(',', @snorm)));

printf "done!\n"

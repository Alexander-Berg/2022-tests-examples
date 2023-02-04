#!/usr/bin/perl -w

use blib;
use StaticMap;
use Time::HiRes qw(gettimeofday tv_interval);

open F, "/home/broadmatching-skreling/lang-dicts/gen-normdicts/norm_dict_sorted";
my %h;
while (<F>) {
    chomp;
    my ($key, $val) = split "\t", $_;
    $h{$key} = $val;
}
close F;

my $q = new StaticMap("/home/broadmatching-skreling/lang-dicts/gen-normdicts/norm_dict_sorted");
my $test_count = 1000 * 1000;
my @arr = (1..$test_count);
my $s = [gettimeofday];
print $h{'собаку'} . "\n";
my $str;
for (@arr) {
    $str = $h{'собаку'};
}
print $test_count / tv_interval($s) . "\n";
print $q->Value("собаку") . "\n";
my $start = [gettimeofday];
for (@arr) {
    $str = $q->Value("собаку");
}
print $test_count / tv_interval($start) . "\n";

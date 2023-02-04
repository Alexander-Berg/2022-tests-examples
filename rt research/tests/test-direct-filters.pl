#!/usr/bin/perl -w
use strict;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Data::Dumper;
use BM::Filter;

# utf8
use utf8;
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
no warnings 'utf8';
# /utf8

print "we are here - 1\n";
my $proj = Project->new({load_dicts => 0});
print "we are here - 2\n";

#   { 'kind' => 'match', value => 'квартира', type => 'url' },

my $condition = [
   { 'kind' => 'exact', value => 'ZOPa.', type => 'domain' },
];

my $array = [
    { 'url' => 'Куплю квартиру в Истре', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'Продам квартиру в Истре', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'ZOPa.gaykoverty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'http://www.zopa.zopa.ru?a=111', 'domain' => 'kondicionery.vseinstrumenti.ru', title => 'TITLE' },
    { 'url' => 'ykoverty', 'domain' => 'new.kondicionery.vseinstrumenti.ru', title => 'TITLE' },
    { 'url' => 'overty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'verty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'rty', 'domain' => 'zzz.ru', title => 'TITLE' },
];



print "we are here - 3\n";
my $array_filtered = $proj->filter({ filter => BM::Filter::ext2bm( $condition ) } )->filter($array);
print "we are here - 4\n";

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

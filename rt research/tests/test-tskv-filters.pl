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

my $proj = Project->new({load_dicts => 0});

#   { 'kind' => 'match', value => 'квартира', type => 'url' },

my $condition = [
   { 'kind' => 'exact', value => 'zopa.', type => 'domain' },
];

my $array = [
    { 'url' => 'Куплю квартиру в Истре', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'Продам квартиру в Истре', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'gaykoverty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'http://www.zopa.zopa.ru?a=111', 'domain' => 'kondicionery.vseinstrumenti.ru', title => 'TITLE' },
    { 'url' => 'ykoverty', 'domain' => 'new.kondicionery.vseinstrumenti.ru', title => 'TITLE' },
    { 'url' => 'overty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'verty', 'domain' => 'zzz.ru', title => 'TITLE' },
    { 'url' => 'rty', 'domain' => 'zzz.ru', title => 'TITLE' },
];



my $array_filtered = $proj->filter({ filter => BM::Filter::ext2bm( $condition ) } )->filter($array);

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

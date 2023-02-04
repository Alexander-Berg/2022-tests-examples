#!/usr/bin/perl -w
use strict;
use FindBin;
use lib '/opt/broadmatching/scripts/lib';
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

my $proj = Project->new({load_dicts => 1});

my $filter_condition =         {
                      'url titlecontains' => [
                                                        'Настенный кондиционер Mitsubishi Heavy',
                                                      ]
                               };



my $array = [
    { name => 'zzz', url => 'http://www.ozonsport.ru/product_1526.html' },
    { name => 'zzz2', url => 'http://www.ozonsport.ru/product_310.html' },
    { name => 'zzz3', url => 'http://iclim.ru/nastennye-kondicionery-c-901_1.html?osCsid=bgoelqf3sk359jojvbr4bp71s0?filter_id=232' },
];

# приготовим исходный массив

my $array_filtered = $proj->filter({ filter => $filter_condition } )->filter($array);

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

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
'url NOT like' => [
                                          '/page_31.html',
                                          '/page_39.html',
                                          '/page_18.html',
                                          '/page_19.html',
                                          '/page_20.html',
                                          '/page_21.html',
                                          '/page_22.html',
                                          '/page_64.html'
                                        ],
                      'url normordertitlecontains' => [
                                                        'беговая'
                                                      ]
                               };



my $array = [
{ name => 'a1', url => 'http://www.ozonsport.ru/product_3594.html'},
{ name => 'a2', url => 'http://www.ozonsport.ru/product_4147.html'},
{ name => 'a3', url => 'http://www.ozonsport.ru/product_2088.html'},
{ name => 'a4', url => 'http://www.ozonsport.ru/product_1293.html'},
{ name => 'a5', url => 'http://www.ozonsport.ru/product_3934.html'},
{ name => 'a6', url => 'http://www.ozonsport.ru/product_3684.html'},
{ name => 'a7', url => 'http://www.ozonsport.ru/product_2580.html'},
{ name => 'a8', url => 'http://www.ozonsport.ru/product_2236.html'},
{ name => 'a9', url => 'http://www.ozonsport.ru/product_3247.html'},
{ name => 'a10', url => 'http://www.ozonsport.ru/product_3082.html'},
{ name => 'a11', url => 'http://www.ozonsport.ru/product_2240.html'},
];

# приготовим исходный массив

my $array_filtered = $proj->filter({ filter => $filter_condition } )->filter($array);

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

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

my $proj = Project->new();

my $filter_condition =         {
'name like' => 'a1',
'newfield exists' => 1,
                               };



my $array = [
{ name => 'a1', url => 'http://www.ozonsport.ru/product_3594.html'},
{ name => 'a2', url => 'http://www.ozonsport.ru/product_4147.html'},
{ name => 'a3', url => 'http://www.ozonsport.ru/product_2088.html'},
{ name => 'a4', url => 'http://www.ozonsport.ru/product_1293.html'},
{ name => 'a5', url => 'http://www.ozonsport.ru/product_3934.html',newfield => 'zzzX'},
{ name => 'a6', url => 'http://www.ozonsport.ru/product_3684.html'},
{ name => 'a7', url => 'http://www.ozonsport.ru/product_2580.html'},
{ name => 'a8', url => 'http://www.ozonsport.ru/product_2236.html'},
{ name => 'a9', url => 'http://www.ozonsport.ru/product_3247.html'},
{ name => 'a10', url => 'http://www.ozonsport.ru/product_3082.html'},
{ name => 'a11', url => 'http://www.ozonsport.ru/product_2240.html', newfield => 'zzz1'},
{ name => 'a12', url => 'http://www.ozonsport.ru/product_2240.html'},
{ name => 'a14', url => 'http://www.ozonsport.ru/product_2240.html', newfield => 0},
];

# приготовим исходный массив

my $array_filtered = $proj->filter({ filter => $filter_condition } )->filter($array);
print Dumper(\@$array);
print Dumper(\@$array_filtered);
$proj->log("finished");

exit(0);

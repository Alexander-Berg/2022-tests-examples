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

my $proj = Project->new({load_dicts => 0});

my $filter_condition =         {
#                                    'name like' => [ '1', '2', 'zzz' ],
#                                    'value like' => [ 'b' ],
#                                   'price <->' => ['100-','-19'],
#                                   'url normordercontains' => [ 'Защита от протечек воды' ], 
                                    'url titlecontains' => [ 'таможня' ],
#                                   'categpath like' => [ 'zopazopa' ],
#                                   'url NOT normcontains' =>  [ 'Товар временно отсутствует в продаже' ] 
                               };



my $array = [
    { url  => 'http://www.rbc.ru/technology_and_media/26/11/2015/5656d58a9a7947287ba642ab', name => 'bbb' },
    { url => 'http://www.mvideo.ru/stiralnye-i-sushilnye-mashiny/stiralnye-mashiny-s-vertikalnoi-zagruzkoi-2432', name => 'bbbb' },
];

# приготовим исходный массив

my $array_filtered = $proj->filter({ filter => $filter_condition } )->filter($array);

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

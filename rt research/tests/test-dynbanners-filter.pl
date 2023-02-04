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

my $proj = Project->new({load_dicts => 1});

my $filter_condition =         {
                                    'url normordercontains' => [ 'Защита от протечек воды' ], 
#                                   'url normtitlecontains' => [ 'zopazopa' ],
#                                   'categpath like' => [ 'zopazopa' ],
#                                   'url NOT normcontains' =>  [ 'Товар временно отсутствует в продаже' ] 
                               };



my $array = [
    { 
       url       => 'https://www.mediamarkt.ru/item/1082400/indesit-iwsb-5093-stiralnaya-mashina',
    },
];

# приготовим исходный массив

my $array_filtered = $proj->filter({ filter => $filter_condition } )->filter($array);

print Dumper(scalar(@$array));
print Dumper(scalar(@$array_filtered));

$proj->log("finished");

exit(0);

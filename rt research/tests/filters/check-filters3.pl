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

my $filter =  {
          'name ilike' => [
            'отвод',
          ],
};
my $urls = [ 
    { name => "Отвод Rehau Raupiano для присоединения111 выпуска унитаза 110/22°", url => "http://term-shop.ru/p150720885-otvod-rehau-raupiano.html" },
    { name => "Отвод Rehau Raupiano для присоединения222 выпуска унитаза 110/22°", url => "http://term-shop.ru/p150720885-otvod-rehau-raupiano.html" },
    { name => "отвод Rehau Raupiano для присоединения333 выпуска унитаза 110/22°", url => "http://term-shop.ru/p150720885-otvod-rehau-raupiano.html" },
    { name => "пщпщпщ Rehau Raupiano для присоединения333 выпуска унитаза 110/22°", url => "http://term-shop.ru/p150720885-otvod-rehau-raupiano.html" },
];

print Dumper($proj->filter({ filter => $filter })->filter($urls));

$proj->log("finished");

exit(0);

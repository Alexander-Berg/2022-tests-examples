#!/usr/bin/perl -w
use strict;
use utf8;
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
no warnings 'utf8';

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Data::Dumper;

print Dumper( 
    Project->new({load_dicts => 0})->filter({ filter => {
        "name like" => 'Кормушка',
        "value NOT" => [1,2,3],
    }})->filter([
        { name => 'Лягушка', value => 1 },
        { name => 'Кормушка для ПТИЦ', value => 2 },
        { name => 'Кормушка НЕ для ПТИЦ', value => 18 },
        { name => 'Кормушка для ПЧЁЛ', value => 18 },
    ])
);

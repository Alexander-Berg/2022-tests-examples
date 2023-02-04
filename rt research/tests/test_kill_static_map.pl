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

my @test_array = Utils::Words::lmr->norm_words('электроинструмент','ru');

print Dumper(\@test_array);

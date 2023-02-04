#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use FindBin;
use lib "$FindBin::Bin/../lib";

use Time::HiRes qw(gettimeofday tv_interval);
use Project;

my $proj = Project->new({ 
        load_dicts => 1,
});

my @test_phrases = (
    "гороскоп",
    "ремонт холодильников",
    "купить mp3 плеер",
    "москва аренда офисного помещения",
    "квартира ремонт",
    "отделочный работа",
    "квартира ремонт",
    "авто продажа",
    "автомобиль",
    "авто",
    "авто покупать",
    "автомобиль покупать",
    "евроремонт",
    "квартира продажа",
);

my $ntime = [gettimeofday];
$proj->phrase("подключи меня")->advqcount();
print STDERR "copnnaction time:" . tv_interval($ntime) . ".\n";


my $all_time = 0;
my $max_exp = 1;
for my $exp_number ( 1 .. $max_exp ) {
	for my $ph (map{$proj->phrase($_)} @test_phrases) {
	    my $start = gettimeofday ;
	    $ph->advqcount;
	    my $finish = gettimeofday ;
 	    $all_time += ( $finish - $start );
	}
}

print "done all in " .$all_time . ".\n";
print "speed = " . int($max_exp * scalar(@test_phrases) / $all_time) . " .phrases per second.\n";


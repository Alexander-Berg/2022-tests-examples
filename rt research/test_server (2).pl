#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw(gettimeofday tv_interval);
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new({ 
    load_dicts => 1 
});

my @a = $proj->cdict_client->get_bnr_count($proj->phrase("купить холодильник"));
print "$a[0]\n";

if($ARGV[0]) {
    # тестируем данными из файла

    open F, $ARGV[0] or die($!);
    my @test = map{(split "\t")[0]} <F>;
    close F;
        
    my $resp = $proj->cdict_client->exec_command("get\tcount\ttest");

    my $start = [gettimeofday];
    for my $text(@test) {
        $text =~ s/\-/ /g;
        $text = $proj->phrase($text)->norm_phr;
        my $resp = $proj->cdict_client->exec_command("get\tcount\t$text");
    }
    my $finish = [gettimeofday];

    my $dt = tv_interval($start, $finish);
    my $n = @test;

    print "$n queries for $dt\n";
    print "RPS: ".int($n / $dt)."\n";

    exit(0);
}

while(defined(my $query = STDIN->getline)) {
    chomp $query;
    print "-> $query\n";
    
    my $resp = $proj->cdict_client->exec_command_once($query);
    print "<- $resp\n" if $resp;

    last if !$proj->cdict_client->is_connected || $resp =~ /^BYE/;
}

#!/usr/bin/perl

use strict;
use utf8;
 
use open ":utf8";
no warnings "utf8";

binmode (STDIN, ":utf8");
binmode (STDOUT, ":utf8");
binmode (STDERR, ":utf8");

use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

$Data::Dumper::Useqq = 1;
{ 
    no warnings 'redefine';
    sub Data::Dumper::qquote {
        my $s = shift;
        return "'$s'";
    }
}

use FindBin;
use lib "/opt/broadmatching/scripts/lib";


use Project;
my $proj = Project->new();

my $filename = $ARGV[0] || '/home/sergio/scripts/banners/feed-tasks-banners/tourism/test.yml';

$proj->log("filename:$filename");
my $url = 'http://yml.techport.ru/?prayer=yandex-cpa&city=MSK';
my $t = time();
$proj->log( "feed creating started");
my $fd;
my $tstart = time();
print "==$tstart\n";
if ( $filename ){
    open F, $filename;
    my $txt = join ('', <F>);
    #$fd = $proj->feed({data => $txt, filters=>{"44"=>{"price >="=>20000}} });
    $fd = $proj->feed({url => $url, filters=>{"44"=>{"price >="=>20000}} });
}
my $tstart = [gettimeofday];
$proj->log("feed created in " . tv_interval($tstart));
$tstart = [gettimeofday];
my $c = $fd->get_offers_count;
$proj->log("counted $c in " . tv_interval($tstart));
exit;
$tstart = [gettimeofday];
my $ptl = $fd->ptl;
print $proj->dump_lite($fd),"\n";
$proj->log("ptl created in " . tv_interval($tstart));
my $cnt = scalar(@$ptl);
$proj->log("count: $cnt");

$proj->log("all finished");
print STDERR $proj->timelogreport;

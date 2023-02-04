#!/usr/bin/perl -w

# head -n100000000 test_file | perl statreduce.pl '11 2' > log_statreduce &

use lib '../../lib';
use StaticMap;

use utf8;
use open utf8;
binmode STDOUT, ":utf8";
binmode STDERR, ":utf8";
binmode STDIN,  ":utf8";

my $lmr = StaticMap->new('');
my $frm = ' 1 222';
#$frm = ' 11';
$frm = $ARGV[0] if $ARGV[0];
my $cc = 0;
my $beg = time;
while(<STDIN>){
    $cc++;
    print STDERR "cc: $cc ".(time - $beg)."\n" if $cc % 1000000 == 0;
    #print "l: $_\n";
    print $lmr->add_statline($_, $frm);
}
print $lmr->add_statline('', $frm);

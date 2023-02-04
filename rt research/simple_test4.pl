#!/usr/bin/perl -w
use strict;
use warnings;
use utf8;
use open ":utf8";

use Encode;

binmode(STDIN,':utf8');
binmode(STDOUT,':utf8');

use FindBin;
use lib "$FindBin::Bin/../../lib";
use lib "$FindBin::Bin/../../wlib";

use Getopt::Long;
use Data::Dumper;

print STDERR "-----------Ready----------\n";

#$proj->optimcamp_taskproc->optimize(31966, 0 );
#exit;
my $cnt = 0;
my %head2tail = ();
my %tail2head = ();
my %tailclusterid = ();
my %headclusterid = ();
my %clusterid2tail = ();
my %clusterid2head = ();
my $cachehits = 0;

my %input;

my $filename = '/home/apovetkin/harmonization_queries_data_head';
open F, $filename
  or die "Could not open file '$filename' $!";
 

while (<F>) {
   chomp;
   next unless $_;
   my ($inputstr) = split "\t", $_;
   $input{$inputstr} = 1;
   print STDERR " loading $cnt\n" unless $cnt++ % 100000;
}

close F;

$cnt = 0;
for my $inputstr (keys %input) {
    my @words = split /\s+/, $inputstr;
    my $text;
    my $found = 0;
    for ( @words ) {
        $text = $text ? join (' ', $text, $_) : $_;
        my $good = exists $input{$text} ? $text : '';
        if ( $good && $good eq $text && length($good) < length($inputstr)) {
            my $tail = substr($inputstr, length($good)+1 );
            if ( $tail ) {
                $found = 1;
                push @{$head2tail{$good}}, $tail;
                $tailclusterid{$tail} = {count => 0, id => ''};
                #push @{$tail2head{$tail}}, $good;
            }
        }
    }
    my $size = scalar keys %head2tail;
    print STDERR "hash init $cnt, size: $size \n" unless $cnt++ % 100000;
}
my %headcount = ();
$cnt = 0;
foreach my $head ( keys %head2tail ) {
    $headcount{$head} = scalar @{$head2tail{$head}};

    print STDERR "counting $cnt \n" unless $cnt++ % 100000;
}
$cnt = 0;
foreach my $head ( keys %head2tail ) {
    foreach my $tail ( @{$head2tail{$head}} ) {
        $tailclusterid{$tail} = {count => $headcount{$head}, id => $head } if $tailclusterid{$tail}->{count} < $headcount{$head};
    }

    print STDERR "assign clusters to tails $cnt \n" unless $cnt++ % 100000;
}
$cnt = 0;
foreach my $head ( keys %head2tail ) {
    my %tailclusters = ();
    foreach my $tail ( @{$head2tail{$head}} ) {
        $tailclusters{$tailclusterid{$tail}->{id}}++;
    }
    my $clusterid = (sort { $tailclusters{$b} <=> $tailclusters{$a} } keys %tailclusters )[0];
    $headclusterid{$head} = $clusterid;

    $clusterid2head{$clusterid}->{$head}++;

    foreach my $tail ( @{$head2tail{$head}} ) {
        $clusterid2tail{$clusterid}->{$tail}++;
    }

    print STDERR "assign clusters to heads $cnt \n" unless $cnt++ % 100000;
}
$cnt = 0;
#foreach my $tail ( keys %tailclusterid ) {
#    $clusterid2tail{$tailclusterid{$tail}->{id}}->{$tail}++;
#    
#    print "build id 2 tail $cnt \n" unless $cnt++ % 100000;
#}

#foreach my $head ( keys %head2tail ) {
#    my $clusterid = $headclusterid{$head};
#    foreach my $tail ( @{$head2tail{$head}} ) {
#        $clusterid2tail{$clusterid}->{$tail}++;
#    }
#    print "build id 2 tail $cnt \n" unless $cnt++ % 100000;
#}

my %clustercount ;
foreach ( keys %headclusterid ) {
    $clustercount{$headclusterid{$_}}++;
}

if (1) {
    foreach my $clusterid ( keys %clusterid2head ) {
        print join ("\t", 
            $clusterid, 
            join(',', map { join (':', $_, $clusterid2head{$clusterid}->{$_}) } keys %{$clusterid2head{$clusterid}} ), 
            join(',', map { join (':', $_, $clusterid2tail{$clusterid}->{$_}) } keys %{$clusterid2tail{$clusterid}} ),
        ) . "\n";
    }
    exit;
}




print "\ntop ten clusters: \n";
print join "\n", firstten(sort { $clustercount{$b} <=> $clustercount{$a} } keys %clustercount );

print "\nheads: " . (scalar keys %head2tail) . "\n";
print "\ntails: " . (scalar keys %tailclusterid) . "\n";
print "\nclusters: ". (scalar keys %clusterid2tail) . "\n";

print "\n-----------READY---------------\n";

while (<STDIN>) {
    chomp;
    my $row = $_;
    unless ( exists $head2tail{$row} ) {
        print "not found\n";
        next;
    }
    print "\nnative tails: " . ( scalar @{$head2tail{$row}} ) . "\n";

    print join "\n", firstten(@{$head2tail{$row}});

    print "\n\nadded tails: " . ( scalar keys %{$clusterid2tail{$headclusterid{$row}}} ) . "\n";
 
    print join "\n", firstten( sort { $clusterid2tail{$headclusterid{$row}}->{$b} <=> $clusterid2tail{$headclusterid{$row}}->{$a} } keys %{$clusterid2tail{$headclusterid{$row}}} );
    print "\n";
}

sub firstten {
    my @arr = @_;

    my $cnt = scalar @arr;
    $cnt = 9 if $cnt > 9;
    my @res;
    for (0..$cnt) { push @res, $arr[$_]};
    return @res;
}

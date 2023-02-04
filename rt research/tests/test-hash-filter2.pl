#!/usr/bin/perl -w
use strict;
use Data::Dumper;

use strict;
use utf8;
no warnings 'utf8';

use Getopt::Long;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

my $proj = Project->new({load_dicts   => 0});

my $start = [gettimeofday];
ts("script");

my $array = [];


ts("load");
open (fF,"<hash-filter.txt") || die $!;
while (<fF>) {
    chomp;
    my @values = split /\t/;
    my $hash = {};
    my $index = 0;
    for my $value ( @values ) {
        $hash->{"value" . (++$index)} = $value;
    }
    push @$array, $hash;
}
close fF;
te("load");

ts("clb filter");
my $array3 = $proj->filter( { filter => { 

    "value1" => 5, 
    "value9 lt" => '2', 
    "value10" => [1,2,3], 
    "value8 gt" => 7 

} } )->filter($array);
$proj->log("a3:" . scalar(@$array3));
te("clb filter");

te("script");

exit;

1;

# subs
{
  my $hash_times = {};
sub ts {
  my ( $name ) = @_;
  $hash_times->{$name} = [gettimeofday];
  $proj->log("$name started...");
}

sub te {
  my ( $name ) = @_;
  $proj->log("$name done in " . tv_interval( $hash_times->{$name} ));
  delete $hash_times->{$name};
}
}

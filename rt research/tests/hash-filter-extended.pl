#!/usr/bin/perl -w
use strict;
use Data::Dumper;

use strict;
use utf8;
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

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
open (fF,"<filter.sample") || die $!;
binmode(fF,":utf8");
while (<fF>) {
    chomp;
    my ( @items ) = split /\t/;
    my $hash = {};
    for my $item ( @items ) {
       if ( $item =~ /^(.*?)\=(.*?)$/ ) {
           $hash->{$1} = $2;
       }
    }
    push @$array, $hash;
}
close fF;
te("load");


$proj->log("a0:" . scalar(@$array));
ts("clb filter");
my $filter = $proj->filter( { filter => {
	"name like"             => [ 'Кормушка', 'Birds' ],
#        "name like"		=> 'ушка',
#	"url titlecontains"     => 'FB164',
#        "url normcontains"      => 'Приморский край',
#        "url contains"          => 'Приморский край',
#        "url normtitlecontains" => "отзывы цена фонарь",
}});
my $array3 = $filter->filter($array);
$proj->log("a3:" . scalar(@$array3));

for my $item ( @$array3 ) {
    print join("\t", map { "$_:" . $item->{$_} } keys %$item ) . "\n";
}

print Dumper($array3);
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

#!/usr/bin/perl -w

use strict;
use Storable;
use Encode qw/:all/;

use lib '/opt/broadmatching/scripts/lib';
use utf8;
use Project;
use Data::Dumper;

my $proj = Project->new();
my $broad_kyoto = $proj->broad_kyoto();

use Encode qw/:all/;

# simple get-set-delete
my $set_value = { 'ключ1 => ' => 'ЭтоРуссКие БуквыЫЫЫЫ', 'key2' => 'zopa', key3 => [1,2,3]};

my $set_result = $broad_kyoto->set( 12345, $set_value, 5);
print Dumper($broad_kyoto->get( 12345 ));

sleep(6);

print Dumper($broad_kyoto->get( 12345 ));

$set_result = $broad_kyoto->set( 12345, $set_value, 5000 );
print Dumper($broad_kyoto->get( 12345 ));
$broad_kyoto->delete(12345);
print Dumper($broad_kyoto->get( 12345 ));

# multi get-set-delete
my @set_multi = ();
for my $exp ( 1 .. 10 ) {
    push @set_multi, [ $exp, { exp_number => $exp, 'russian' => 'русские буквы', random_value => rand() * 100 }, 5 ];
}
push @set_multi, [ 11, "СКАЛЯР", 5 ];

$broad_kyoto->set_multi( @set_multi ); # array
$broad_kyoto->delete_multi( 1,2,3,5,7 );
print Dumper($broad_kyoto->get_multi( 1 .. 11 ));
sleep(6);
print Dumper($broad_kyoto->get_multi( 1 .. 11 ));



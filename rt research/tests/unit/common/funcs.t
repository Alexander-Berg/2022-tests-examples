#!/usr/bin/env perl

use strict;
use warnings;
use utf8;

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Storable qw(dclone);
use Utils::Funcs qw(patch_hash);


my @patch_hash_examples = (
    # simple test
    {
        old => { name => "Ivan", age => 20 },
        patch => { age => 30 },
        new => { name => "Ivan", age => 30 },
    },

    # add new key
    {
        old => { name => "Ivan", age => 20 },
        patch => { weight => 80 },
        new => { name => "Ivan", age => 20, weight => 80 },
    },

    # patch with array
    {
        old => { name => "Ivan", children => [qw(Oleg Olga)] },
        patch => { children => [qw(Oleg Olya Olesya)] },
        new => { name => "Ivan", children => [qw(Oleg Olya Olesya)] },
    },

    # patch subhash
    {
        old => { name => "Ivan", info => { age => 55, weight => 100 } },
        patch => { info => { age => 66 } },
        new => { name => "Ivan", info => { age => 66, weight => 100 } },
    },

    # something complex
    {
        old => { name => "Ivan", iii => [1,2,3], info => { age => 55, weight => 100 }, l1 => { l2 => { l3 => [] } } },
        patch => { iii => { jjj => 777 }, info => { height => 180 }, l1 => { l2 => { l3 => "" } }}, 
        new => { name => "Ivan", iii => { jjj => 777 }, info => { age => 55, weight => 100, height => 180 }, l1 => { l2 => { l3 => ""}}},
    },

    # with strict branches
    {
        old => {
            name => "Ivan",
            info => { age => 55, weight => 100 },
            favorite_ponies => {
                unicorns => {
                    stallions => [ "Shining Armour", "Flim" ],
                    mares => [ "Twilight Sparkle", "Lyra Heartstrings" ]
                },
                earth_ponies => {
                    mares => [ "Cheerilee" ]
                }
            }
        },
        
        patch => {
            'info#{strict}' => { age => 66 },
            favorite_ponies => {
                'unicorns#{strict}' => {
                    mares => [ "Starlight Glimmer" ],
                },
                earth_ponies => {
                    stallions => [ "Big Macintosh" ],
                },
                pegasi => {
                    mares => [ "Rainbow Dash" ],
                },
            }
        },

        new => {
            name => "Ivan",
            info => { age => 66 },
            favorite_ponies => {
                unicorns => {
                    mares => [ "Starlight Glimmer" ],
                },
                earth_ponies => {
                    mares => [ "Cheerilee" ],
                    stallions => [ "Big Macintosh" ],
                },
                pegasi => {
                    mares => [ "Rainbow Dash" ],
                },
            }
        },
    },

    # add values
    {
        old   => { 'good' => 1, 'bad' => { 'broken' => 11, 'lost' => 12 }, 'unknown' => 10 },
        patch => { 'good' => 2, 'bad' => { 'broken' => 3, 'lost' => 4 } },
        new   => { 'good' => 3, 'bad' => { 'broken' => 14, 'lost' => 16 }, 'unknown' => 10 },
        params => { add => 1 },
    },
    # add new values
    {
        old   => { 'good' => 1, },
        patch => {              'bad' => { 'broken' => 3, 'lost' => 4 } },
        new   => { 'good' => 1, 'bad' => { 'broken' => 3, 'lost' => 4 } },
        params => { add => 1 },
    },

    # empty patch does not change anything
    {
        old => { name => "Ivan", info => { age => 55, weight => 100 } },
        patch => { },
        new => { name => "Ivan", info => { age => 55, weight => 100 } },
    },

);

for my $h (@patch_hash_examples) {
    my %par = %{$h->{params} // {}};

    # default
    is_deeply(patch_hash($h->{old}, $h->{patch}, %par), $h->{new}, 'patch_hash');

    # in-place
    my $data = dclone($h->{old});
    patch_hash($data, $h->{patch}, clone_source => 0, %par);
    is_deeply($data, $h->{new}, "patch_hash in-place");
}


done_testing();

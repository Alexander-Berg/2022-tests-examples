#!/usr/bin/env perl
use strict;
use warnings;

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::Math qw(permutation_by_number);

my $h = {};
$h->{join '', permutation_by_number(5, $_)} = 1 for (0..119);
is(scalar(keys %$h), 120);

done_testing();

1;

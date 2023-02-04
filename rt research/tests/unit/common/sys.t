#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::Sys qw(are_strings_equal are_refs_same);

# are_strings_equal
ok(are_strings_equal(undef, undef), 'are_strings_equal two undef');
ok(!are_strings_equal(undef, "1"), 'are_strings_equal string with undef 1');
ok(!are_strings_equal("undef", undef), 'are_strings_equal string with undef 2');
ok(!are_strings_equal(undef, ""), 'are_strings_equal empty with undef');
ok(!are_strings_equal("1", "2"), 'are_strings_equal unequal strings 1');
ok(!are_strings_equal("", "222"), 'are_strings_equal unequal strings 2');
ok(are_strings_equal("1", "1"), 'are_strings_equal equal strings');
ok(are_strings_equal("", ""), 'are_strings_equal empty strings');

# are_refs_same
ok(are_strings_equal(undef, undef), 'are_refs_same two undef');
ok(!are_strings_equal(undef, 1), 'are_refs_same undef, not ref');
ok(!are_strings_equal(\"undef", undef), 'are_refs_same undef, ref str');
ok(!are_strings_equal(\"111", \"222"), 'are_refs_same two ref strings');
ok(!are_strings_equal(\"111", \"111"), 'are_refs_same two ref equal strings');
my $s = "111";
ok(are_strings_equal(\$s, \$s), 'are_refs_same two ref same object');

done_testing();

1;

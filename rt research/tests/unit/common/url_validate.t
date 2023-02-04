#!/usr/bin/env perl
use strict;
use warnings;

use utf8;

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::URLValidator::URLValidator;

my $url_validator = Utils::URLValidator::URLValidator->new( );

ok($url_validator->validate_url("http://ya.ru"));

done_testing();

1;

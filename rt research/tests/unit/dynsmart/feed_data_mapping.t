#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BM::BannersMaker::FeedDataMapping;
use Data::Dumper;
use Project;

my $proj = Project->new();

sub get_format {
	my ($h, $line) = @_;
	my %r = BM::BannersMaker::FeedDataMapping::_format_str($proj, 1, 0, 0, ($h, $line, 'res'));
	return $r{res};
}

sub get_format_no_empty_ci {
	my ($h, $line) = @_;
	my %r = BM::BannersMaker::FeedDataMapping::_format_str($proj, 0, 1, 0, ($h, $line, 'res'));
	return $r{res};
}

sub get_format_no_empty_ci_translit {
	my ($h, $line) = @_;
	my %r = BM::BannersMaker::FeedDataMapping::_format_str($proj, 0, 1, 1, ($h, $line, 'res'));
	return $r{res};
}

is(get_format({a => 1, b => 2}, '[a] - [b]'), '1 - 2');
is(get_format({a => 1, b => 2}, '[a] - b'), '1 - b');
is(get_format({a => 1}, '[a] - [b]'), '1 -');
is(get_format({a => 1}, '[A] - [b]'), '-');
is(get_format({a => 'ГК', b => 'US'}, '[a] - [b]'), 'ГК - US');

is(get_format_no_empty_ci({a => 1, b => 2}, '[a] - [b]'), '1 - 2');
is(get_format_no_empty_ci({a => 1}, '[a] - [b]'), undef);
is(get_format_no_empty_ci({a => 1, b => 2}, '[A] - [b]'), '1 - 2');

is(get_format_no_empty_ci_translit({a => 1, b => 2}, '[a] - [b]'), '1 - 2');
is(get_format_no_empty_ci_translit({a => 1}, '[a] - [b]'), undef);
is(get_format_no_empty_ci_translit({a => 1, b => 2}, '[A] - [b]'), '1 - 2');
is(get_format_no_empty_ci_translit({a => 'ГК', b => 'US'}, '[a] - [b]'), 'GK - US');

done_testing();

1;

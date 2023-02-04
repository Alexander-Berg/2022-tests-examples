#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use File::Slurp qw(read_file);
use JSON qw(from_json);

use BM::BannersMaker::BannerLandProject;
use BM::BannersMaker::FeedDataSource;


my $proj = BM::BannersMaker::BannerLandProject->new({});
my $fds = BM::BannersMaker::FeedDataSource->new({ proj => $proj });

my $test_cases_file = "$FindBin::Bin/line2h.json";
my $test_cases = from_json(read_file($test_cases_file, binmode => ':utf8'));


for my $case (@$test_cases) {
    my $tskv_line = $case->{tskv_line};
    $fds->{offer_tag} = $case->{offer_tag};
    my $offer_h = $fds->_line2h($tskv_line);
    my $expected = $case->{expected};
    is_deeply($offer_h, $expected, $case->{name});
}

done_testing();

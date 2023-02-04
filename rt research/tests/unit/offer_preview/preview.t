#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use File::Slurp qw(read_file);
use JSON qw(from_json to_json);

use BM::BannersMaker::MakeBanners;

my $self = {
    ID_FIELD => 'row_id',
    yield => sub {},
    dst_tables => {
        OUTPUT_TABLE => 0,
        LOG_TABLE => 1,
    }
};

BM::BannersMaker::MakeBanners::init_process_offer_common($self);
BM::BannersMaker::MakeBanners::begin_process_offer_common($self);

my $test_cases_file = "$FindBin::Bin/offers.json";
my $test_cases = from_json(read_file($test_cases_file, binmode => ':utf8'));

for my $case (@$test_cases) {
    my $expected = delete $case->{output};
    $case->{product_inf} = to_json($case->{product_inf});
    $self->{row} = $case;
    $self->{yield} = make_assert($self, $expected, $case->{product_class});
    BM::BannersMaker::MakeBanners::get_offer_preview($self);
}

BM::BannersMaker::MakeBanners::end_process_offer_common($self);

done_testing();


sub make_assert {
    my ($self, $expected, $comment) = @_;
    return sub {
        my ($data, $table) = @_;
        return unless $table == $self->{dst_tables}->{OUTPUT_TABLE};
        my $title = delete $data->{Title};
        my $expected_title = delete $expected->{Title};

        my $min_title_length = $expected_title ? 6 : 0;
        ok(length($title) >= $min_title_length, "$comment title length");

        is_deeply($data, $expected, "$comment output excluding title")
    };
}


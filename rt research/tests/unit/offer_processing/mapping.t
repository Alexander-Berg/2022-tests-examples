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

my @test_cases_files = qw /mapping_base.json mapping_case_sensitivity.json/;

sub get_mapping {
    my $case = shift;

    my $tskv_line = $case->{tskv_line};
    my $feed_data_type = $case->{feed_data_type};
    my $categs_inf = $case->{categs_inf};
    my $offer_tag = $case->{offer_tag};

    my $fdm = $proj->fdm;
    my ($str_source_to_canonical) = $fdm->get_mapping_by_feed_data_type($feed_data_type);
    $fds->{origin_map} = $fdm->get_origin_mapping_h($feed_data_type);
    $fds->{offer_tag} = $offer_tag;
    $fds->{tskvmap} = $str_source_to_canonical;
    my $offer_h = $fds->_line2h($tskv_line);

    my %h = %$offer_h;
    if (keys %$categs_inf and $offer_h->{categoryId} and $categs_inf->{$offer_h->{categoryId}}) {
        $h{categpath} = $categs_inf->{$offer_h->{categoryId}}{path};
    }
    # маппинг полей source_to_canonical
    if (exists $offer_h->{tskvmap}) {
        my %hres_tskv_map = $fdm->do_smartmap($offer_h->{tskvmap}, \%h);
        $h{$_} = $hres_tskv_map{$_} for (keys %hres_tskv_map);
        delete $h{tskvmap};
    }
    return \%h;
}

for my $test_file (@test_cases_files) {
    my $test_cases_file = "$FindBin::Bin/$test_file";
    my $test_cases = from_json(read_file($test_cases_file, binmode => ':utf8'));

    for my $case (@$test_cases) {
        my $mapped_offer = get_mapping($case);
        my $expected = $case->{expected};
        is_deeply($mapped_offer, $expected, "$test_file: " . $case->{feed_data_type} . " mapping");
    }
}

done_testing();


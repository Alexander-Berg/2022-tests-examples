#!/usr/bin/perl -w
use strict;
use utf8;
use FindBin;
use lib "$FindBin::Bin/../lib";
use lib "$FindBin::Bin/../wlib";
use Getopt::Long;
use Utils::Sys qw{url_encode handle_errors print_log};
use Utils::Urls;
use JSON qw(from_json);
use Data::Dumper;
$Data::Dumper::Useqq = 1;
{
    no warnings 'redefine';
    sub Data::Dumper::qquote {
        my $s = shift;
        return "'$s'";
    }
}

handle_errors();

my %opt;
GetOptions(\%opt, 'help', 'local', 'host=s', 'new_feed|new-feed');
if ($opt{help}) {
    printf "yml2directinf calls on local host or on chosen host using fcgi\n";
    printf "Options:\n";
    printf "    --local   run yml2directinf on local host (0 by default)\n";
    printf "    --host L  host on which yml2directinf should be launched using fcgi (if local = 0, default host is 'bmapi.yandex.ru')\n";
    printf "Examples:\n";
    printf "    perl test-yml2directinf-fcgi.pl --host man1-6782-24550.vm.search.yandex.net\n";
    printf "    perl test-yml2directinf-fcgi.pl --local\n";
    exit (0);
}

use CatalogiaMediaProject;
my $proj = CatalogiaMediaProject->new({ indcmd => 1,
    load_dicts => 1,
    load_minicategs_light => 1,
    use_comptrie_subphraser => 1,
    no_auth => 1,
    no_form => 1,
    nrmsrv => 0,
    projsrv => 0,
});

my $h = {
    max_file_size_type => 'bytes',
    max_file_size      => 536870912,
    business_type      => 'retail',
    gen_previews       => 0,
    no_feed_data       => 1,
    debug              => 1,
};

$opt{host} = $ARGV[0] // 'bmapi.yandex.ru';
$opt{local} //= 0;
$opt{new_feed} //= 0;
$h->{is_new_feed} = 1 if $opt{new_feed};


my $options = $Utils::Common::options;
my $query = $options->{tests_banners_generation}{bmapi_yml2directinf_url};
$query =~ s/bmapi\.yandex\.ru/$opt{host}/;

my @test_urls = (
    # normal tests
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_ampersand_1.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_closed.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_correct_tag_symbols.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_cycle.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_cycle_self.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_doctype.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_eof.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_equals.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_expected_close_one.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_expected_close_two.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_expected_gt.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_expected_quot.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_symbols.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1204_utf8.xml",
    "https://proxy.sandbox.yandex-team.ru/3365535125/1204_wrong_path.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_bad_price.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_bad_url.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_duplicate_offer_id.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_no_offers.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_no_valid_offers.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_offer_id.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1205_offerid_not_found.xml",
    "https://proxy.sandbox.yandex-team.ru/3358931317/1205_price_0.xml",
    "https://proxy.sandbox.yandex-team.ru/3358931317/1205_vendor_model.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1206_no_categories.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1212_cant_detect.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1213_cant_detect_any.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1213_feed_data_type.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1213_possible_types.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1213_too_many_missing.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1220_not_correspond.xml",
    "https://proxy.sandbox.yandex-team.ru/3337961151/1290_category.xml",
    "http://httpstat.us/500",
);

my @failed;

for my $i (0..$#test_urls) {
    $h->{url} = $test_urls[$i];
    print_log("Test url '".($h->{url})."'");
    my $json_res = '';

    if ($opt{local}) {
        my $datacamp_crawler_url = Utils::Urls::get_cgi_url_param($h->{url}, "site");
        if ($h->{url} and Utils::Urls::is_datacamp_feed_url($h->{url}) and $datacamp_crawler_url) {
            # for smart by site, fake answer
            $json_res = BM::BannersMaker::Feed::fake_yml2directinf($proj, $datacamp_crawler_url);
        } else {
            my @arr_feed_params_names = qw{url max_file_size_type max_file_size business_type is_new_feed};
            if (Utils::Urls::is_datacamp_feed_url($h->{url})) {
                $h->{url} = Utils::Urls::get_cgi_url_param($h->{url}, 'url')
            }
            my $fd = $proj->feed({ map {$_ => $h->{$_}} grep {$h->{$_}} @arr_feed_params_names });

            my @params_to_pass = qw/debug gen_previews no_feed_data/;
            my %inf_params = map {$_ => $h->{$_} // ''} @params_to_pass;
            eval {
                $json_res = $fd->yml2directinf(\%inf_params);
            };
            if ($@) {
                print_log("Test failed:\n$@");
                push @failed, $i;
                next;
            }
        }
    } else {
        my $test_query = $query . url_encode($h->{url});
        if ($h->{is_new_feed}) {
            $test_query .= "&status=New"
        }
        my $result_file = $proj->get_tempfile("yml2directinf_test", UNLINK => 1);
        my $load_status = $proj->load_file_by_url($test_query, $result_file);
        if (!$load_status) {
            print_log("Can't get response on yml2directinf query! ('$test_query' failed)");
            push @failed, $i;
            next;
        }

        open my $F, "<", $result_file;
        $json_res = <$F>;
        chomp $json_res;
        close $F;
    }

    if ($json_res) {
        my $res = from_json($json_res);
        if ((not @{$res->{errors}}) and (not @{$res->{warnings}}) and $res->{offer_examples} and $res->{offer_examples}->{data_params}) {
            my $previews_number = scalar keys %{$res->{offer_examples}->{data_params}};
            if ($previews_number) {
                print_log("Test passed successfully! $previews_number previews are generated");
            } else {
                print_log("Test did not pass: yml2directinf has worked, but there is no previews, generation did not work");
                push @failed, $i;
            }
        } else {
            print_log("errors:\n".Dumper($res->{errors})) if ($res->{errors});
            print_log("warnings:\n".Dumper($res->{warnings})) if ($res->{warnings});
            push @failed, $i;
        }
    } else {
        print_log("Test did not pass: error while calling yml2directinf function");
        push @failed, $i;
    }
    print "------------------------------------\n";
}

print_log((scalar @failed)." test failed: ".(join ", ", @failed)) if (@failed);
exit(@failed ? 1 : 0);

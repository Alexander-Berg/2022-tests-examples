use utf8;
use BM::BannersMaker::BannerLandProject;

my $proj = BM::BannersMaker::BannerLandProject->new({
    load_dicts => 1,
    load_minicategs_light => 1,
    load_languages => [ qw(ru en tr) ],
    allow_lazy_dicts => 1,
    use_comptrie_subphraser => 1,
    use_sandbox_categories_suppression_dict => 1,
});

my $phr = $proj->phrase("товары для дома");

my $norm = $phr->norm_phr;
if ($norm ne 'дом товар') {
    die "Bad normalization!" 
}

my @ctg = $phr->get_minicategs;
if (!@ctg) {
    die "Categs not found!";
}

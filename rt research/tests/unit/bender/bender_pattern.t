#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";

use BM::BMClient::BenderPattern;
use Project;
my $proj = Project->new({
    load_dicts                              => 1,
    load_minicategs_light                   => 1,
    use_comptrie_subphraser                 => 1,
    use_sandbox_categories_suppression_dict => 1,
});

my $pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "пицца <доставка бесплатно>"});
ok($pattern->is_match(['доставка пиццы', 'доставка бесплатно']));
ok(!$pattern->is_match(['доставка пиццы', 'доставка по москве бесплатно']));
ok($pattern->is_match(['доставка пиццы', 'внутри мкад доставка бесплатно']));
ok(!$pattern->is_match(['доставка пиццы бесплатно', '']));

$pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "[<доставка пицца>/<доставка суши>]"});
ok($pattern->is_match(['доставка пиццы', 'доставка бесплатно']));
ok(!$pattern->is_match(['доставка 20 минут', 'суши доставка']));
ok($pattern->is_match(['доставка пиццы', 'доставка суши']));
ok($pattern->is_match(['   доставка   пиццы   ', '  доставка   бесплатно   ']));

$pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "[<доставка пицца>/<доставка суши>] <one>"});
is($pattern->{server_pattern}, 'one (__mw_доставка_пицца|__mw_доставка_суши)');

$pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "[<доставка пицца>/<доставка суши>]"});
is($pattern->{server_pattern}, '(__mw_доставка_пицца|__mw_доставка_суши)');

# стопслова учитываются для мультивордов
$pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "техосмотр <для осаго>"});
is($pattern->{server_pattern}, 'техосмотр __mw_для_осаго');

# стопслова не учитываются вне мультивордов
$pattern = BM::BMClient::BenderPattern->new({proj => $proj, text => "на техосмотр <для осаго>"});
is($pattern->{server_pattern}, 'техосмотр __mw_для_осаго');

done_testing();

1;
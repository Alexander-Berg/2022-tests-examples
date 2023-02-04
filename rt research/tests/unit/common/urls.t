#!/usr/bin/env perl
use strict;
use warnings;

use utf8;

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::Urls qw(canonical_url get_sec_level_domain
                   url_to_domain get_url_parts
                   url_to_punycode url_to_unicode);

is(url_to_domain('http://www.yandex.ru/maps'), 'yandex.ru');
is(url_to_domain('http://money.yandex.ru/maps'), 'money.yandex.ru');
is(url_to_domain('http://money.yandex.ru'), 'money.yandex.ru');

is(canonical_url('http://www.yandex.ru/maps'), 'yandex.ru/maps');
is(canonical_url('https://yandex.ru/maps'), 'yandex.ru/maps');
is(canonical_url('https://www.yandex.ru/maps'), 'yandex.ru/maps');
is(canonical_url('www.yandex.ru/maps'), 'yandex.ru/maps');

is(get_sec_level_domain('http://www.yandex.ru/maps'), 'yandex.ru');
is(get_sec_level_domain('http://money.yandex.ru/basket'), 'yandex.ru');
is(get_sec_level_domain('http://www.video.vk.com/123456'), 'vk.com');
is(get_sec_level_domain('https://www.video.vk.com/123456'), 'vk.com');
is(get_sec_level_domain('www.video.vk.com/123456'), 'vk.com');
is(get_sec_level_domain('video.vk.com/123456'), 'vk.com');
is(get_sec_level_domain('vk.com/123456'), 'vk.com');
is(get_sec_level_domain('xn--90aeelpae0bb3a.xn--p1ai'), 'xn--90aeelpae0bb3a.xn--p1ai');

is_deeply(get_url_parts('http://www.yandex.ru/maps/123'), ['yandex.ru' ,'maps', '123']);
is_deeply(get_url_parts('http://www.yandex.ru/maps/123/'), ['yandex.ru' ,'maps', '123']);
is_deeply(get_url_parts('yandex.ru/maps'), ['yandex.ru' ,'maps']);

is(url_to_punycode('http://www.yandex.ru'), 'http://www.yandex.ru');
is(url_to_punycode('http://yandex.ru'), 'http://yandex.ru');
is(url_to_punycode('yandex.ru'), 'yandex.ru');
is(url_to_punycode('http://мир-закона.рф'), 'http://xn----8sba0adpjnip.xn--p1ai');
is(url_to_punycode('мир-закона.рф'), 'xn----8sba0adpjnip.xn--p1ai');

is(url_to_unicode('http://www.yandex.ru'), 'http://www.yandex.ru');
is(url_to_unicode('http://yandex.ru'), 'http://yandex.ru');
is(url_to_unicode('yandex.ru'), 'yandex.ru');
is(url_to_unicode('http://xn----8sba0adpjnip.xn--p1ai'), 'http://мир-закона.рф');
is(url_to_unicode('xn----8sba0adpjnip.xn--p1ai'), 'мир-закона.рф');

done_testing();

1;

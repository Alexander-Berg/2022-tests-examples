#!/usr/bin/perl

use strict;
use utf8;
use v5.10;

use Data::Dumper;
use Test::More;
use POSIX qw/strftime/;
use FindBin;

use lib "$FindBin::Bin/../../../lib";
use Project;
use Utils::Sys qw(md5int);

# init kyoto client
my $proj = Project->new;
my $broad_kyoto = $proj->broad_kyoto();

# init pseudo-unique value for testing purposes (since cache is global)
my $pseudo_unique_scalar = join(".", 'pseudo_unique_scalar', $$, strftime('%Y%m%d%H%M%S', localtime));

# test simple get-set-delete
my $set_key = $pseudo_unique_scalar . '12345';
my $set_value = {'ключ1 => ' => 'ЭтоРуссКие БуквыЫЫЫЫ', 'key2' => 'zopa', key3 => [1,2,3]};

# set is ok
my $set_result = $broad_kyoto->set($set_key, $set_value, 1000);
ok($set_result == 1, 'set return value is OK');
sleep(1);

# get several times is equal to set
for my $i (1..3) {
    my $get_value = $broad_kyoto->get($set_key);
    is_deeply($get_value, $set_value, "get result # $i is equal to set result");
}

# result is undefined after delete
$broad_kyoto->delete($set_key);
sleep(1);
my $get_value2 = $broad_kyoto->get($set_key);
is($get_value2, undef, 'get result is undef after delete');

sleep(1);
# delete the same key second time is OK
ok(defined $broad_kyoto->delete($set_key), 'delete the same key second time is OK');

# set big value
my $set_key2 .= '_with_big_value';
my $base = md5int(rand);
my $set_value2 = join('', map {"$base $_ абвгдеёжз"} 1 .. 500_000);
my $value_size = sprintf "%.2f", length($set_value2) / 1024 / 1024;
my $set_result2 = $broad_kyoto->set($set_key2, $set_value2, 1000);
ok($set_result2 == 1, "set big value ($value_size MB) is OK");
sleep(1);
my $get_value2 = $broad_kyoto->get($set_key2);
ok($get_value2 eq $set_value2, "get result after set big value");
$broad_kyoto->delete($set_key2);
sleep(1);

# set several times is OK
my $inc_set_key = $pseudo_unique_scalar . 'inc_set_key';
my $last_val = 5;
for my $inc_val (1 .. $last_val) {
    my $set_result = $broad_kyoto->set($inc_set_key, $inc_val, 100);
}
sleep(1);
is($broad_kyoto->get($inc_set_key), $last_val, 'set different values several times is OK');
$broad_kyoto->delete($inc_set_key);

# it's safe to set undef as value
my $undef_key = $pseudo_unique_scalar . 'undef_key';
my $set_undef_result = $broad_kyoto->set($undef_key, undef);
ok($set_undef_result == 1, 'set undef return value is OK');
sleep(1);
my $get_undef_value = $broad_kyoto->get($undef_key);
is($get_undef_value, undef, 'get result of set undef');
$broad_kyoto->delete($undef_key);

# try to set undef as key
my $val = $pseudo_unique_scalar . 'value_of_undef_key';
my $undef_set_result = $broad_kyoto->set(undef, $val);
ok($undef_set_result == 1, 'set return value for undef is OK');
sleep(1);
is($broad_kyoto->get(undef), $val, 'get by undef is OK');

# try to set sub as value
my $sub_key = $pseudo_unique_scalar . 'sub_key';
my $sub_value = { 1 => sub {42} };
my $sub_set_result = 0;
eval {
    $sub_set_result = $broad_kyoto->set($sub_key, $sub_value, 10);
};
ok(!$sub_set_result, 'value cannot be subroutine');
is($broad_kyoto->get($sub_key), undef, 'nothing is got after setting subroutine as value');

# test multi set-get-delete
# prepare multi keys
my @set_multi = ();
for my $mul (1..3) {
    push @set_multi, [ $pseudo_unique_scalar . "set multi $mul", { val => $mul, }, 120 ];
}

# set multi
my @keys_multi = map {$_->[0]} @set_multi;
my $set_multi_result = $broad_kyoto->set_multi(@set_multi);
for my $key_multi (@keys_multi) {
    ok($set_multi_result->{$key_multi}, "set_multi result is OK");
}

sleep(1);
# get multi
my $get_multy_result = $broad_kyoto->get_multi(@keys_multi);
for my $item (@set_multi) {
    my ($key, $value, $timeout) = @$item;
    is_deeply($get_multy_result->{$key}, $value, 'get_multi result is OK');
}

sleep(1);
# delete multi
my $delete_multi_result = $broad_kyoto->delete_multi(@keys_multi);
for my $key_multi (@keys_multi) {
    ok($delete_multi_result->{$key_multi}, "delete_multi result is OK");
}

sleep(1);
# second get is undefined
my $get_multy_result_deleted = $broad_kyoto->get_multi(@keys_multi);
for my $item (@set_multi) {
    my ($key, $value, $timeout) = @$item;
    is($get_multy_result_deleted->{$key}, undef, 'get_multi result after delete is undefined');
}
done_testing();

1;

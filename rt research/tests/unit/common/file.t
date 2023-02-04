#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;
use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Project;

my $proj = Project->new();

my $filename = "test96201.txt";
$proj->do_sys_cmd("echo '1\n2\n3\n4\n5\n6\n7\n8\n9' > $filename");
my $f = $proj->file($filename);

# test wc_l
is($f->wc_l, 9, "count");

# test random_lines many
is_deeply([$f->random_lines(20)], [1..9], "random many");

# test random_lines
sub is_in_range {
	my ($h, $start, $end) = @_;
	for (values %$h) {
		return 0 unless $start <= $_ && $_ <= $end;
	}
	return 1;
}

my $h = {};
for (1..90000) {
	map {$h->{$_}++} $f->random_lines(3);
}
ok(is_in_range($h, 29000, 31000), "random");


$proj->do_sys_cmd("rm $filename");
done_testing();

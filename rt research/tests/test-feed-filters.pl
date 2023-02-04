#!/usr/bin/perl -w
use strict;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new({load_dicts   => 0});

# фильтры
my $feed_filters = {
    'filter15'    => {category_id => 15},
    'filter22'    => {category_id => [1,91,18,15]},
};

# пример исходных данных
my $feed_data = [
    { id => 1, 'category_id' => 15 },
    { id => 2, 'category_id' => 1 },
    { id => 3, 'category_id' => 36 },
    { id => 4, 'category_id' => 1 },
    { id => 5, 'category_id' => 91 },
    { id => 6, 'category_id' => 18 },
];

$proj->log("started");
my $filters = $proj->filters( $feed_filters );
for my $item ( @$feed_data ) {
    $proj->log("(id:" . $item->{id} . ",cat:" . $item->{category_id} . ")\tfilters ON:" . join(',', $filters->grep_filter($item) ) );
}

$proj->log("finished");

exit(0);

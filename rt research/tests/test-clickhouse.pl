#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new;
my $conv = $proj->conversion_collector2;

my $bid = 422359835;
my $BannerID = 297733586;

my $date = '2014-08-31';
my $sql = "
    select 
        sum(Sign) AS Clicks,
        sum(Sign*(GoalReachesAny > 0)) AS GoalClicks
    FROM clicks_all
    WHERE
        ClickBannerID = $BannerID
        AND AdvEngineID = 1
        AND StartDate = toDate('$date')
        AND ClickGoodEvent > 0
";
print $sql, "\n";
my $res = $conv->fetch_clickhouse($sql);
print $res;

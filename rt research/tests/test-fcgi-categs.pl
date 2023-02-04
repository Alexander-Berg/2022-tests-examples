#!/usr/bin/perl -w

use strict;
use utf8;
use Data::Dumper;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN, ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use LWP::UserAgent;
print "SERVER RESULT IS:\n" . LWP::UserAgent->new->post( 'http://bmapi-dev01e.yandex.ru/fcgi-bin/', ['act' => 'test_categs_info', 'data' => 'пожарный сигнализация стоимость'."\n#END"] )->decoded_content() . "\n";

exit(0);



# sudo perl -MCPAN -e 'install Judy'

use Time::HiRes qw/gettimeofday tv_interval time/;
use Data::Dumper;

use Judy::HS qw( Set Get Free );

my $judy;
my %hh = ();
=h
for (qw( d d c a d b c b a b a c )) {
    my ( undef, $value ) = Get( $judy, $_ );
    if ( ! $value ) {
        Set( $judy, $_, 1 );
        print "$_\n";
    }
}
printf "Freed %d bytes\n", Free( $judy );
=cut

my $start;
$start = [gettimeofday];
print time."\n";
for(1 .. 10_000_000){
    Set($judy, 'sdfsdfsdfs'.$_ => $_ );
    $hh{'sdfsdfsdfs'.$_} = $_;
}
print  "create:" . tv_interval($start) . "\n";

#print Get($judy, 'sdfsdfsdfs1232')."\n";

$start = [gettimeofday];
Get($judy, 'sdfsdfsdfs1232') for 1 .. 10_000_000 ;
print  "judy:" . tv_interval($start) . "\n";

$start = [gettimeofday];
$hh{'sdfsdfsdfs'.1231} for 1 .. 10_000_000 ;
print  "hash:" . tv_interval($start) . "\n";


sleep(100) while(1);

=h
use Tie::Judy;

my $judy = Tie::Judy->new();
my %hh = ();

print time."\n";
for(1 .. 10000){
    $judy->insert( { 'sdfsdfsdfs'.$_ => $_ } );
    $hh{'sdfsdfsdfs'.$_} = $_;
}

$judy->insert( { 'aaaa' => 12312312 } );

print time."\n";
$judy->retrieve('sdfsdfsdfs'.1231) for 1 .. 1_000_000 ;
print time."\n";

print time."\n";
$hh{'sdfsdfsdfs'.1231} for 1 .. 1_000_000 ;
print time."\n";
=cut


print "Ok\n";

#Judy::1::Set(
#    $judy_1,
#    42
#);



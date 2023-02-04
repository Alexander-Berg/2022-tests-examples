#!/usr/bin/perl

use strict;
use Time::HiRes qw/gettimeofday tv_interval/;
use Data::Dumper;
use FindBin;
use Date::Calc qw(:all);

select STDERR; $| = 1;
select STDOUT; $| = 1;

print STDERR "preparing...\n";

# setup
my $MAPPER_TEMPLATE = "$FindBin::Bin/mapper.template";
my $REDUCER         = "$FindBin::Bin/reducer.sh";
my $MR_COMMAND	    = "/usr/bin/mapreduce -server bsmr-server01i.yandex.ru:8013 -opt box=developers -jobcount 50"; 

# stage0 - начало

# директория для работы
my $tempdir = '/opt/broadmatching/temp/mapreduce';
`mkdir -p $tempdir`;

# stage1 - получаем параметры запуска - from-to, режим работы (словарь-статистика), from, to
my $date = Get2DayBefore();
my $stime = [gettimeofday];

print STDERR "get hour tables...\n";

# stage2 - получаем список таблиц, разбитых по часам, считаем, что meta первого часа относится ко всему часу
my @all_tables = GetAllHourTables( $date );
@all_tables = @all_tables[3 .. 3]; # первые два часа

print STDERR "total tables:" . scalar(@all_tables) . ".\n";

# stage3 - DoMapReduce
my $htime = [gettimeofday];
my $current_outfile = $tempdir . '/' . join("_", $$, "PPCGOT.mapreduce");
print STDERR "do map-reduce for " . scalar(@all_tables) . " . tables into file:$current_outfile\n";
my $result = DoMapReduce( \@all_tables, $current_outfile );

print STDERR "done in " . tv_interval($htime) . ", outfile:$current_outfile, left" . scalar(@all_tables) . " tables\n";

print STDERR "to main outfile...\n";
# stage4 - для всех выходных файлов: склейка их в один, сортировка и финальный reduce, удаление выходных файлов
my $cmd = "cat $current_outfile " . q( | LANG=C sort -k1,1 -t$'\t' -T ) . $tempdir . ' -S2G | head -n10 ';
my $res = `$cmd`;

print "TOP-10 MAPREDUCE RESULT:\nBEGIN\n$res" . "END\n";

# удалим промежуточный файл
unlink $current_outfile;

print STDERR "all done in " . tv_interval($stime) . "\n";
# stage5 - конец
exit(0);

sub GetAllHourTables {
	my ( $date ) = @_;
	
	my $map_list = `$MR_COMMAND -list | grep RotateHitLog | grep -v meta`;
	my @all_tables = ();

	for my $table ( split /[\r\n]+/, $map_list ) {
		if ( $table =~ /^yabs-log\/\d{6}\/RotateHitLog(\d{8})(\d{2})/ ) {
			if ( $1 eq $date ) {
				push @all_tables, $table;
			}
		}
	}
	return @all_tables;
}

sub DoMapReduce {
	# stage0 - начало
	my ( $this_tables, $outfile ) = @_;
	
	# stage1 - по первой таблице читаем meta, заполняем шаблон mapper-a в mapper_current.sh
	my $first_meta_table = "meta/" . $this_tables->[0];
	my $last_meta_table =  "meta/" . $this_tables->[ scalar(@$this_tables) - 1 ];

        my $meta = `$MR_COMMAND -read $first_meta_table`;
        $meta =~ s/^.*?FORMAT:str\s+(.*?)$/$1/;

	my @fields = 
		     map { (split /\:/, $_)[0] }
		     split /\,/, 
		     $meta
    ;

	my $mapper_template = "";

	open (fF,"<$MAPPER_TEMPLATE") || die("cannot open mapper_template:$MAPPER_TEMPLATE");
	while (<fF> ) {
		$mapper_template .= $_;
	}
	close fF;

	for ( my $field_index = 0; $field_index < scalar(@fields); $field_index++ ) {
		$mapper_template =~ s/%$fields[$field_index]%/($field_index+2)/gex;
	}

    my $mapper_name = 'mapper' . $$ . '.sh';
    my $mapper_script = $tempdir . '/' . $mapper_name;
	system("rm -f $mapper_script");
	open (tF,">$mapper_script") || die("cannot open mapper_script:$mapper_script");
	print tF $mapper_template;
	close tF;
	system("chmod +x $mapper_script");

	print STDERR "mapper script:$mapper_script.\n";
	print STDERR "cat mapper script:$mapper_template.\n";

	# stage2 - map первой пачки в out1
	print STDERR "map...";
	system("rm -f $outfile");
	system("$MR_COMMAND -drop users/bmclient/out1.$$");
    my $map1_command = "$MR_COMMAND -map ./$mapper_name " . join(" ", map { "-src $_" } @$this_tables) . " -dst users/bmclient/out1.$$ -file $mapper_script";
    print "map1_command:$map1_command.\n";
    system($map1_command);

	# stage3 - reduce первой пачки в out2
	print STDERR "reduce...";
	system("$MR_COMMAND -reduce ./reducer.sh -src users/bmclient/out1.$$ -dst users/bmclient/out2.$$ -file " . $REDUCER);
	
	# stage4 - read в outfile
	print STDERR "read...";
	system("$MR_COMMAND -read users/bmclient/out2.$$ > $outfile");

	system("$MR_COMMAND -drop users/bmclient/out1.$$");
	system("$MR_COMMAND -drop users/bmclient/out2.$$");
	system("rm -f $mapper_script");

	# stage5 - конец
	print STDERR "done...";
	return $outfile;
}

sub Get2DayBefore {
    my @day_before = Add_Delta_Days( Today() , -2 );
    return sprintf("%04s%02s%02s", @day_before );
}

1;

__END__

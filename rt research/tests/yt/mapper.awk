#!/usr/bin/mawk -f 'BEGIN 		{ FS="\t" } { if ( $4 == 8 && $5 == 2 ) { print } }

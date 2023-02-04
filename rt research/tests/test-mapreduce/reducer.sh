#!/bin/bash
awk -F$'\t' 'BEGIN {lastkey=""} {key = $1; if ( lastkey != key && lastkey != "" ) { print lastkey"\t"hits"\t"sprintf("%d",sum/hits)"\t"sprintf("%d",maxhits); hits=0;sum=0;maxhits=0; } lastkey=key; hits += 1; sum += $2; if ( $2 > maxhits ) { maxhits = $2 } } END                                 { print lastkey"\t"hits"\t"sprintf("%d",sum/hits)"\t"sprintf("%d",maxhits) }'

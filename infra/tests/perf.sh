bpftool=$(ya tool bpftool --print-path)
sudo ./test_perf_egress.py -t $bpftool -o ../progs/obj/dummy_egress.o
sudo ./test_perf_egress.py -t $bpftool -o ../progs/obj/net_stat_dc_tx.o -n nets.txt

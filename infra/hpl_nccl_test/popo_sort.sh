


for i in $(cat mpi_hosts.txt);do grep ${i:0:9} host_map.txt.sorted | gawk '{print $1 " slots=8 #" $2}';done  | sort -k3 > mpi_hosts.txt.sorted

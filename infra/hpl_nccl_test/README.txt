Run xhpl and nccl_tests as yt job


- stage0  (per host  selftest from opt/hpc-benchmarks-cfg/selftest)
  - nccl/reduce_perf -r -1
  - nccl/all_reduce_perf 
  - xhpl with 40GB
- create ssh swarm
- measurments for all noder
  nccl/sendrecv -r -1 (limit 8GB/s)
  nccl/all_reduce (limit 70Gb/s)
  xhpl config from CLI


Uage:

```
./hpl_nccl_test --settings settings_a100_80g.yson  --ssh-key $ARC_ROOT/infra/qemu/guest_images/qavm/keys/id_rsa  --job-count 2

# Run tests on YATI2 cluster test
./hpl_nccl_test --settings settings_a100_80g_test.yson  \
		--ssh-key $ARC_ROOT/infra/qemu/guest_images/qavm/keys/id_rsa  \
		--scheduling_tag_filter infiniband_cluster_tag:YATI2 \
		--job-count 4 
```

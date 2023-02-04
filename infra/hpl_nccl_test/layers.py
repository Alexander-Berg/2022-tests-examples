def get_layers(layers_path):
    layers = \
        [layers_path + "/hpc-benchmarks-cfg.tar.zst"] + \
        [layers_path + "/hpc-benchmarks.tar.zst"] + \
        [layers_path + "/xhpl_cuda.tar.zst"] + \
        [layers_path + "/nccl-tests-v2.10.1-3-gf60770e1681b.tar.zst"] + \
        [layers_path + "/hpcx.tar.zst"] + \
        [layers_path + "/cuda-11.0.tar.zst"] + \
        [layers_path + "/drivers.tar.zst"] + \
        ["//home/runtimecloud/porto_layers/home_yt_slots.tar.zst"] + \
        ["//home/runtimecloud/porto_layers/rtc-base-xenial/vm-layer/layer.tar.zst"]

    # RESMAN-101: Reduce GPU_CLOCK_WARNING during summer month
    # Patch /opt/hpc-benchmarks-cfg/hpl_tune.sh, set GPU_CLOCK_WARNING=1200
    # TODO: Should be dropped once nvidia-gpumanager reverts gpuclocks back to 1275
    layers = [layers_path + "/hpc-benchmarks-cfg-clock-warn-1200.tar.zst"] +  layers

    return layers

def get_layers(layers_path):
    layers = \
        [layers_path + "/hpc-benchmarks-cfg.tar.zst"] + \
        [layers_path + "/hpc-benchmarks.tar.zst"] + \
        [layers_path + "/hpcx.tar.zst"] + \
        [layers_path + "/cuda-11.0.tar.zst"] + \
        [layers_path + "/drivers.tar.zst"] + \
        ["//home/runtimecloud/porto_layers/home_yt_slots.tar.zst"] + \
        ["//home/runtimecloud/porto_layers/rtc-base-xenial/vm-layer/layer.tar.zst"]

    return layers

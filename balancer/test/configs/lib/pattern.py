# --- CONSTANTS ---


_XFFY_HEADERS = [None, '1.1.1.1']


suggest_options = {
    "has_antirobot": False,
    "has_laas": False,
    "has_uaas": False,
}


# --- FUNCTIONS ---


def gen_test_ids(paths):
    return [p.lstrip('/').replace('/', '_') if (p != '/') else "index" for p in paths]


def extend_weights_test_params(subheavy_ctx, locations_weights_info, service_stat_name):
    for location, value in locations_weights_info["weights"].iteritems():
        if "devnull" in location and value == 1:
            locations_weights_info["exception_message"] = "status code is not OK"
            break

    extended = locations_weights_info
    extended["service_stat_name"] = service_stat_name
    extended["service_total_stat_name"] = "service_total"
    return extended


def main_test(subheavy_ctx, testcfg, path):
    subheavy_ctx.set_native_location(testcfg.native_location)
    service_stat_name = testcfg.module_name
    subheavy_ctx.run_base_service_test(path,
                                       backends=(testcfg.backends),
                                       service_stat_name=service_stat_name,
                                       **testcfg.get_options())


def header_test(subheavy_ctx, testcfg, path, xffy_header):
    subheavy_ctx.set_native_location(testcfg.native_location)
    subheavy_ctx.run_header_test(path,
                                 backends=testcfg.backends,
                                 xffy_header=xffy_header)


def location_weights_test(subheavy_ctx, testcfg, path, locations_weights_info, **kwargs):
    subheavy_ctx.set_native_location(testcfg.native_location)
    options = dict(testcfg.get_options().items() + extend_weights_test_params(subheavy_ctx, locations_weights_info, testcfg.module_name).items())
    options.update(kwargs)
    subheavy_ctx.run_location_weights_test(path, **options)

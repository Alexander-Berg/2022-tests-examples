from ads.bsyeti.big_rt.py_test_lib.logsampler.config.config import Config

config = Config(
    yav_version="sec-01cq1pnpgqg07gm78ndzj9h7t3",
    tvm_id_key="BIGB_RESHARDER_TVM_ID",
    tvm_secret_key="BIGB_RESHARDER_TVM_SECRET",
    default_stand=9,
    ylock_lock={"name": "bigb-testdata-buzzard-sampler"},
    sandbox_owner="BSYETI",
    sandbox_resource_type="RESHARDER_TEST_DATA",
    logbroker_consumer="/bigb/test/sampler-consumer",
    logbroker_perm_requirements=(
        Config.PermRequirement("svc_bigb@staff", "abc", Config.LOGBROKER_MANAGEMENT_PERMS),
        Config.PermRequirement("2001431@tvm", "tvm", Config.LOGBROKER_SERVICE_PERMS),
        Config.PermRequirement("2001433@tvm", "tvm", Config.LOGBROKER_SERVICE_PERMS),
    ),
    resource_file_path="ads/bsyeti/tests/resharder/new_b2b/ya.make.inc",
    source_settings={},
    unsupported_sources=[],
)

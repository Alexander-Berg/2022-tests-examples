# proto-file: ci/proto/storage/storage_api.proto
# proto-message: LargeTestJob

id {
    iteration_id {
        check_id: "1"
        check_type: HEAVY
    }
}
title: "Large Right"
right: true
precommit: true
target: "a/b/c"
test_info: "{\"toolchain\":\"chain-2\",\"tags\":[\"t\"],\"suite_name\":\"java\",\"suite_id\":\"02f327ca347f486b087713a01b51e115\",\"suite_hid\":9223372036854775809,\"requirements\":{},\"owners\":{},\"size\":\"large\"}"
test_info_source: {
    toolchain: "chain-2"
    tags: ["t"]
    suite_name: "java"
    suite_id: "02f327ca347f486b087713a01b51e115"
    suite_hid: 9223372036854775809
}
check_task_type: CTT_LARGE_TEST
arcadia_url: "arcadia-arc:/#right"
arcadia_base: "400"
arcadia_patch: "zipatch:https://zipatch"
distbuild_priority {
    priority_revision: 123
}

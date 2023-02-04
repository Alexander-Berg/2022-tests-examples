name = "billing-scheduler-test"
env = "PRESTABLE"
description = "Cluster for newbilling scheduler component testing environment"
pgversion = 13
resource_preset = "s2.micro"
disk_size = 24
dbname = "schedulerdb"
dbowner = "scheduler"
username = "scheduler"
hosts = [
    {
        zone = "vla",
        priority = 5
    },
    {
        zone = "sas",
        priority = 5
    },
]
pgconfig = {
    shared_preload_libraries = "1" # auto_explain enum
    auto_explain_log_min_duration = 5000
    auto_explain_log_analyze = true
    auto_explain_log_buffers = true
    auto_explain_log_nested_statements = true
    auto_explain_sample_rate = 0.01
}

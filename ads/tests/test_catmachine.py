from __future__ import print_function

import yatest.common

import yt.wrapper as yt


def get_binary(src, dst, yt_server, namespaces, prefix, cluster_field, target_field):
    cmd = [yatest.common.binary_path("ads/factor_check/catmachine/calc_namespaces/calc_namespaces")]
    cmd.extend(['--yt-server', yt_server])
    cmd.extend(['--dst-table-settings-erasure-codec', 'none'])
    cmd.extend(['--src-table', src])
    cmd.extend(['--dst-table', dst])
    cmd.extend(['--cluster-field', cluster_field])
    cmd.extend(['--target-field', target_field])
    cmd.extend(['--namespace-prefix', prefix])
    for ns in namespaces:
        cmd.extend(['--namespaces', ns])
    return cmd


def test_calcers(yt_stuff):
    client = yt_stuff.get_yt_client()

    namespaces = [
        "Categories",
        "ClickedCategories",
        "CryptaSegments",
        "DeviceFeatures",
        "Goals",
        "InstalledSoft",
        "STCategories",
        "STWords",
        "Socdem",
        "Words",
        "WeightedCategoryProfiles",
        "Location"
    ]

    schema = [
        {"name": "UserID", "type": "string"},
        {"name": "Cluster", "type": "uint64"},
        {"name": "Target", "type": "uint32"},
        {"name": "TimeStamp", "type": "int64"}
    ]
    for ns in namespaces:
        schema.append({"name": ns, "type": "string"})

    path = yt.TablePath(
        "//features_table",
        schema=schema,
    )
    write_format = yt.JsonFormat(encoding=None, encode_utf8=False)
    client.write_table(path, open("./yt/features_table.json"), format=write_format, raw=True)
    cmd = get_binary(
        src="//features_table",
        dst="//dst",
        yt_server=yt_stuff.get_server(),
        namespaces=namespaces,
        prefix="Cross",
        cluster_field="Cluster",
        target_field="Target"
    )
    yatest.common.execute(cmd)

    read_format = yt.YsonFormat(attributes={"format": "text"})
    res = client.read_table("//dst", format=read_format, raw=True).read()
    with open('records.yson', 'w') as out:
        out.write(res)
    return yatest.common.canonical_file(
        "records.yson",
        diff_tool=[
            yatest.common.binary_path("quality/ytlib/tools/canonical_tables_diff/bin/canonical_tables_diff")
        ],
        diff_tool_timeout=600
    )

from maps.routing.matrix_router.data_preparation.bin.prepare_slices.lib\
    .clustering import perform_clustering
from maps.routing.matrix_router.data_preparation.pytests.test_clustering\
    .bindings import prepare_slices_wrapper


def test_prepare_slices_small():
    routes_table = "data/small_routes.yson"
    time_parts_table = "data/small_time_parts.yson"

    etas = prepare_slices_wrapper(routes_table, time_parts_table, 10.0, 10, 100, 120)
    assert etas == [
        [19.0, 13.0, 7.0],
        [13.0, 19.0, 7.0],
        [29.0, 19.0, 19.0],
        [30.0, 20.0, 19.0],
        [13.0, 19.0, 10.0],
    ]

    config = perform_clustering(etas=etas, clusters_count=3, time_part_length=900, parts_directory="dir")
    assert config["version"] == 2
    clusters = config["slices"]
    part_paths = [cluster["yt_table"] for cluster in clusters]
    assert part_paths == [
        "dir/part_0000",
        "dir/part_0001",
        "dir/part_0002",
    ]
    time_parts = [cluster["time_parts"] for cluster in clusters]
    assert time_parts == [
        [['thu', 0, 899]],
        [['thu', 900, 1799], ['thu', 3600, 4499]],
        [['thu', 1800, 3599]],
    ]

    config = perform_clustering(etas=etas, clusters_count=2, time_part_length=900, parts_directory="dir/")
    assert config["version"] == 2
    clusters = config["slices"]
    part_paths = [cluster["yt_table"] for cluster in clusters]
    assert part_paths == [
        "dir/part_0000",
        "dir/part_0001",
    ]
    time_parts = [cluster["time_parts"] for cluster in clusters]
    assert time_parts == [
        [['thu', 0, 1799], ['thu', 3600, 4499]],
        [['thu', 1800, 3599]],
    ]

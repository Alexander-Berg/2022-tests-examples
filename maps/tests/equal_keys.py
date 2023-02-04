from maps.garden.sdk.core import Version, Resource


def test_file_resource():
    v1 = Version(properties={"a": 1, "b": 1})
    v2 = Version(properties={"a": 1, "b": 2})
    r1 = Resource("one_file", "important_file_{a}.path")
    r2 = Resource("another_file", "important_file_{a}.path")
    assert r1.calculate_key(version=v1) != r2.calculate_key(version=v1)
    assert r1.calculate_key(version=v1) != r1.calculate_key(version=v2)
    assert r1.calculate_key(version=v1, contour_name="test") != r1.calculate_key(version=v1)

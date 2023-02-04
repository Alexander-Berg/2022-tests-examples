import shutil
import os
import pytest
import tempfile
import random

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock


@pytest.fixture
def folder():
    with tempfile.TemporaryDirectory() as tmp:
        yield tmp


def test_path_join():
    adapter = CypressAdapterMock()
    assert adapter.path_join("//home", "alxmopo3ov", "table1") == os.path.join("//home", "alxmopo3ov", "table1")
    assert adapter.path_join("//home", "alxmopo3ov", "@attr") == os.path.join("//home", "alxmopo3ov", "@attr")


@pytest.mark.asyncio
async def test_file_not_exists(folder):
    adapter = CypressAdapterMock()
    assert not await adapter.exists(os.path.join(folder, "f1"))


@pytest.mark.asyncio
async def test_attribute_not_exists(folder):
    adapter = CypressAdapterMock()
    assert not await adapter.exists(os.path.join(folder, "f1/@row_count"))


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_create(folder, method):
    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "f1")
    await getattr(adapter, method)(fpath)
    assert await adapter.exists(fpath)
    if method == 'create_directory':
        assert os.path.isdir(fpath)
    else:
        assert os.path.isfile(fpath)

    # test fail create again
    # FIXME: right now CypressFileSystemAdapter implements force strategy, so Mock does. We must change this and change test.
    await adapter.create_file(fpath)
    await adapter.create_table(fpath)
    await adapter.create_directory(fpath)


@pytest.mark.asyncio
async def test_temp_directory():
    adapter = CypressAdapterMock()
    async with adapter.temporary_directory() as tmp:
        assert os.path.isdir(tmp)
        assert await adapter.exists(tmp)
    assert not await adapter.exists(tmp)


@pytest.mark.asyncio
async def test_rmtree(folder):
    adapter = CypressAdapterMock()
    # test fail on file
    fpath = adapter.path_join(folder, "ff1")
    await adapter.create_file(fpath)
    with pytest.raises(Exception):
        await adapter.rmtree(fpath)
    assert await adapter.exists(fpath)

    # test ok on dir
    fpath = adapter.path_join(folder, "ff2")
    await adapter.create_directory(fpath)
    assert await adapter.exists(fpath)
    await adapter.rmtree(fpath)
    assert not await adapter.exists(fpath)

    # test fail not exists
    fpath = adapter.path_join(folder, "ff3")
    assert not await adapter.exists(fpath)
    with pytest.raises(Exception):
        await adapter.rmtree(fpath)


@pytest.mark.asyncio
async def test_remove(folder):
    adapter = CypressAdapterMock()
    # test ok on file
    fpath = adapter.path_join(folder, "ff1")
    await adapter.create_file(fpath)
    assert await adapter.exists(fpath)
    await adapter.remove(fpath)
    assert not await adapter.exists(fpath)

    # test fail on dir
    fpath = adapter.path_join(folder, "ff2")
    await adapter.create_directory(fpath)
    assert await adapter.exists(fpath)
    with pytest.raises(Exception):
        await adapter.remove(fpath)
    assert await adapter.exists(fpath)

    # test fail not exists
    fpath = adapter.path_join(folder, "ff3")
    assert not await adapter.exists(fpath)
    with pytest.raises(Exception):
        await adapter.remove(fpath)


@pytest.mark.asyncio
async def test_listdir(folder):
    adapter = CypressAdapterMock()
    folder = adapter.path_join(folder, "f1")

    # not exists test
    with pytest.raises(Exception):
        await adapter.listdir(folder)

    # empty dir
    await adapter.create_directory(folder)
    lst = await adapter.listdir(folder)
    assert not len(lst)

    # filled dir
    await adapter.create_file(adapter.path_join(folder, "file1"))
    await adapter.create_file(adapter.path_join(folder, "file2"))
    lst = await adapter.listdir(folder)
    assert sorted(lst) == ["file1", "file2"]


# cat tests
# bytes(bytearray(random.getrandbits(8) for _ in range(1000000)))


##################################################
#               CONCATENATE TESTS                #
##################################################

# FIXME: test that you cant concat file and table


@pytest.mark.asyncio
async def test_cat_ok(folder):
    adapter = CypressAdapterMock()
    dst_path = adapter.path_join(folder, "dst")
    files = [adapter.path_join(folder, "file1"), adapter.path_join(folder, "file2")]
    with open(files[0], "wb") as f:
        f.write(b"123")
    with open(files[1], "wb") as f:
        f.write(b"456")
    await adapter.concatenate([adapter.path_join(folder, "file1"), adapter.path_join(folder, "file2")], dst_path)
    assert await adapter.exists(dst_path)
    with open(dst_path, "rb") as f:
        res = f.read()
    assert res == b"123456"


@pytest.mark.asyncio
async def test_cat_ok_overwrite(folder):
    adapter = CypressAdapterMock()
    dst_path = adapter.path_join(folder, "dst")
    files = [adapter.path_join(folder, "file1"), adapter.path_join(folder, "file2")]
    with open(files[0], "wb") as f:
        f.write(b"123")
    with open(files[1], "wb") as f:
        f.write(b"456")
    with open(dst_path, "wb") as f:
        f.write(b"275273582")
    await adapter.concatenate([adapter.path_join(folder, "file1"), adapter.path_join(folder, "file2")], dst_path)
    assert await adapter.exists(dst_path)
    with open(dst_path, "rb") as f:
        res = f.read()
    assert res == b"123456"


@pytest.mark.asyncio
async def test_cat_not_exists(folder):
    adapter = CypressAdapterMock()
    files = [adapter.path_join(folder, str(i)) for i in range(3)]
    await adapter.create_file(files[0])
    await adapter.create_file(files[1])
    assert not await adapter.exists(files[-1])
    dst_path = adapter.path_join(folder, "dst")
    with pytest.raises(Exception):
        await adapter.concatenate(files, adapter.path_join(folder, "dst"))
    assert not await adapter.exists(dst_path)


@pytest.mark.asyncio
async def test_cat_empty(folder):
    adapter = CypressAdapterMock()
    dst_path = adapter.path_join(folder, "dst")
    with open(dst_path, "wb") as f:
        f.write(b"12345")
    await adapter.concatenate([], dst_path)
    assert await adapter.exists(dst_path)
    with open(dst_path, "rb") as f:
        res = f.read()
    assert res == b""


@pytest.mark.asyncio
async def test_cat_same_file(folder):
    adapter = CypressAdapterMock()
    dst_path = adapter.path_join(folder, "dst")
    files = [adapter.path_join(folder, "file1"), adapter.path_join(folder, "file2")]
    with open(files[0], "wb") as f:
        f.write(b"123")
    with open(files[1], "wb") as f:
        f.write(b"456")
    with open(dst_path, "wb") as f:
        f.write(b"789")
    await adapter.concatenate([dst_path] + files, dst_path)
    assert await adapter.exists(dst_path)
    with open(dst_path, "rb") as f:
        res = f.read()
    assert res == b"789123456"


##################################################
#               ATTRIBUTES TESTS                 #
##################################################


"""
get/set have special meaning (as long as special implementation) so they are
tested separately
"""


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_bad_attribute(folder, method):
    adapter = CypressAdapterMock()
    dirname = os.path.join(folder, "d1")
    await getattr(adapter, method)(dirname)
    with pytest.raises(ValueError):
        await adapter.set(os.path.join(dirname, "trololo"), 1)


@pytest.mark.asyncio
async def test_set_attribute_non_existent_node(folder):
    adapter = CypressAdapterMock()
    assert not await adapter.exists(os.path.join(folder, "f1", "@row_count"))
    with pytest.raises(Exception):
        await adapter.set(os.path.join(folder, "f1", "@row_count"), 1)
    assert not await adapter.exists(os.path.join(folder, "f1", "@row_count"))
    assert not await adapter.exists(os.path.join(folder, "f1"))


@pytest.mark.asyncio
async def test_get_non_existent(folder):
    adapter = CypressAdapterMock()
    assert not await adapter.exists(os.path.join(folder, "f1", "@row_count"))
    with pytest.raises(Exception):
        await adapter.get(os.path.join(folder, "f1", "@row_count"))
    await adapter.create_file(os.path.join(folder, "f1"))
    with pytest.raises(Exception):
        await adapter.get(os.path.join(folder, "f1", "@row_count"))


@pytest.mark.asyncio
async def test_get_set(folder):
    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "ff1")
    await adapter.create_file(os.path.join(folder, "ff1"))
    await adapter.set(os.path.join(fpath, "@rtc"), 1)
    assert await adapter.exists(os.path.join(fpath, "@rtc"))
    assert await adapter.get(os.path.join(fpath, "@rtc")) == 1
    await adapter.set(os.path.join(fpath, "@rtc"), "105")
    assert await adapter.get(os.path.join(fpath, "@rtc")) == "105"


@pytest.mark.asyncio
async def test_remove_attr(folder):
    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "ff1")
    await adapter.create_file(os.path.join(folder, "ff1"))
    await adapter.set(os.path.join(fpath, "@rtc"), 1)
    assert await adapter.exists(os.path.join(fpath, "@rtc"))
    await adapter.remove(os.path.join(fpath, "@rtc"))
    assert not await adapter.exists(os.path.join(fpath, "@rtc"))
    with pytest.raises(Exception):
        await adapter.get(os.path.join(fpath, "@rtc"))


@pytest.mark.asyncio
async def test_set_multiple_attrs(folder):
    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "ff1")
    await adapter.create_file(os.path.join(folder, "ff1"))
    await adapter.set(os.path.join(fpath, "@rtc"), 1)
    await adapter.set(os.path.join(fpath, "@rtc2"), 2)
    await adapter.set(os.path.join(fpath, "@rtc3"), 3)

    assert await adapter.get(os.path.join(fpath, "@rtc")) == 1
    assert await adapter.get(os.path.join(fpath, "@rtc2")) == 2
    assert await adapter.get(os.path.join(fpath, "@rtc3")) == 3

    await adapter.set(os.path.join(fpath, "@rtc2"), "105")
    assert await adapter.get(os.path.join(fpath, "@rtc")) == 1
    assert await adapter.get(os.path.join(fpath, "@rtc2")) == "105"
    assert await adapter.get(os.path.join(fpath, "@rtc3")) == 3


@pytest.mark.asyncio
async def test_nested_yson_structure_attr(folder):
    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "ff1")
    await adapter.create_directory(os.path.join(folder, "ff1"))
    attr_value = {"1": 2, "b": "123", "c": {"1": [6, 7, 8]}}
    await adapter.set(os.path.join(fpath, "@rtc"), attr_value)
    assert await adapter.get(os.path.join(fpath, "@rtc")) == attr_value


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_get_set_no_side_effect(folder, method):
    adapter = CypressAdapterMock()
    fpath = adapter.path_join(folder, "ff1")
    await getattr(adapter, method)(fpath)
    attr_path = adapter.path_join(fpath, "@attr1")
    dct = {"a": 1, "b": 2}
    await adapter.set(attr_path, dct)
    assert await adapter.get(os.path.join(fpath, "@attr1")) == {"a": 1, "b": 2}
    dct["a"] = 100500
    assert await adapter.get(os.path.join(fpath, "@attr1")) == {"a": 1, "b": 2}

    get_dct = await adapter.get(os.path.join(fpath, "@attr1"))
    get_dct["a"] = 100500
    assert await adapter.get(os.path.join(fpath, "@attr1")) == {"a": 1, "b": 2}


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_create_with_attrs(folder, method):
    attr_dict = dict(
        attr1=1,
        attr2=dict(a="b", b="c")
    )

    adapter = CypressAdapterMock()
    fpath = os.path.join(folder, "ff1")
    await getattr(adapter, method)(fpath, attributes=attr_dict)
    assert await adapter.get(os.path.join(fpath, "@attr1")) == 1
    assert await adapter.get(os.path.join(fpath, "@attr2")) == {"a": "b", "b": "c"}
    # Check the side effects
    attr_dict["attr1"] = 100500
    attr_dict["attr2"]["b"] = 100500
    assert await adapter.get(os.path.join(fpath, "@attr1")) == 1
    assert await adapter.get(os.path.join(fpath, "@attr2")) == {"a": "b", "b": "c"}


# recursive attributes remove test


@pytest.mark.asyncio
async def test_recursive_remove_dir(folder):
    adapter = CypressAdapterMock()
    await adapter.create_directory(os.path.join(folder, "d1"), attributes=dict(a=1))
    await adapter.create_file(os.path.join(folder, "d1", "f1"), attributes=dict(b=2))
    await adapter.create_table(os.path.join(folder, "d1", "t1"), attributes=dict(c=3))

    # sanity check
    paths = [
        os.path.join(folder, "d1"),
        os.path.join(folder, "d1", "@a"),
        os.path.join(folder, "d1", "f1"),
        os.path.join(folder, "d1", "f1", "@b"),
        os.path.join(folder, "d1", "t1"),
        os.path.join(folder, "d1", "t1", "@c"),
    ]
    for path in paths:
        assert await adapter.exists(path)

    await adapter.rmtree(os.path.join(folder, "d1"))

    for path in paths:
        assert not await adapter.exists(path)


@pytest.mark.asyncio
async def test_recursive_remove_file(folder):
    adapter = CypressAdapterMock()
    await adapter.create_file(os.path.join(folder, "f1"), attributes=dict(b=2))
    await adapter.create_table(os.path.join(folder, "t1"), attributes=dict(c=3))

    # sanity check
    paths = [
        os.path.join(folder, "f1"),
        os.path.join(folder, "f1", "@b"),
        os.path.join(folder, "t1"),
        os.path.join(folder, "t1", "@c"),
    ]
    for path in paths:
        assert await adapter.exists(path)

    await adapter.remove(os.path.join(folder, "f1"))
    await adapter.remove(os.path.join(folder, "t1"))

    for path in paths:
        assert not await adapter.exists(path)


##################################################
#                  COPY TESTS                    #
##################################################


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_fail_copy_to_existing_node(method, folder):
    adapter = CypressAdapterMock()
    await getattr(adapter, method)(adapter.path_join(folder, "f1"), attributes=dict(attr1=dict(a=1)))
    await adapter.set(adapter.path_join(folder, "f1", "@attr2"), dict(c=3))
    await getattr(adapter, method)(adapter.path_join(folder, "f2"))

    with pytest.raises(ValueError):
        await adapter.copy(adapter.path_join(folder, "f1"), adapter.path_join(folder, "f2"))

    # test that attributes did not copied
    assert not await adapter.exists(adapter.path_join(folder, "f2", "@attr1"))
    assert not await adapter.exists(adapter.path_join(folder, "f2", "@attr2"))


@pytest.mark.parametrize('method', ['create_file', 'create_table'])
@pytest.mark.asyncio
async def test_single_node_copy(method, folder):
    adapter = CypressAdapterMock()
    await getattr(adapter, method)(adapter.path_join(folder, "f1"), attributes=dict(attr1=dict(a=1)))
    await adapter.set(adapter.path_join(folder, "f1", "@attr2"), dict(c=3))

    await adapter.copy(adapter.path_join(folder, "f1"), adapter.path_join(folder, "f2"))
    assert await adapter.get(adapter.path_join(folder, "f1", "@attr2")) == dict(c=3)
    assert await adapter.get(adapter.path_join(folder, "f1", "@attr1")) == dict(a=1)


async def test_copy_directory(folder):
    adapter = CypressAdapterMock()
    await adapter.create_directory(adapter.path_join(folder, "d1"), attributes=dict(attr1=dict(a=1)))
    await adapter.create_table(adapter.path_join(folder, "d1", "t1"), attributes=dict(c=3))
    await adapter.create_file(adapter.path_join(folder, "d1", "f1"), attributes=dict(b=6))

    await adapter.copy(adapter.path_join(folder, "d1"), adapter.path_join(folder, "d2"))

    # test recursive copy
    assert await adapter.exists(adapter.path_join(folder, "d2"))
    assert await adapter.get(adapter.path_join(folder, "d2", "@attr1")) == {"a": 1}
    assert await adapter.exists(adapter.path_join(folder, "d2", "t1"))
    assert await adapter.exists(adapter.path_join(folder, "d2", "f1"))
    assert await adapter.get(adapter.path_join(folder, "d2", "f1", "@b")) == 6
    assert await adapter.get(adapter.path_join(folder, "d2", "t1", "@c")) == 3

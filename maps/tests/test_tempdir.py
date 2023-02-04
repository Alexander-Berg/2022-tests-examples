import unittest
import os
import os.path as osp
import filecmp

from maps.pylibs.utils.lib.system import create_dir

from maps.garden.sdk.extensions.tempdir import copy_or_link, TempDir


def prepare(tempdir, path):
    rpath = osp.join(tempdir, path)
    create_dir(osp.dirname(rpath))
    return rpath


def create_file(tdir, path, content):
    f = open(prepare(tdir, path), "w")
    f.write(content)
    f.close()


def create_subdir(tdir, path):
    create_dir(prepare(tdir, path))


def create_link(tdir, path, content):
    os.symlink(content, prepare(tdir, path))


class PathsObject:
    def __init__(self, path, rel_path):
        self._path = path
        self._rel_path = rel_path

    def path(self):
        return self._path

    def relative_path(self):
        return self._rel_path


class TempDirCopyTest(unittest.TestCase):

    def _compare_dir(self, path1, path2):
        self.assertTrue(osp.isdir(path1))
        self.assertTrue(osp.isdir(path2))

        self.assertEqual(sorted(os.listdir(path1)), sorted(os.listdir(path2)))

        for name in os.listdir(path1):
            rpath1 = osp.join(path1, name)
            rpath2 = osp.join(path2, name)

            if osp.islink(rpath1):
                self.assertTrue(osp.islink(rpath2))
                self.assertEqual(os.readlink(rpath1), os.readlink(rpath2))

            elif osp.isfile(rpath1):
                self.assertTrue(osp.isfile(rpath2))
                self.assertTrue(filecmp.cmp(rpath1, rpath2, shallow=False))

            elif osp.isdir(rpath1):
                self._compare_dir(rpath1, rpath2)

    def _add_dir_content(self, tempdir):
        create_file(tempdir, "a/b/file.ext", "avc" * 1000)
        create_file(tempdir, "file2.ext", "rr" * 2000)
        create_link(tempdir, "rt/link1", "../aa")
        create_link(tempdir, "l2", "a/b/")
        create_subdir(tempdir, "asd/bbb")

    def test_copy(self):
        with TempDir() as dir1:
            with TempDir() as dir2:
                path1 = dir1.path()
                path2 = dir2.path()

                self.assertFalse(path1 == path2)
                self.assertTrue(osp.isdir(path1))
                self.assertTrue(osp.isdir(path2))
                self.assertFalse(os.listdir(path1))
                self.assertFalse(os.listdir(path2))
                self._compare_dir(path1, path2)

                self._add_dir_content(path1)
                copy_or_link(path1, path2)

                self._compare_dir(path1, path2)

        self.assertFalse(osp.exists(path1))
        self.assertFalse(osp.exists(path2))

    def test_add_resource(self):
        with TempDir() as dir1:

            subdir = os.path.join(dir1.path(), "subdir")
            os.mkdir(subdir)

            self._add_dir_content(subdir)

            relpath1 = "a/b/c"
            res1 = PathsObject(subdir, relpath1)

            subdir2 = os.path.join(dir1.path(), "subdir2")
            os.mkdir(subdir2)

            fname = "another_file1.ext"
            create_file(subdir2, fname, "aa1a" * 500)

            relpath2 = "r/t/another_res_file.ext"
            res2 = PathsObject(os.path.join(subdir2, fname), relpath2)

            with TempDir() as dir2:
                paths = dir2.add_resources(res1, res2)
                self._compare_dir(subdir, osp.join(dir2.path(), relpath1))

                self.assertTrue(filecmp.cmp(
                    os.path.join(subdir2, fname),
                    os.path.join(dir2.path(), relpath2),
                    shallow=False
                ))

                expected_paths = [
                    os.path.join(dir2.path(), relpath1),
                    os.path.join(dir2.path(), relpath2)
                ]

                self.assertEqual(paths, expected_paths)

    def test_exit_without_enter(self):
        t = TempDir()
        t.__exit__(None, None, None)


if __name__ == "__main__":
    unittest.main()

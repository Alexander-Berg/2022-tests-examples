#!/usr/bin/env python3

import unittest
from collections import namedtuple

from ccache_clang import make_flags_relative_to_path

TestArgs = namedtuple("TestArgs", ["cmd_flags", "cwd", "expected"])

class CcacheClangTest(unittest.TestCase):
    def check_relativize(self, flags_array):
        for flags in flags_array:
            actual = make_flags_relative_to_path(flags.cmd_flags, flags.cwd)
            self.assertEqual(actual, flags.expected,
                             msg="\n\nFAILED: make_flags_relative_to_path was called with: "
                             "({flags.cmd_flags}, '{flags.cwd}')\n"
                             "Actual:   {actual}\n"
                             "Expected: {flags.expected}".format(flags=flags, actual=actual))

    def test_include_flags(self):
        # pylint: disable=bad-whitespace
        self.check_relativize([
            TestArgs(["-I", "/a/b", "-I/b/c"], "/a", ["-I", "b", "-I../b/c"]),
            TestArgs(["-isystem", "/a/b"],     "/a", ["-isystem", "b"]),
            TestArgs(["-iquote", "/a/b"],      "/a", ["-iquote", "b"]),
        ])

    def test_include_pch(self):
        self.check_relativize([
            TestArgs(["-include", "/a/b.pch"], "/a", ["-include", "b.pch"]),
        ])

    def test_toolchain_flags(self):
        # pylint: disable=bad-whitespace
        self.check_relativize([
            TestArgs(["--sysroot=/a/b"],       "/a", ["--sysroot=b"]),
            TestArgs(["--gcc-toolchain=/a/b"], "/a", ["--gcc-toolchain=b"]),
        ])

    def test_various_flags_with_path_arguments(self):
        # pylint: disable=bad-whitespace
        self.check_relativize([
            TestArgs(["--serialize-diagnostics", "/a/b.dia"], "/a",
                     ["--serialize-diagnostics", "b.dia"]),
            TestArgs(["-fmodule-map-file=/a/m.modulemap"], "/a",
                     ["-fmodule-map-file=m.modulemap"]),
            TestArgs(["-index-store-path", "/a/b"], "/a", ["-index-store-path", "b"]),
            TestArgs(["-MF", "/a/b.d"],             "/a", ["-MF", "b.d"]),
        ])

    def test_keep_system_path(self):
        self.check_relativize([
            TestArgs(["-I/a/b", "-I/Applications/Xcode/usr/include"], "/a",
                     ["-Ib", "-I/Applications/Xcode/usr/include"]),
        ])

    def test_keep_sysroot(self):
        self.check_relativize([
            TestArgs(["-isysroot", "/Applications/Xcode/iphone-sdk"], "/a",
                     ["-isysroot", "/Applications/Xcode/iphone-sdk"]),
        ])

    def test_keep_ndk(self):
        self.check_relativize([
            TestArgs(["--sysroot=/opt/android-sdk-linux/ndk/21.0.6113669/toolchains/llvm"],
                     "/opt",
                     ["--sysroot=/opt/android-sdk-linux/ndk/21.0.6113669/toolchains/llvm"]),
        ])

    def test_input_file(self):
        # pylint: disable=bad-whitespace
        self.check_relativize([
            TestArgs(["-c", "-I/a/b", "/a/input.cpp"],   "/a", ["-c", "-Ib", "input.cpp"]),
            TestArgs(["-I/a/b", "-c", "/a/input.cpp"],   "/a", ["-Ib", "-c", "input.cpp"]),
            TestArgs(["-I/a/b", "-c", "/a/input.c"],     "/a", ["-Ib", "-c", "input.c"]),
            TestArgs(["-I/a/b", "-c", "/a/input.pch"],   "/a", ["-Ib", "-c", "input.pch"]),
            TestArgs(["-I/a/b", "-c", "/a/input.m"],     "/a", ["-Ib", "-c", "input.m"]),
            TestArgs(["-I/a/b", "-c", "/a/input.mm"],    "/a", ["-Ib", "-c", "input.mm"]),
            TestArgs(["-I/a/b", "-c", "/a/input.pb.cc"], "/a", ["-Ib", "-c", "input.pb.cc"]),
        ])

    def test_output_file(self):
        self.check_relativize([
            TestArgs(["-I/a/b", "-o", "/a/main.o"], "/a", ["-Ib", "-o", "main.o"]),
        ])

    def test_multiple_flags(self):
        # pylint: disable=bad-whitespace
        flags = [
            "-I/a/b/c",                         "-Ib/c",
            "-I/Applications/Xcode/include",    "-I/Applications/Xcode/include",
            "-fmodule-map-file=/a/m.modulemap", "-fmodule-map-file=m.modulemap",
            "-MF",                              "-MF",
            "/a/b.d",                           "b.d",
            "-c",                               "-c",
            "/a/src/input.cpp",                 "src/input.cpp",
            "-o",                               "-o",
            "/a/out/input.o",                   "out/input.o",
        ]
        # pylint: enable=bad-whitespace

        input_flags = flags[::2]
        output_flags = flags[1::2]

        self.check_relativize([
            TestArgs(input_flags, "/a", output_flags),
        ])

if __name__ == '__main__':
    unittest.main()

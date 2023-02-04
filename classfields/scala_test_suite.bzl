def _is_test(src, test_suffixes, nontest_classes):
    for suffix in test_suffixes:
        if src.endswith(suffix) and not src in nontest_classes:
            return True
    return False

# Common package prefixes, in the order we want to check for them
_PREFIXES = (".com.", ".org.", ".net.", ".io.", ".ai.", ".ru.")
_DIRS = ("src.main.scala", "src.test.scala", "src.main.java", "src.test.java", "src", "scala", "java")

# By default bazel computes the name of test classes based on the
# standard Maven directory structure, which we may not always use,
# so try to compute the correct package name.
def _get_package_name():
    pkg = native.package_name().replace("/", ".")

    for prefix in _PREFIXES:
        idx = pkg.find(prefix)
        if idx != -1:
            return pkg[idx + 1:] + "."

    return ""

# Converts a file name into what is hopefully a valid class name.
def _get_class_name(package, src):
    # Strip the suffix from the source
    idx = src.rindex(".")
    name = src[:idx].replace("/", ".")

    for dir in _DIRS:
        idx = name.find(dir)
        if idx != -1:
            return name[idx + len(dir) + 1:]

    for prefix in _PREFIXES:
        idx = name.find(prefix)
        if idx != -1:
            return name[idx + 1:]

    # Make sure that the package has a trailing period so it's
    # safe to add the class name. While `get_package_name` does
    # the right thing, the parameter passed by a user may not
    # so we shall check once we have `pkg` just to be safe.
    pkg = package if package else _get_package_name()
    if len(pkg) and not pkg.endswith("."):
        pkg = pkg + "."

    if pkg:
        return pkg + name
    return name

def scala_test_suite(
        name,
        srcs,
        test_suffixes,
        package,
        library_attributes,
        define_library,
        define_test,
        deps = None,
        nontest_files = [],
        runtime_deps = [],
        tags = [],
        visibility = None,
        size = None,
        **kwargs):
    """Generate a test suite for rules that "feel" like `java_test`.

    Given the list of `srcs`, this macro will generate:

      1. A `*_test` target per `src` that matches any of the `test_suffixes`
      2. A shared library that these tests depend on for any non-test `srcs`
      3. A `test_suite` tagged as `manual` that aggregates all the tests

    The reason for having a test target per test source file is to allow for
    better parallelization. Initial builds may be slower, but iterative builds
    while working with on unit tests should be faster, and this approach
    makes best use of the RBE.

    Args:
      name: The name of the generated test suite.
      srcs: A list of source files.
      test_suffixes: A list of suffixes (eg. `["Test.kt"]`)
      package: The package name to use. If `None`, a value will be
        calculated from the bazel package.
      library_attributes: Attributes to pass to `define_library`.
      define_library: A function that creates a `*_library` target.
      define_test: A function that creates a `*_test` target.
      runner: The junit runner to use. Either "junit4" or "junit5".
      deps: The list of dependencies to use when compiling.
      runtime_deps: The list of runtime deps to use when compiling.
      tags: Tags to use for generated targets.
      size: Bazel test size
    """

    # First, grab any interesting attrs
    library_attrs = {attr: kwargs[attr] for attr in library_attributes if attr in kwargs}

    test_srcs = [src for src in srcs if _is_test(src, test_suffixes, nontest_files)]
    nontest_srcs = [src for src in srcs if not _is_test(src, test_suffixes, nontest_files)]

    # Build a shared test library to use for everything. If we don't do this,
    # each rule needs to compile all sources, and that seems grossly inefficient.
    # Only include the non-test sources since we don't want all tests to re-run
    # when only one test source changes.
    library_name = "%s-test-lib" % name
    define_library(
        name = library_name,
        deps = deps,
        srcs = srcs,
        testonly = True,
        visibility = visibility,
        **library_attrs
    )

    tests = []

    for src in test_srcs:
        test_class = _get_class_name(package, src)
        test_name = test_class
        tests.append(test_name)

        define_test(
            name = test_name,
            size = size,
            srcs = [],
            args = ["-s", test_class],
            deps = [":" + library_name],
            tags = tags,
            runtime_deps = runtime_deps,
            visibility = ["//visibility:private"],
            **kwargs
        )

    native.test_suite(
        name = name,
        tests = tests,
        tags = ["manual"] + tags,
        visibility = visibility,
    )

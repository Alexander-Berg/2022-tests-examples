import yatest.common
import pytest
from bmtest.env import make_fs_root, make_merged_root, get_env


@pytest.fixture()
def unpack():
    return [make_fs_root(), make_merged_root()]


def tst_perl(fs_root, merged_root, metrics):
    cmd = [
        '{}/rt-research/broadmatching/scripts/tests/perlcritic.pl'.format(merged_root),
        '--include', 'ControlStructures::ProhibitMutatingListFunctions',
        '--include', 'ValuesAndExpressions::ProhibitMismatchedOperators',
        '--include', 'BuiltinFunctions::ProhibitSleepViaSelect',
        '--include', 'BuiltinFunctions::RequireGlobFunction',
        '--include', 'ClassHierarchies::ProhibitOneArgBless',
        '--include', 'Modules::ProhibitEvilModules',
        '--include', 'Modules::RequireBarewordIncludes',
        '--include', 'Modules::RequireFilenameMatchesPackage',
        '--include', 'ValuesAndExpressions::ProhibitLeadingZeros',
        '--include', 'Variables::ProhibitConditionalDeclarations',
        '--only',
        '{}/rt-research/broadmatching/scripts/lib/BM'.format(merged_root),
        '--quiet',
    ]
    yatest.common.execute(
        cmd,
        env=get_env(fs_root),
        check_exit_code=True
    )


def test_critic(unpack, metrics):
    tst_perl(unpack[0], unpack[1], metrics)

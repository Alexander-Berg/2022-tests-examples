import yatest.common
import json
import tempfile
import pytest


def get_binary(profiles, queries, desc, output):
    cmd = [yatest.common.binary_path("ads/factor_check/query_predictor/ut/test_applier/test_applier")]
    cmd.extend(['--output', output])
    cmd.extend(['--profiles-file', profiles])
    cmd.extend(['--queries-file', queries])
    cmd.extend(['--model-desc', desc])
    return cmd


@pytest.yield_fixture()
def dssm_config():
    with tempfile.NamedTemporaryFile(suffix='model.json') as f:
        json.dump(
            {
                "DssmModel": {
                    "LocalModelPath": "resources/dssm_5000"
                },
                "ProfileCalcerDesc": {
                    "CategoricalNamespaces": [
                        "Words",
                        "STWords",
                        "Categories",
                        "STCategories",
                        "Socdem",
                        "Location",
                        "DeviceFeatures",
                        "Goals",
                        "UrlSegments",
                        "WeightedCategoryProfiles",
                        "InstalledSoft",
                        "ClickedCategories",
                        "CryptaSegments"
                    ],
                    "ProfileCalcer": {
                        "WordsMapFile": "smth",
                        "LocalWordsMapFile": "./resources/BroadPhraseNorm.dict"
                    }
                }
            },
            f
        )
        f.flush()
        yield f.name


@pytest.yield_fixture()
def catmachine_config():
    with tempfile.NamedTemporaryFile(suffix='model.json') as f:
        json.dump(
            {
                "CatmachineModel": {
                    "LocalModelPath": "resources/catmachine_model",
                    "QueryNamespaces": [
                        "Cluster"
                    ]
                },
                "ProfileCalcerDesc": {
                    "CategoricalNamespaces": [
                        "Words",
                        "STWords",
                        "Categories",
                        "STCategories",
                        "Socdem",
                        "Location",
                        "DeviceFeatures",
                        "Goals",
                    ],
                    "ProfileCalcer": {
                        "WordsMapFile": "smth",
                        "LocalWordsMapFile": "./resources/BroadPhraseNorm.dict"
                    }
                }
            },
            f
        )
        f.flush()
        yield f.name


def test_dssm(dssm_config):
    cmd = get_binary(
        profiles="./resources/profiles",
        queries="./resources/queries",
        desc=dssm_config,
        output="output")
    yatest.common.execute(cmd)
    return yatest.common.canonical_file(
        "output",
        diff_tool=[
            yatest.common.binary_path("quality/ytlib/tools/canonical_tables_diff/bin/canonical_tables_diff"),
            "--json"
        ],
        diff_tool_timeout=600
    )


def test_catmachine(catmachine_config):
    cmd = get_binary(
        profiles="./resources/profiles",
        queries="./resources/queries",
        desc=catmachine_config,
        output="output")
    yatest.common.execute(cmd)
    return yatest.common.canonical_file(
        "output",
        diff_tool=[
            yatest.common.binary_path("quality/ytlib/tools/canonical_tables_diff/bin/canonical_tables_diff"),
            "--json"
        ],
        diff_tool_timeout=600
    )

import tempfile
import shutil
import yatest.common
import pytest
from ads.pytorch.lib.online_learning.production.processors.tsar_processor.lib.applicable_processor import ApplicableModelProcessor
from ads.pytorch.lib.online_learning.production.processors.tsar_processor.lib.deep_part_processor import DeepPartProcessor
from ads.pytorch.lib.online_learning.production.processors.tsar_processor.lib.hash_embeddings_processor import HashEmbeddingsProcessor


@pytest.mark.parametrize("model_name", ["UserNamespaces_test"])
@pytest.mark.parametrize("embeddding_builder_type", ["STHashEmbedding"])
@pytest.mark.parametrize("join_embeddings", [True, False])
@pytest.mark.parametrize("compression", ["float", "half"])
def test_applicable_processor(model_name, embeddding_builder_type, join_embeddings, compression):
    yt_model_path = model_name
    processor = ApplicableModelProcessor(
        deep_processor=DeepPartProcessor(),
        hash_embedding_processor=HashEmbeddingsProcessor(
            embeddding_builder_type, join_embeddings,
            {"MaxCreationTimeQueryWords": compression, "UserCryptaYandexLoyalty": compression}
        )
    )
    tempdir = tempfile.mkdtemp()
    try:
        processor.process(yt_model_path, tempdir)
        return yatest.common.canonical_dir(tempdir)
    finally:
        shutil.rmtree(tempdir)


@pytest.mark.parametrize("model_name", ["UserNamespaces_test"])
@pytest.mark.parametrize("embeddding_builder_type", ["STHashEmbedding"])
@pytest.mark.parametrize("join_embeddings", [True])
@pytest.mark.parametrize("compression", ["float"])
def test_applicable_processor_minify(model_name, embeddding_builder_type, join_embeddings, compression):
    yt_model_path = model_name
    processor = ApplicableModelProcessor(
        deep_processor=DeepPartProcessor(),
        hash_embedding_processor=HashEmbeddingsProcessor(
            embeddding_builder_type, join_embeddings,
            {"MaxCreationTimeQueryWords": compression, "UserCryptaYandexLoyalty": compression},
            max_count=1000
        )
    )
    tempdir = tempfile.mkdtemp()
    try:
        processor.process(yt_model_path, tempdir)
        return yatest.common.canonical_dir(tempdir)
    finally:
        shutil.rmtree(tempdir)

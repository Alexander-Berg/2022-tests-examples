import tempfile
import shutil
import yatest.common
import pytest
from ads.pytorch.lib.online_learning.production.processors.tsar_processor.lib.hash_embeddings_processor import HashEmbeddingsProcessor


@pytest.mark.parametrize("embeddding_builder_type", ["STHashEmbedding"])
@pytest.mark.parametrize("join_embeddings", [True, False])
@pytest.mark.parametrize("compression", ["float", "half", "uint8_length"])
def test_hash_embedding_processing(embeddding_builder_type, join_embeddings, compression):
    yt_model_path = "UserNamespaces_test/hash_embeddings"
    processor = HashEmbeddingsProcessor(
        embeddding_builder_type,
        join_embeddings,
        {"MaxCreationTimeQueryWords": compression, "UserCryptaYandexLoyalty": compression}
    )
    tempdir = tempfile.mkdtemp()
    try:
        processor.process(yt_model_path, tempdir)
        return yatest.common.canonical_dir(tempdir)
    finally:
        shutil.rmtree(tempdir)


@pytest.mark.parametrize("embeddding_builder_type", ["STHashEmbedding"])
@pytest.mark.parametrize("join_embeddings", [True])
@pytest.mark.parametrize("compression", ["float"])
def test_hash_embedding_processing_minify(embeddding_builder_type, join_embeddings, compression):
    yt_model_path = "UserNamespaces_test/hash_embeddings"
    processor = HashEmbeddingsProcessor(
        embeddding_builder_type,
        join_embeddings,
        {"MaxCreationTimeQueryWords": compression, "UserCryptaYandexLoyalty": compression},
        max_count=1000
    )
    tempdir = tempfile.mkdtemp()
    try:
        processor.process(yt_model_path, tempdir)
        return yatest.common.canonical_dir(tempdir)
    finally:
        shutil.rmtree(tempdir)

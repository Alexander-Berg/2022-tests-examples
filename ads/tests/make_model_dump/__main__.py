from ads.nirvana.automl.pipelines.mlmarines.deploy_v2 import convert_deep_embedding_model_to_cpp_format

import os

if __name__ == '__main__':
    convert_deep_embedding_model_to_cpp_format(
        model_path="./model_dump/user",
        path_type="local",
        result_folder="./user",
        yt_client=None
    )

    convert_deep_embedding_model_to_cpp_format(
        model_path="./model_dump/document",
        path_type="local",
        result_folder="./document",
        yt_client=None
    )

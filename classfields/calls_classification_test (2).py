import vh3
from typing import Literal, NamedTuple, Optional, Sequence, TypedDict, Union

from ops import *
from queries import COLLECT_CALLS_FOR_CLS_TEST_TEMPLATE, ADD_NO_SCRIPT_TEXTS_TEST_TEMPALTE, \
    HELPER_TEST_TEMPALTE, LOAD_RESULT_TEST_TEMPLATE, BRANDS_DICT


@vh3.decorator.graph(release_workflow='df90182d-4afe-4a3d-8fc0-a162a31585fa')
@vh3.decorator.nirvana_names_transformer(vh3.name_transformers.snake_to_dash, options=True)
def main(
    *,
    mr_account: vh3.String = "verticals/.tmp",
    yql_operation_title: vh3.String = "YQL Nirvana Operation, VSML-1003, Call Clustering: {{nirvana_operation_url}}",
    calls_directory: vh3.String = "home/verticals/broker/test/warehouse/dust/nirvana_clustering/call_scenario_realty/1h",
    yt_pool: vh3.String = "verticals",
    yt_token: vh3.Secret = "robot_vertis_ml_yt_token",
    yql_token: vh3.Secret = "robot_vertis_ml_yql_token",
    mr_output_path: vh3.String = "//home/verticals/.tmp",
    path2result: vh3.String = "//home/verticals/dust/clustering_results/test/",
    table_with_result_latest: vh3.String = "home/verticals/vsml/autoru_call_clusterization/classification_realty_test_result_latest",
    bucket_name: vh3.String = 'realty',
    vectorizer_s3_path: vh3.String = 'calls_clustering/script_detection/script_call_detection_vectorizer.joblib',
    classifier_s3_path: vh3.String = 'calls_clustering/script_detection/script_call_detection_clf.joblib',
) -> None:
    """
    realty_calls_classification[prod]

    :param mr_account: MR Account:
      [[MR Account Name.
    By default, output tables and directories will be created in some subdirectory of home/<MR Account>/<workflow owner>/nirvana]]
    :param yql_operation_title: YQL Operation title
      [[YQL operation title for monitoring]]
    :param calls_directory: Calls directory
    :param yt_pool: YT Pool:
      [[Pool used by YT scheduler. Leave blank to use default pool.
    This option has no effect on YaMR.]]
    :param yt_token: YT Token:
      [[ID of Nirvana Secret with YT access token (https://nda.ya.ru/3RSzVU).
    Guide to Nirvana Secrets: https://nda.ya.ru/3RSzWZ]]
    :param yql_token: YQL Token:
      [[YQL OAuth Token, see https://wiki.yandex-team.ru/kikimr/yql/userguide/cli/#autentifikacija]]
    :param mr_output_path: MR Output Path:
      [[Directory for output MR tables and directories.
    Limited templating is supported: `${param["..."]}`, `${meta["..."]}`, `${mr_input["..."]}` (path to input MR *directory*) and `${uniq}` (= unique path-friendly string).]]
    """
    get_target_arcadia_revision_result: vh3.JSON = get_arcadia_revision(
        path=vh3.Expr("trunk/arcadia/classifieds/vsml/infrastructure/calls_clusterization_common/scripts/"),
        output_json_key="revision",
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=False, source_block=None),
            code="get_arcadia_revision",
            name="Get target arcadia revision"
        )
    )
    svn_checkout_target_result: SvnCheckoutDeterministicOutput = svn_checkout_deterministic(
        arcadia_path=vh3.Expr("classifieds/vsml/infrastructure/calls_clusterization_common/scripts/"),
        revision=9554364,
        path_prefix="trunk/arcadia",
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=False, source_block=None),
            code="svn_checkout_deterministic_2",
            dynamic_options=get_target_arcadia_revision_result,
            name="SVN: Checkout target"
        )
    )
    extract_target_result: ExtractFromTarOutput = extract_from_tar(
        archive=svn_checkout_target_result.archive,
        path=vh3.Expr("script_detection.py"),
        out_type="text",
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=True, source_block=None),
            code="extract_from_tar",
            name="Extract target"
        )
    )

    download_classifier_result: MdsS3DownloadFileWithMetaOutput = mds_s3_download_file_with_meta(
        endpoint_url="http://s3.mdst.yandex.net",
        aws_access_key_id="vertis_mdst_s3_access_key_id",
        aws_secret_access_key="vertis_mdst_s3_secret_access_key",
        bucket=bucket_name,
        file_path=vh3.Expr(classifier_s3_path),
        **vh3.block_args(
            code="MDS_S3_Download_file_with_meta_10", name="Download Classifier"
        )
    )
    
    download_vectorizer_result: MdsS3DownloadFileWithMetaOutput = mds_s3_download_file_with_meta(
        endpoint_url="http://s3.mdst.yandex.net",
        aws_access_key_id="vertis_mdst_s3_access_key_id",
        aws_secret_access_key="vertis_mdst_s3_secret_access_key",
        bucket=bucket_name,
        file_path=vh3.Expr(vectorizer_s3_path),
        **vh3.block_args(
            code="MDS_S3_Download_file_with_meta_9", name="Download Vectorizer"
        )
    )
    get_mr_directory_result: vh3.MRDirectory = get_mr_directory(
        cluster="hahn",
        path=calls_directory,
        yt_token="robot_vertis_ml_yt_token",
        **vh3.block_args(code="Get_MR_Directory_2", name="Get MR Directory")
    )
    get_last_mr_table_result: vh3.MRTable = get_last_mr_table(
        path=get_mr_directory_result,
        yt_token="robot_vertis_ml_yt_token",
        mr_account=mr_account,
        cluster="hahn",
        **vh3.block_args(code="Get_last_MR_Table_2", name="Get last MR Table")
    )
    collect_calls_for_classification_result: Yql1Output = yql_1(
        input1=[get_last_mr_table_result],
        request=vh3.Expr(COLLECT_CALLS_FOR_CLS_TEST_TEMPLATE),
        mr_account=mr_account,
        yt_pool=yt_pool,
        yt_token=yt_token,
        yql_token=yql_token,
        mr_output_path=mr_output_path,
        **vh3.block_args(code="yql_6", name="Collect Calls For Classification")
    )
    mr_read_tsv_result: vh3.TSV = mr_read_tsv(
        table=collect_calls_for_classification_result.output1,
        yt_token="robot_vertis_ml_yt_token",
        columns=("call_id", "source", "cluster_type", "domain", "external_dialog_id"),
        **vh3.block_args(code="mr_read_tsv_2", name="MR Read TSV")
    )
    add_tsv_header_result: vh3.TSV = add_tsv_header(
        file=mr_read_tsv_result,
        columns=("call_id", "source", "cluster_type", "domain", "external_dialog_id"),
        **vh3.block_args(code="add_tsv_header_2", name="Add tsv header")
    )

    detect_scripts_result: vh3.TSV = detect_scripts(
        vectorizer=download_vectorizer_result.file,
        classifier=download_classifier_result.file,
        calls_table=add_tsv_header_result,
        source_code=extract_target_result.text_file,
        max_ram=2048,
        call_text_column_name="source",
        min_text_length=30,
        **vh3.block_args(code="detect_scripts", name="Detect Scripts")
    )

    filter_scripts_result: vh3.TSV = bash_pipeline(
        input=[detect_scripts_result],
        script="awk '$NF != 0 { print $0 }' input > output",
        **vh3.block_args(code="bash_pipeline_2", name="Filter Scripts")
    )
    download_current_version_info_result: MdsS3DownloadFileWithMetaOutput = mds_s3_download_file_with_meta(
        endpoint_url="http://s3.mdst.yandex.net",
        aws_access_key_id="vertis_mdst_s3_access_key_id",
        aws_secret_access_key="vertis_mdst_s3_secret_access_key",
        bucket=bucket_name,
        file_path=vh3.Expr("calls_clustering/current/version_info.json"),
        **vh3.block_args(
            code="MDS_S3_Download_file_with_meta_7",
            name="Download Current Version Info"
        )
    )
    download_vectorizer_result1: MdsS3DownloadFileWithMetaOutput = mds_s3_download_file_with_meta(
        endpoint_url="http://s3.mdst.yandex.net",
        aws_access_key_id="vertis_mdst_s3_access_key_id",
        aws_secret_access_key="vertis_mdst_s3_secret_access_key",
        bucket=bucket_name,
        file_path=vh3.Expr("calls_clustering/current/vectorizer.joblib"),
        **vh3.block_args(
            code="MDS_S3_Download_file_with_meta_8", name="Download Vectorizer"
        )
    )
    download_cluster_info_result: MdsS3DownloadFileWithMetaOutput = mds_s3_download_file_with_meta(
        endpoint_url="http://s3.mdst.yandex.net",
        aws_access_key_id="vertis_mdst_s3_access_key_id",
        aws_secret_access_key="vertis_mdst_s3_secret_access_key",
        bucket=bucket_name,
        file_path=vh3.Expr("calls_clustering/current/cluster_info.json"),
        **vh3.block_args(
            code="MDS_S3_Download_file_with_meta_4", name="Download Cluster Info"
        )
    )
    helper_result: Yql1Output = yql_1(
        input1=[get_last_mr_table_result],
        request=vh3.Expr(HELPER_TEST_TEMPALTE),
        mr_account=mr_account,
        yt_pool=yt_pool,
        yt_token=yt_token,
        yql_token=yql_token,
        mr_output_path=mr_output_path,
        mr_output_ttl=5,
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=False, source_block=None),
            code="yql_3",
            name="helper"
        )
    )
    binary_to_json_result: vh3.JSON = binary_to_json(
        in_=download_current_version_info_result.file,
        **vh3.block_args(code="binary_to_json_4", name="Binary to json")
    )
    binary_to_json_result1: vh3.JSON = binary_to_json(
        in_=download_cluster_info_result.file,
        **vh3.block_args(code="binary_to_json_3", name="Binary to json")
    )
    mr_write_tsv_create_new_result: vh3.MRTable = mr_write_tsv_create_new(
        tsv=detect_scripts_result,
        header=True,
        yt_token="robot_vertis_ml_yt_token",
        mr_account=mr_account,
        yt_pool=yt_pool,
        mr_default_cluster="hahn",
        columns=("call_id", "source", "cluster_type", "domain", "external_dialog_id", "is_script"),
        **vh3.block_args(
            code="mr_write_tsv_create_new_3", name="MR Write TSV (Create New)"
        )
    )
    add_no_sctipt_texts_result: Yql1Output = yql_1(
        input1=[mr_write_tsv_create_new_result],
        request=vh3.Expr(ADD_NO_SCRIPT_TEXTS_TEST_TEMPALTE),
        mr_account=mr_account,
        yt_pool=yt_pool,
        yt_token=yt_token,
        yql_token=yql_token,
        mr_output_path=mr_output_path,
        mr_output_ttl=5,
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=False, source_block=None),
            code="yql_10",
            name="add no_sctipt texts"
        )
    )
    load_table_with_result_result: vh3.MRTable = get_mr_table(
        cluster="hahn",
        table=vh3.Expr(table_with_result_latest),
        yt_token="robot_vertis_ml_yt_token",
        **vh3.block_args(
            cache_options=vh3.graph.execution_block.BlockCacheOptions(enable_cache=False, source_block=None),
            code="get_mr_table_4",
            name="load table with result"
        )
    )
    brands_result: vh3.JSON = single_option_to_json_output(
        BRANDS_DICT,
        **vh3.block_args(code="single_option_to_json_output_5", name="Brands")
    )
    predict_cluster_result: PredictClusterOutput = predict_cluster(
        calls_table=filter_scripts_result,
        vectorizer=download_vectorizer_result1.file,
        cluster_version_info=binary_to_json_result,
        cluster_info=binary_to_json_result1,
        brands=brands_result,
        cpu_guarantee=800,
        threshold="0.9",
        n_threads=8,
        **vh3.block_args(code="predict_cluster_3", name="Predict Cluster")
    )
    buidl_result_result: MrWriteJsonOutput = mr_write_json(
        dst_table=load_table_with_result_result,
        json=predict_cluster_result.result_json,
        yt_token=yt_token,
        write_mode="OVERWRITE",
        mr_account=mr_account,
        **vh3.block_args(code="mr_write_json_3", name="buidl result")
    )
    load_results_to_yt_result: Yql4Output = yql_4(
        input3=[helper_result.output1],
        input4=[get_last_mr_table_result],
        input2=[add_no_sctipt_texts_result.output1],
        input1=[buidl_result_result.new_table],
        request=vh3.Expr(LOAD_RESULT_TEST_TEMPLATE),
        yql_operation_title=yql_operation_title,
        mr_account=mr_account,
        yt_pool=yt_pool,
        yt_token=yt_token,
        yql_token=yql_token,
        mr_output_path=mr_output_path,
        **vh3.block_args(code="yql_11", name="load results to YT")
    )
    
from datacloud.model_applyer.tables.models_config_table import ApiModelsConfigTable
from datacloud.model_applyer.tables.apply_history_table import ApplyHistoryTable
from datacloud.dev_utils.yt.yt_config_table import ConfigTable
from datacloud.stability.scores_psi_table import ScoresPsiTable
from datacloud.stability.features_psi_table import FeaturesPSITable
from datacloud.stability.batch_monitoring_log_table import BatchMonitoringLogTable

AUC_THRESHOLD_PARAM = 'AUC_threshold'
HIT_THRESHOLD_PARAM = 'hit_threshold'
DEFAULT_PSI_THRESH = 0.1
DEFAULT_MIN_TABLE_SIZE = int(4e7)
DEFAULT_AUC_THRESH = 0.6
DEFAULT_HIT_THRESH = 0.6

PURE_CRYPTA_TYPE = BatchMonitoringLogTable.PURE_CRYPTA_TYPE


class StabilityNotReadyException(Exception):
    pass


def assert_psi_score(yt_client, score_name, date_str, score_type, threshold=DEFAULT_PSI_THRESH):
    psi_row = ScoresPsiTable().get_record(
        score_name=score_name, date=date_str, score_type=score_type)

    if psi_row is None:
        raise StabilityNotReadyException('PSI not found for score {score_name} {date_str}!'.format(
            score_name=score_name,
            date_str=date_str
        ))

    assert psi_row['PSI'] < threshold, 'PSI SCORE for {score_name} {date_str} is {PSI} with {threshold} thresh!'.format(
        score_name=score_name,
        date_str=date_str,
        PSI=psi_row['PSI'],
        threshold=threshold
    )


def assert_psi_features(yt_client, score_name, date_str, threshold=DEFAULT_PSI_THRESH,
                        min_table_size=DEFAULT_MIN_TABLE_SIZE):

    apply_rec = ApplyHistoryTable().get_record_by_params({
        'score_name': score_name,
        'date_str': date_str
    })
    if apply_rec is None:
        raise RuntimeError('Applyed model not found in apply-history table!')

    features_psi_table = FeaturesPSITable()
    for feature_name, feature_dates in apply_rec['features_dict'].iteritems():
        for feature_date in feature_dates:
            f_psi_rec = features_psi_table.get_record_by_params({
                'date': feature_date,
                'feature_name': feature_name
            })
            if f_psi_rec is None:
                raise StabilityNotReadyException('Feature PSI for {feature_date} {feature_name} wasn\'t found!'.format(
                    feature_date=feature_date,
                    feature_name=feature_name
                ))

            assert all(psi < threshold for psi in f_psi_rec['PSI']), 'Bad PSI at {feature_date} {feature_name}'.format(
                feature_date=feature_date,
                feature_name=feature_name
            )
            assert f_psi_rec['table_size'] > min_table_size, 'Too small table {feature_date} {feature_name}'.format(
                feature_date=feature_date,
                feature_name=feature_name
            )


def assert_batch_monitoring(yt_client, score_name, date_str, score_type, partner_id):
    config_rec = ApiModelsConfigTable().get_model_or_raise(partner_id, score_name)

    batch_monitoring_config = config_rec['additional'].get('batch_monitoring')
    assert batch_monitoring_config is not None, 'No batch_monitoring for {partner_id} {score_name} in config!'.format(
        partner_id=partner_id,
        score_name=score_name
    )

    batch_monitoring_table = BatchMonitoringLogTable()
    for batch_monitoring_params in batch_monitoring_config:
        batch_name = batch_monitoring_params.get('batch_name')
        assert batch_name is not None, 'No batch name found in {partner_id} {score_name}!'.format(
            partner_id=partner_id,
            score_name=score_name
        )

        monitoring_rec = batch_monitoring_table.get_record(
            partner_id=partner_id, date_str=date_str, score_name=score_name,
            score_type=score_type, crypta_type=PURE_CRYPTA_TYPE, batch_name=batch_name)
        if monitoring_rec is None:
            raise StabilityNotReadyException('No batch monitoring rec for {} {} {} {}!'.format(
                partner_id, date_str, score_name, batch_name
            ))

        AUC_threshold = batch_monitoring_params.get(AUC_THRESHOLD_PARAM, DEFAULT_AUC_THRESH)
        hit_threshold = batch_monitoring_params.get(HIT_THRESHOLD_PARAM, DEFAULT_HIT_THRESH)

        assert monitoring_rec['AUC'] > AUC_threshold, 'Bad AUC ({} with {} thresh) in {} {} {} {}!'.format(
            monitoring_rec['AUC'], AUC_threshold, partner_id, date_str, score_name, batch_name
        )
        assert monitoring_rec['hit'] > hit_threshold, 'Bad hit ({} with {} thresh) in {} {} {} {}!'.format(
            monitoring_rec['hit'], hit_threshold, partner_id, date_str, score_name, batch_name
        )

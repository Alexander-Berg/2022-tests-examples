from datacloud.audience.lib import score_info


def test_create_score_info():
    partner_id = 'test_partner_id'
    name = 'test_score_name'
    score = score_info.Score(partner_id, name)
    assert score.partner_id == partner_id
    assert score.name == name

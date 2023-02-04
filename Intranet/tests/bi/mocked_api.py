from tests.fixtures_for_bi import (
    INCOME_SAMPLE_YT_RECORDS,
    DETAILED_INCOME_SAMPLE_YT_RECORDS,
    VESTING_SAMPLE_YT_RECORDS,
    ASSIGNMENT_SAMPLE_YT_RECORDS,
)


class FakeBusinessIntelligenceAPI(object):

    def __init__(self, instance_name):
        self.instance_name = instance_name

    def get_data(self, *args, **kwargs):
        yt_records = {
            'income': INCOME_SAMPLE_YT_RECORDS,
            'detailed_income': DETAILED_INCOME_SAMPLE_YT_RECORDS,
            'assignment': ASSIGNMENT_SAMPLE_YT_RECORDS,
            'vesting': VESTING_SAMPLE_YT_RECORDS,
        }.get(self.instance_name, [])

        for record in yt_records:
            yield record.copy()

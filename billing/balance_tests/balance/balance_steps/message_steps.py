from balance import balance_api as api


class MessageSteps(object):

    @staticmethod
    def get_message_data(object_data):
        return api.test_balance().GetEmailMessageData(object_data)

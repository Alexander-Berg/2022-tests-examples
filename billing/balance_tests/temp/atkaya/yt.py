# # coding: utf-8
# from decimal import Decimal as D
#
# import datetime as dt
# import pytest
# import xmlrpclib
#
# import btestlib.utils as utils
# # from balance import balance_steps as steps
# from btestlib.matchers import contains_dicts_equal_to, equal_to
# from btestlib import reporter
# from btestlib.constants import YTSourceName, YTDefaultPath, TaxiOrderType, Currencies, Services, CorpTaxiOrderType
# from btestlib.data.partner_contexts import TAXI_RU_CONTEXT

# def test_yt():
#     yt_client = steps.YTSteps.create_yt_client()
#     yt_path = ""
#     steps.YTSteps.fill_table(yt_client, date, data_for_yt, path, attributes=None)
#     filepath = steps.YTSteps.get_table_path(path, date)
#     steps.YTSteps.remove_table_in_yt(filepath, yt_client)
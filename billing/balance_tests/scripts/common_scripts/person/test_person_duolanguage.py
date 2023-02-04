# coding=utf-8
import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, Products, Firms, Paysyses
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions
from decimal import Decimal as D, ROUND_HALF_UP
from btestlib.matchers import contains_dicts_with_entries

KZU_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.KZU,
                                                   additional_person_params={'local_name': 'local_nameжігіт Жоғары',
                                                                             'local_city': 'local_cityжігіт Жоғары',
                                                                             'local_postaddress': 'local_postaddressжігіт Жоғары',
                                                                             'local_legaladdress': 'local_legaladdressжігіт Жоғары',
                                                                             'local_bank': 'local_bankжігіт Жоғары',
                                                                             'local_signer_person_name': 'local_signer_person_nameжігіт Жоғары',
                                                                             'local_signer_position_name': 'local_signer_position_nameжігіт Жоғары',
                                                                             'local_authority_doc_details': 'local_authority_doc_detailsжігіт Жоғары'})

EU_YT = KZU_CONTEXT.new(person_type=PersonTypes.EU_YT,
                        additional_person_params={'local_name': 'local_nameжігіт Жоғары',
                                                  'local_representative': 'local_representativeжігіт Жоғары',
                                                  'local_postaddress': 'local_postaddressжігіт Жоғары',
                                                  'local_ben_bank': 'local_ben_bankжігіт Жоғары',
                                                  'local_other': 'local_other Жоғары'})

AM_JP = KZU_CONTEXT.new(person_type=PersonTypes.AM_UR,
                        additional_person_params={'local_name': 'local_nameհամաձայանգրով սահմանված ծառայություններ',
                                                  'local_longname': 'local_longnameհամաձայանգրով սահմանված ծառայություններ',
                                                  'local_postaddress': 'local_postaddressհամաձայանգրով սահմանված ծառայություններ Жоғары',
                                                  'local_ben_bank': 'local_ben_bankжігіт համաձայանգրով սահմանված ծառայություններ',
                                                  'local_legaladdress': 'local_legaladdressհամաձայանգրով սահմանված ծառայություններ Жоғары',
                                                  'local_city': 'local_cityжігіт համաձայանգրով սահմանված ծառայություններ',
                                                  'local_representative': 'հlocal_representativeամաձայանգրով սահմանված ծառայություններ Жоғары'})


@pytest.mark.parametrize('context', [KZU_CONTEXT,
                                     EU_YT,
                                     AM_JP])
def test_local_attributes(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, type_=context.person_type.code,
                                         params=context.additional_person_params, full=True)
    extprops = steps.CommonSteps.get_extprops(classname='Person', object_id=person_id)
    extprops_dict = {extprop['attrname']: extprop['value_str'] for extprop in extprops}
    utils.check_that([extprops_dict],
                     contains_dicts_with_entries([context.additional_person_params], same_length=False))

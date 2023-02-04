from enum import Enum
from textwrap import dedent

from luigi.parameter import ParameterException
import pytest

from dwh.grocery.targets import (
    TargetParameter,
    YTTableTarget,
    DBViewTarget,
)
from dwh.grocery.tools import (
    make_tuple_parameter,
)


class TestParameter:

    def test_target_parameter_for_yt_table(self):

        schema = [{'name': 'lolo', 'type': 'popkekat'}]
        test_target = YTTableTarget("//ololo", update_id=1488, schema=schema)

        target = TargetParameter().parse(TargetParameter().serialize(test_target))
        assert test_target == target
        assert test_target.update_id == target.update_id
        assert target.schema == schema

    def test_target_parameter_for_oracle_view(self):
        sql = dedent("""
            select arr.contract_id, arr.dkv_to_charge dkv, caf.discount_findt discdt
            from bo.v_ar_rewards arr join bo.v_contract_apex_full caf
            on arr.contract_id = caf.contract_id
        """)
        test_target = DBViewTarget(("meta/meta", sql))
        target = TargetParameter().parse(TargetParameter().serialize(test_target))
        assert isinstance(target, DBViewTarget)
        assert target.sql == sql

    def test_tuple_parameter(self):

        chunks = make_tuple_parameter(str, int)()
        with pytest.raises(ParameterException):
            chunks.parse("ololo, 15)")
            chunks.parse("(26, 15")
            chunks.parse("(ololo, 15, False)")
        with pytest.raises(ValueError):
            chunks.parse("(ololo, 16a)")
        assert chunks.parse("(ololo, 15)") == ("ololo", 15)
        assert chunks.parse("(ololo,15)") == ("ololo", 15)

    def test_tuple_parameter_unions(self):

        class Pirozhok(Enum):
            echpochmak = "echpochmak"
            posekunchik = "posekunchik"

            def __str__(self):
                return str(self.value)

        pitanie = make_tuple_parameter((Pirozhok, str), (int, float))()
        assert pitanie.parse("(echpochmak, 15)") == (Pirozhok.echpochmak, 15)
        assert pitanie.parse("(kashka, 15)") == ("kashka", 15)
        assert pitanie.serialize(pitanie.parse("(echpochmak, 15)")) == "(echpochmak,15)"
        assert pitanie.serialize((Pirozhok.echpochmak, 15)) == "(echpochmak,15)"
        assert pitanie.serialize(("echpochmak", 15)) == "(echpochmak,15)"
        assert pitanie.cast(("echpochmak", 15)) == (Pirozhok.echpochmak, 15)
        with pytest.raises(ValueError):
            pitanie.parse("(23, kashka)")

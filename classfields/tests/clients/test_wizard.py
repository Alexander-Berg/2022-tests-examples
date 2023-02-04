import pytest

from app.clients.wizard import WizardClient


class TestWizardClient:
    @pytest.mark.skip(reason="no longer used")
    @pytest.mark.asyncio
    async def test_mark_model(self):
        wc = WizardClient()
        resp = await wc.get_mark_model("Ваз")
        test_resp = {"mark": "LADA (ВАЗ)", "model": None}
        assert resp == test_resp
        resp = await wc.get_mark_model("Ваз (LADA) 2106")
        test_resp = {"mark": "LADA (ВАЗ)", "model": "2106"}
        assert resp == test_resp
        resp = await wc.get_mark_model("Ваз (LADA) 210699")
        test_resp = {"mark": None, "model": None}
        assert resp == test_resp
        resp = await wc.get_mark_model("")
        test_resp = {"mark": None, "model": None}
        assert resp == test_resp

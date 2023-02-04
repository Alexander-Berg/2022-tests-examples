import json
import urllib.parse
import pytest
import time
from pathlib import Path

from captcha.server.test.CaptchaTestSuite import CaptchaTestSuite


WATCH_INTERVAL_MS = 100


class TestOcrGeneration(CaptchaTestSuite):
    Controls = Path.cwd() / "controls"
    options = {
        "ModeFileWatcher": {
            "Path": str(Controls / "mode"),
            "PollIntervalMs": WATCH_INTERVAL_MS,
        },
    }

    @classmethod
    def setup_class(cls):
        cls.Controls.mkdir(exist_ok=True)
        super().setup_class()

    def set_fallback(self, fallback_enabled):
        with open(self.options["ModeFileWatcher"]["Path"], 'w') as out:
            if fallback_enabled:
                out.write("fallback_active")
            else:
                out.write("")
        time.sleep((WATCH_INTERVAL_MS + 100) / 1000.0)

    @pytest.fixture(autouse=True)
    def run_around_tests(self):
        self.set_fallback(False)
        yield

    def test_simple_generate(self):
        resp = self.send_request("/generate?json=1&type=ocr&vtype=ru")
        assert resp.getcode() == 200
        data = json.loads(resp.read().decode())

        ru_sound_intro_url = None
        for cfg in self.captcha.config["VoiceCaptchaTypes"]:
            if cfg["Name"] == "ru":
                ru_sound_intro_url = cfg["IntroUrl"]
        assert ru_sound_intro_url

        assert data.get("token")
        assert data.get("json") == '1'
        assert data.get("https") == 1
        assert data.get("imageurl").endswith(f"/image?key={data['token']}")
        assert data.get("voiceurl").endswith(f"/voice?key={data['token']}")
        assert data.get("voiceintrourl") == f"https://{ru_sound_intro_url}"

    @pytest.mark.parametrize(
        "try_wrong, try_voice, fallback_enabled",
        [
            (True, False, False),
            (False, False, False),
            (False, True, False),

            (True, False, True),
            (False, False, True),
            (False, True, True),
        ],
    )
    def test_generate_and_check(self, try_wrong, try_voice, fallback_enabled):
        self.set_fallback(fallback_enabled)

        resp = self.send_request("/generate?json=1&type=ocr&vtype=ru")
        assert resp.getcode() == 200
        data = json.loads(resp.read().decode())
        token = data["token"]

        # get session from YDB
        session = self.get_session(token)
        if fallback_enabled:
            assert not session
            # don't know how to get real answer
            known_answer = "known_answer"
            voice_answer = "voice_answer"
        else:
            assert session
            metadata = json.loads(session['metadata'])
            known_answer = metadata["image_metadata"]["answer"]
            voice_answer = metadata["voice_metadata"]["answer"]

        # try check without res
        resp = self.send_request(f"/check?json=1&key={token}")
        assert resp.getcode() == 200
        d = json.loads(resp.read().decode())
        assert d["status"] == "failed"
        assert d["error"] == "no user res(ponse)"
        assert d["json"] == "1"

        # try invalid token
        resp = self.send_request("/check?json=1&key=asd&res=qwe")
        assert resp.getcode() == 200
        d = json.loads(resp.read().decode())
        assert d["status"] == "failed"
        assert d["error"] == "not found"
        assert d["json"] == "1"

        if fallback_enabled:
            # image with enabled fallback is already allocated
            pass
        else:
            # try invalid res without allocated image
            resp = self.send_request(f"/check?json=1&key={token}&res=wowowowo")
            assert resp.getcode() == 200
            d = json.loads(resp.read().decode())
            assert d["status"] == "failed"
            assert d["error"] == "not found"
            assert d["error_desc"] == "image not allocated, go get image on /image first"
            assert d["json"] == "1"

        # allocate image
        resp = self.send_request(data["imageurl"])
        assert resp.getcode() == 200

        if try_wrong:
            # try invalid res
            resp = self.send_request(f"/check?json=1&key={token}&res=wowowowo")
            assert resp.getcode() == 200
            d = json.loads(resp.read().decode())
            assert d["status"] == "failed"
            assert "error" not in d
            assert d["json"] == "1"

        # try ok
        resp = self.send_request(f"/check?json=1&key={token}&res={urllib.parse.quote(voice_answer if try_voice else known_answer)}")
        assert resp.getcode() == 200
        d = json.loads(resp.read().decode())

        if fallback_enabled:
            assert d["status"] == "failed"
        else:
            if try_wrong:
                # на ответ есть только одна попытка, после чего сессия должна быть удалена
                assert d["status"] == "failed"
                assert d["error"] == "not found"
            elif not fallback_enabled:
                assert d["status"] == "ok"
                assert "error" not in d
        assert d["json"] == "1"

    @pytest.mark.parametrize(
        "client, check_wrong",
        [
            (None, False),
            (None, True),
            ('antirobot', False),
            ('antirobot', True),
        ],
    )
    def test_return_answer(self, client, check_wrong):
        resp = self.send_request("/generate?json=1&type=ocr&vtype=ru")
        data = json.loads(resp.read().decode())
        token = data["token"]
        assert self.send_request(data["imageurl"]).getcode() == 200

        session = self.get_session(token)
        metadata = json.loads(session['metadata'])
        answer = metadata["image_metadata"]["answer"]
        voice_answer = metadata["voice_metadata"]["answer"]

        check_url = f"/check?json=1&key={token}&res={urllib.parse.quote('asdasd' if check_wrong else answer)}"
        if client is not None:
            check_url += f"&client={client}"
        check_resp = self.send_request(check_url)
        check_data = json.loads(check_resp.read().decode())

        if client is None:
            assert "answer" not in check_data
            assert "voice_answer" not in check_data
        else:
            # независимо от правильного ответа поля должны быть
            assert check_data["answer"] == answer
            assert check_data["voice_answer"] == voice_answer
            for key in "voice_key", "image_key", "voice_metadata", "image_metadata":
                assert key in check_data["session_metadata"]

        if check_wrong:
            assert check_data["status"] == "failed", json.dumps(check_data)
        else:
            assert check_data["status"] == "ok", json.dumps(check_data)

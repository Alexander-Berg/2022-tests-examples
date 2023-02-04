from builtins import object

import pytest

from kelvin.scorm.utils import extract_progress_score_from_cmi


class TestScorm(object):
    def test_only_score_passed_scorm_12(self):
        cmi = {
            "completion_status": "",
            "objectives": {
                "_count": 0,
                "_children": {}
            },
            "comments_from_learner": {
                "_count": 0,
                "_children": ""
            },
            "score": {
                "raw": 0,
                "max": 0,
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "min": 0
            },
            "exit": "",
            "location": "",
            "learner_id": "",
            "time_limit_action": "",
            "learner_preference": {
                "audio_captioning": 0,
                "delivery_speed": 0,
                "audio_level": 0,
                "language": "",
                "_children": "audio_level,language,delivery_speed,audio_captioning"
            },
            "launch_data": "",
            "total_time": 0,
            "core": {
                "lesson_status": "completed",
                "score": {
                    "raw": "76"
                },
                "session_time": "00:02:19",
                "lesson_location": "15;1"
            },
            "max_time_allowed": "",
            "interactions": {
                "_count": 0,
                "_children": ""
            },
            "success_status": 0,
            "comments_from_lms": {
                "_count": 0,
                "_children": ""
            },
            "entry": "",
            "scaled_passing_score": 0,
            "learner_name": "",
            "session_time": 0,
            "_version": "1484.11",
            "suspend_data": "lesson_progress=1;101;00;0000;00;00;00;00;0000;00;00;00;00;00;0000;01",
            "credit": "",
            "mode": "",
            "progress_measure": 0,
            "completion_threshold": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': True,
            'score_percent': 76,
        }

    def test_only_score_failed_scorm_12(self):
        cmi = {
            "completion_status": "",
            "objectives": {
                "_count": 0,
                "_children": {}
            },
            "comments_from_learner": {
                "_count": 0,
                "_children": ""
            },
            "score": {
                "raw": 0,
                "max": 0,
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "min": 0
            },
            "exit": "",
            "location": "",
            "learner_id": "",
            "time_limit_action": "",
            "learner_preference": {
                "audio_captioning": 0,
                "delivery_speed": 0,
                "audio_level": 0,
                "language": "",
                "_children": "audio_level,language,delivery_speed,audio_captioning"
            },
            "launch_data": "",
            "total_time": 0,
            "core": {
                "lesson_status": "incomplete",
                "score": {
                    "raw": "24"
                },
                "session_time": "00:00:45",
                "lesson_location": "15;1"
            },
            "max_time_allowed": "",
            "interactions": {
                "_count": 0,
                "_children": ""
            },
            "success_status": 0,
            "comments_from_lms": {
                "_count": 0,
                "_children": ""
            },
            "entry": "",
            "scaled_passing_score": 0,
            "learner_name": "",
            "session_time": 0,
            "_version": "1484.11",
            "suspend_data": "lesson_progress=1;101;00;0000;00;00;00;00;0000;00;00;00;00;00;0000;01#"
                            "tests_progress=-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;24",
            "credit": "",
            "mode": "",
            "progress_measure": 0,
            "completion_threshold": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': False,
            'score_percent': 24,
        }

    def test_full_score_passed_scorm_12(self):
        cmi = {
            "_version": "1484.11",
            "comments_from_learner": {
                "_children": "",
                "_count": 0
            },
            "comments_from_lms": {
                "_children": "",
                "_count": 0
            },
            "completion_status": "",
            "completion_threshold": 0,
            "credit": "",
            "entry": "",
            "exit": "",
            "interactions": {
                "0": {
                    "id": "Scene6_Slide1_MultiResponse_0_0",
                    "type": "choice",
                    "student_response": "undefined,undefined,undefined",
                    "correct_responses": [
                        {
                            "pattern": "undefined,undefined,undefined"
                        }
                    ],
                    "result": "correct",
                    "weighting": "1",
                    "latency": "0000:01:06.57",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "time": "14:12:58"
                },
                "1": {
                    "id": "Scene7_Slide2_FreeFormHotspot_0_0",
                    "type": "choice",
                    "student_response": "Incorrect",
                    "correct_responses": [
                        {
                            "pattern": "I_am_performer"
                        }
                    ],
                    "result": "wrong",
                    "weighting": "1",
                    "latency": "0000:01:15.29",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "time": "14:13:07"
                },
                "_children": "0,1,2",
                "_count": 2
            },
            "launch_data": "",
            "learner_id": "",
            "learner_name": "",
            "learner_preference": {
                "_children": "audio_level,language,delivery_speed,audio_captioning",
                "audio_level": 0,
                "language": "",
                "delivery_speed": 0,
                "audio_captioning": 0
            },
            "location": "",
            "max_time_allowed": "",
            "mode": "",
            "objectives": {
                "_children": {},
                "_count": 0
            },
            "progress_measure": 0,
            "scaled_passing_score": 0,
            "score": {
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "raw": 0,
                "min": 0,
                "max": 0
            },
            "session_time": 0,
            "success_status": 0,
            "suspend_data": "2FbY60708090a0b0c0d0e0f0g0h0i0t0u0v0y0z0A0B0C0F0G0I0J0K0L0M0N0H0~2B2~2x21001116",
            "time_limit_action": "",
            "total_time": 0,
            "core": {
                "exit": "suspend",
                "lesson_status": "passed",
                "session_time": "0000:01:33.45",
                "score": {
                    "raw": "94",
                    "max": "100",
                    "min": "0"
                }
            }
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': True,
            'score_percent': 94,
        }

    def test_full_score_pending_scorm_12(self):
        cmi = {
            "_version": "1484.11",
            "comments_from_learner": {
                "_children": "",
                "_count": 0
            },
            "comments_from_lms": {
                "_children": "",
                "_count": 0
            },
            "completion_status": "",
            "completion_threshold": 0,
            "credit": "",
            "entry": "",
            "exit": "",
            "interactions": {
                "0": {
                    "id": "Scene6_Slide1_MultiResponse_0_0",
                    "type": "choice",
                    "student_response": "undefined,undefined,undefined",
                    "correct_responses": [
                        {
                            "pattern": "undefined,undefined,undefined"
                        }
                    ],
                    "result": "correct",
                    "weighting": "1",
                    "latency": "0000:01:06.57",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "time": "14:12:58"
                },
                "1": {
                    "id": "Scene7_Slide2_FreeFormHotspot_0_0",
                    "type": "choice",
                    "student_response": "Incorrect",
                    "correct_responses": [
                        {
                            "pattern": "I_am_performer"
                        }
                    ],
                    "result": "wrong",
                    "weighting": "1",
                    "latency": "0000:01:15.29",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "time": "14:13:07"
                },
                "_children": "0,1",
                "_count": 2
            },
            "launch_data": "",
            "learner_id": "",
            "learner_name": "",
            "learner_preference": {
                "_children": "audio_level,language,delivery_speed,audio_captioning",
                "audio_level": 0,
                "language": "",
                "delivery_speed": 0,
                "audio_captioning": 0
            },
            "location": "",
            "max_time_allowed": "",
            "mode": "",
            "objectives": {
                "_children": {},
                "_count": 0
            },
            "progress_measure": 0,
            "scaled_passing_score": 0,
            "score": {
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "raw": 0,
                "min": 0,
                "max": 0
            },
            "session_time": 0,
            "success_status": 0,
            "suspend_data": "2U6M60708090a0b0c0d0e0f0g0h0i0t0u0v0y0z0A0B0C0F0G0I0~2t4~2p41001017~",
            "time_limit_action": "",
            "total_time": 0,
            "core": {
                "exit": "suspend",
                "lesson_status": "incomplete",
                "session_time": "0000:34:37.91",
                "score": {
                    "raw": "25",
                    "max": "50",
                    "min": "0"
                }
            }
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': False,
            'score_percent': 50,
        }

    def test_full_scorm_passed_scorm_2004(self):
        cmi = {
            "completion_status": "completed",
            "objectives": {
                "_count": 0,
                "_children": {}
            },
            "comments_from_learner": {
                "_count": 0,
                "_children": ""
            },
            "score": {
                "raw": "67",
                "max": "100",
                "_children": "scaled,raw,min,max",
                "scaled": "1",
                "min": "0"
            },
            "exit": "suspend",
            "location": "",
            "learner_id": "",
            "time_limit_action": "",
            "learner_preference": {
                "audio_captioning": 0,
                "delivery_speed": 0,
                "audio_level": 0,
                "language": "",
                "_children": "audio_level,language,delivery_speed,audio_captioning"
            },
            "launch_data": "",
            "total_time": 0,
            "max_time_allowed": "",
            "interactions": {
                "0": {
                    "latency": "PT1M3.44S",
                    "correct_responses": [
                        {
                            "pattern": "undefined[,]undefined[,]undefined"
                        }
                    ],
                    "description": "Для каких целей НЕ стоит использовать Трекер?\n"
                                   "(выберите все правильные варианты ответа)",
                    "timestamp": "2019-08-14T14:32:50",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "weighting": "1",
                    "result": "correct",
                    "type": "choice",
                    "id": "Scene6_Slide1_MultiResponse_0_0",
                    "learner_response": "undefined[,]undefined[,]undefined"
                },
                "_count": 1,
                "_children": "0"
            },
            "success_status": "passed",
            "comments_from_lms": {
                "_count": 0,
                "_children": ""
            },
            "entry": "",
            "scaled_passing_score": 0,
            "learner_name": "",
            "session_time": "PT59.97S",
            "_version": "1484.11",
            "suspend_data": "2ccY60708090a0b0c0d0e0f0g0h0i0u0v0w0z0A0B0C0D0F0H0J0K0L0M0N0O0I0",
            "credit": "",
            "mode": "",
            "progress_measure": 0,
            "completion_threshold": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': True,
            'score_percent': 67,
        }

    def test_full_scorm_failed_2004(self):
        cmi = {
            "completion_status": "incomplete",
            "objectives": {
                "_count": 0,
                "_children": {}
            },
            "comments_from_learner": {
                "_count": 0,
                "_children": ""
            },
            "score": {
                "raw": "25",
                "max": "100",
                "_children": "scaled,raw,min,max",
                "scaled": "0.25",
                "min": "0"
            },
            "exit": "suspend",
            "location": "",
            "learner_id": "",
            "time_limit_action": "",
            "learner_preference": {
                "audio_captioning": 0,
                "delivery_speed": 0,
                "audio_level": 0,
                "language": "",
                "_children": "audio_level,language,delivery_speed,audio_captioning"
            },
            "launch_data": "",
            "total_time": 0,
            "max_time_allowed": "",
            "interactions": {
                "0": {
                    "latency": "PT1M3.44S",
                    "correct_responses": [
                        {
                            "pattern": "undefined[,]undefined[,]undefined"
                        }
                    ],
                    "description": "Для каких целей НЕ стоит использовать Трекер?\n"
                                   "(выберите все правильные варианты ответа)",
                    "timestamp": "2019-08-14T14:32:50",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "weighting": "1",
                    "result": "correct",
                    "type": "choice",
                    "id": "Scene6_Slide1_MultiResponse_0_0",
                    "learner_response": "undefined[,]undefined[,]undefined"
                },
                "1": {
                    "latency": "PT2M4.91S",
                    "correct_responses": [
                        {
                            "pattern": "I_am_performer"
                        }
                    ],
                    "description": "Hotspot",
                    "timestamp": "2019-08-14T14:33:52",
                    "objectives": [
                        {
                            "id": "l1_intro"
                        }
                    ],
                    "weighting": "1",
                    "result": "incorrect",
                    "type": "choice",
                    "id": "Scene7_Slide2_FreeFormHotspot_0_0",
                    "learner_response": "Incorrect"
                },
                "_count": 2,
                "_children": "0,1"
            },
            "success_status": "unknown",
            "comments_from_lms": {
                "_count": 0,
                "_children": ""
            },
            "entry": "",
            "scaled_passing_score": 0,
            "learner_name": "",
            "session_time": "PT5M53.11S",
            "_version": "1484.11",
            "suspend_data": "2Kd_60708090a0b0c0d0e0f0g0h0i0u0v0w0z0A0B0C0D0F0H0J0K0L0M0R0S0T0I0~205~2Y41001116",
            "credit": "",
            "mode": "",
            "progress_measure": 0,
            "completion_threshold": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': False,
            'score_percent': 25,
        }

    def test_partial_scorm_incomplete_2004(self):
        cmi = {
            "_version": "1484.11",
            "comments_from_learner": {
                "_children": "",
                "_count": 0
            },
            "comments_from_lms": {
                "_children": "",
                "_count": 0
            },
            "completion_status": "incomplete",
            "completion_threshold": 0,
            "credit": "",
            "entry": "",
            "exit": "suspend",
            "interactions": {
                "_children": "",
                "_count": 0
            },
            "launch_data": "",
            "learner_id": "",
            "learner_name": "",
            "learner_preference": {
                "_children": "audio_level,language,delivery_speed,audio_captioning",
                "audio_level": 0,
                "language": "",
                "delivery_speed": 0,
                "audio_captioning": 0
            },
            "location": "",
            "max_time_allowed": "",
            "mode": "",
            "objectives": {
                "_children": {},
                "_count": 0
            },
            "progress_measure": 0,
            "scaled_passing_score": 0,
            "score": {
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "raw": 0,
                "min": 0,
                "max": 0
            },
            "session_time": "PT1M59.95S",
            "success_status": "unknown",
            "suspend_data": "8g880v_player.5h6dRlbgfe7.6DGsJfI3yBs1^1^jdcb101001a1a10000000000",
            "time_limit_action": "",
            "total_time": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': False,
            'score_percent': 0,
        }

    def test_partial_scorm_completed_2004(self):
        cmi = {
            "_version": "1484.11",
            "comments_from_learner": {
                "_children": "",
                "_count": 0
            },
            "comments_from_lms": {
                "_children": "",
                "_count": 0
            },
            "completion_status": "completed",
            "completion_threshold": 0,
            "credit": "",
            "entry": "",
            "exit": "suspend",
            "interactions": {
                "_children": "",
                "_count": 0
            },
            "launch_data": "",
            "learner_id": "",
            "learner_name": "",
            "learner_preference": {
                "_children": "audio_level,language,delivery_speed,audio_captioning",
                "audio_level": 0,
                "language": "",
                "delivery_speed": 0,
                "audio_captioning": 0
            },
            "location": "",
            "max_time_allowed": "",
            "mode": "",
            "objectives": {
                "_children": {},
                "_count": 0
            },
            "progress_measure": 0,
            "scaled_passing_score": 0,
            "score": {
                "_children": "scaled,raw,min,max",
                "scaled": 0,
                "raw": 0,
                "min": 0,
                "max": 0
            },
            "session_time": "PT8M9.36S",
            "success_status": "passed",
            "suspend_data": "2rfU102030405060708090a0b0c0d0e0f0g0h0i0j0k0l0m0n0o0p0q0r0s0~2r2~2n21001r10~",
            "time_limit_action": "",
            "total_time": 0
        }

        assert extract_progress_score_from_cmi(cmi) == {
            'passed': True,
            'score_percent': 100,
        }

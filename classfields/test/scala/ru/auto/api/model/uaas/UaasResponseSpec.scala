package ru.auto.api.model.uaas

import org.apache.commons.codec.binary.Base64
import ru.auto.api.BaseSpec

class UaasResponseSpec extends BaseSpec {
  "unexpected data in uaas exp flags" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """pewpew"""
          .getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )
    uaasResponse.appFlags shouldBe Set()
    uaasResponse.desktopFlags shouldBe Set()
  }

  "appFlags with values" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64("""
          |[
          |  {
          |    "HANDLER": "AUTORU_APP",
          |    "CONTEXT": {
          |      "MAIN": {
          |        "AUTORU_APP": {
          |          "listing_response_optimizations": "back"
          |        }
          |      }
          |    },
          |    "TESTID": [
          |      "569820"
          |    ]
          |  }
          |]
          |""".stripMargin.getBytes("UTF-8")),
      "UTF-8"
    )

    UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    ).appMapFlags shouldBe Map("listing_response_optimizations" -> "back")
  }

  "unexpected data in uaas exp flags 2" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """["pewpew": 123]"""
          .getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )
    uaasResponse.appFlags shouldBe Set()
    uaasResponse.desktopFlags shouldBe Set()
  }

  "unexpected data in uaas exp flags 3" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """{"pewpew": 123}"""
          .getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )
    uaasResponse.appFlags shouldBe Set()
    uaasResponse.desktopFlags shouldBe Set()
  }

  "disabled uaas exp flags parse from app" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """[{"HANDLER": "AUTORU_APP","CONTEXT": {"MAIN": {"AUTORU_APP": {"exp1": true, "call_badge_enabled": false}}},"TESTID": ["234013"]}]"""
          .getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )
    uaasResponse.appFlags shouldBe Set("exp1")
    uaasResponse.desktopFlags shouldBe Set()
  }

  "uaas exp flags empty parse" in {
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      None
    )
    uaasResponse.appFlags shouldBe Set()
    uaasResponse.desktopFlags shouldBe Set()
    uaasResponse.searcherExperimentFlags shouldBe Map()
  }

  "uaas exp flags parse" in {
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(
        """W3siSEFORExFUiI6IkFVVE9SVV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiQVVUT1JVX0FQUCI6eyJhdXRvX3J1X29ubHkiOnRydWV9fX0sIlRFU1RJRCI6WyIyMzQwMTMiXX1d,W3siSEFORExFUiI6IkFVVE9SVV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiQVVUT1JVX0FQUCI6eyJtb3NfcnVfZW5hYmxlZCI6dHJ1ZSwidXNlcl9wcm9maWxlX21vc3J1X2VuYWJsZSI6dHJ1ZX19fSwiVEVTVElEIjpbIjIzODgxNSJdfV0=,W3siSEFORExFUiI6IkFVVE9SVV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiQVVUT1JVX0FQUCI6eyJqc19yZXBvcnRzX2VuYWJsZWQiOnRydWV9fX0sIlRFU1RJRCI6WyIyMjMzMDgiXX1d,W3siSEFORExFUiI6IkFVVE9SVV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiQVVUT1JVX0FQUCI6eyJsb2dpbl9mb3JfZmF2b3JpdGVzX2VuYWJsZWQiOnRydWV9fX0sIlRFU1RJRCI6WyIyMjczOTciXX1d,W3siSEFORExFUiI6IkFVVE9SVV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiQVVUT1JVX0FQUCI6e319fSwiVEVTVElEIjpbIjE5OTM3MyJdfV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTgzNDVfYmxvY2tfdW5kZXJfY3JlZGl0Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTgzMTNfYWxsX25ld19ibG9jayJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTgxODhfZmlyc3QtbG9vayJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTY3MjVfdmlucmVwb3J0c19lbWJlZGRlZF9hdXRoX29uX2NhcmQiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc4NDhfaG9yaXpvbnRhbF9ibG9ja3MiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTgxNTlfc2FtZV9idXRfbmV3Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc3OTFfd2l0aF9idXR0b25zIl19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc0NDdfY29udHJvbCJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc3OTlfbm8tZW1haWwiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc3NDVfaGlzdG9yeS1vbGQtbmFtZSJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc3MTFfbmV3X2dyb3VwaW5nX2NvbnRyb2wiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc2NjNfc2luZ2xlLWJ1dHRvbiJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTcwMDBfbW9iaWxlX3VuYXV0aG9yaXplZF9idXR0b25zIl19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTczOTlfY2lyY2xlX2FwcHJveGltYXRpb24iXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc2OTVfc2FsZXMtYXNzaXN0YW50Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc2NjZfY29tcGFyZV9idXR0b25faW5fbmV3Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc2NjUtdHJhZGVpbi1wcmljZS1zdGF0Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc1OTUtbmV3LXJlcG9ydC1wcmV2aWV3Il19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTc0MTYtcHJlc2V0c19mcm9tX2NhcmQiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTczNzNfZXhwXzI1cyJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTY3MjRfY2FsbF9zdGF0c19pbl9mYXZvcml0ZXMiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTg0MTNfdXNlZF9jaGVhcF9nYXpwcm9tYmFuayJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTY2OTdfbm9fc2luZ2xlX3JlcG9ydCIsIkFVVE9SVUZST05ULTE2Njk3X3RhYnNfY29sb3JfY2hhbmdlIl19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTY1NzZfcHJvbW9fZmlyc3RfdmlzaXQiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTY1MjVfdmFzLXRvb2x0aXAiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTU4OTFfbWluaW5nX3Bob25lc192X2IiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTU3NzRfZ3JlYXRfZGVhbCJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsic3Jhdm5pX29zYWdvIl19fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnt9fX1d,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTU3NzBfY2FsbGJhY2stb25seSJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsicnVzZmluYW5jZV9jcmVkaXQiXX19fV0=,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTU0MzVfYm90dG9tX2FuZF9pbnBsYWNlX2FsbCJdfX19XQ==,W3siSEFORExFUiI6IkFVVE9fUlUiLCJDT05URVhUIjp7IkFVVE9fUlUiOnsiZmxhZ3MiOlsiQVVUT1JVRlJPTlQtMTMyMzNfc2VhcmNobGluZSJdfX19XQ=="""
      )
    )
    uaasResponse.appFlags shouldBe Set(
      "user_profile_mosru_enable",
      "mos_ru_enabled",
      "js_reports_enabled",
      "auto_ru_only",
      "login_for_favorites_enabled"
    )
    uaasResponse.desktopFlags shouldBe Set(
      "AUTORUFRONT-17848_horizontal_blocks",
      "AUTORUFRONT-13233_searchline",
      "rusfinance_credit",
      "AUTORUFRONT-17711_new_grouping_control",
      "AUTORUFRONT-16697_tabs_color_change",
      "AUTORUFRONT-17447_control",
      "AUTORUFRONT-15435_bottom_and_inplace_all",
      "AUTORUFRONT-17665-tradein-price-stat",
      "AUTORUFRONT-16525_vas-tooltip",
      "AUTORUFRONT-18313_all_new_block",
      "AUTORUFRONT-15891_mining_phones_v_b",
      "AUTORUFRONT-17399_circle_approximation",
      "AUTORUFRONT-17791_with_buttons",
      "AUTORUFRONT-17595-new-report-preview",
      "AUTORUFRONT-18345_block_under_credit",
      "sravni_osago",
      "AUTORUFRONT-17000_mobile_unauthorized_buttons",
      "AUTORUFRONT-16576_promo_first_visit",
      "AUTORUFRONT-18159_same_but_new",
      "AUTORUFRONT-18188_first-look",
      "AUTORUFRONT-17799_no-email",
      "AUTORUFRONT-17666_compare_button_in_new",
      "AUTORUFRONT-17663_single-button",
      "AUTORUFRONT-17745_history-old-name",
      "AUTORUFRONT-15770_callback-only",
      "AUTORUFRONT-15774_great_deal",
      "AUTORUFRONT-16725_vinreports_embedded_auth_on_card",
      "AUTORUFRONT-18413_used_cheap_gazprombank",
      "AUTORUFRONT-17416-presets_from_card",
      "AUTORUFRONT-16724_call_stats_in_favorites",
      "AUTORUFRONT-17373_exp_25s",
      "AUTORUFRONT-16697_no_single_report",
      "AUTORUFRONT-17695_sales-assistant"
    )
  }

  "searcher experiment flags parse" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """[{
          "HANDLER": "AUTO_RU",
          "CONTEXT": {
            "AUTO_RU": {
              "flags": ["SEARCHER_relevance_BatchRealtimeCatboostComparator"],
              "SEARCHER" : {
                "state": "NEW",
                "exp_flags": "Exp1",
                "boost": 3.3
              }
            }
          }
        }]""".getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )

    uaasResponse.searcherExperimentFlags shouldBe
      Map("state" -> Set("NEW"), "exp_flags" -> Set("Exp1"), "boost" -> Set("3.3"))
  }

  "bad data for searcher experiment flags" in {
    val uaasExpFlags = new String(
      Base64.encodeBase64(
        """[{
          "HANDLER": "AUTO_RU",
          "CONTEXT": {
            "AUTO_RU": {
              "flags": ["SEARCHER_relevance_BatchRealtimeCatboostComparator"]
            },
            "SEARCHER" : {
              "state": "NEW",
              "exp_flags": "Exp1"
            }
          }
        }]""".getBytes("UTF-8")
      ),
      "UTF-8"
    )
    val uaasResponse = UaasResponse(
      None,
      None,
      None,
      Some(uaasExpFlags)
    )

    uaasResponse.searcherExperimentFlags shouldBe Map()
  }
}

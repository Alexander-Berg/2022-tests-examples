package com.yandex.mail.network.json.response;

import com.yandex.mail.network.response.GsonTest;
import com.yandex.mail.network.response.SettingsJson;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.MailSettings;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class SettingsJsonTest extends GsonTest {

    @Test
    public void settingsResponse_shouldDeserializeFromJson() throws IOException {
        final String userParametersWrapperString =
                "{\n" +
                "        \"body\":{\n" +
                "            \"alias-promo\":\"%7B%22shown-on-done%22%3Atrue%7D\",\n" +
                "            \"is_ad_disabled_via_billing\": \"on\",\n" +
                "            \"collectors_promo_s\":\"1445532711797\",\n" +
                "            \"done_shown_counter\":\"7\",\n" +
                "            \"folders_open\":\"2060000490004707824,2060000490004711726\",\n" +
                "            \"has_inbox_message\":\"on\",\n" +
                "            \"hk_promo_bubble\":\"count=1&lastTS=1445532767137&codes=e&e=1453308767132\",\n" +
                "            \"inline-wizard-step\":\"1\",\n" +
                "            \"last-login-ts\":\"1447333155656\",\n" +
                "            \"last_news\":\"21\",\n" +
                "            \"mail-in-browser\":\"%7B%22firstStart%22%3A0%7D\",\n" +
                "            \"mail_inbrowser_close\":\"1446749075900\",\n" +
                "            \"messages_avatars\":\"on\",\n" +
                "            \"no_collectors_bubble\":\"on\",\n" +
                "            \"no_reply_notify\":\"\",\n" +
                "            \"noreply_popup_shown\":\"on\",\n" +
                "            \"mobile_open_from_web\":\"1\",\n" +
                "            \"qr-paranja-shows\":\"2\",\n" +
                "            \"rph\":\"mozilla=1&opera=0&webkit=1\",\n" +
                "            \"search-version\":\"2014.03.14\",\n" +
                "            \"seasons-modifier\":\"whatever\",\n" +
                "            \"tb_mail_mailbox_act\":\"on\",\n" +
                "            \"todo_promo\":\"on\",\n" +
                "            \"todo_promo_onsent\":\"on\",\n" +
                "            \"todo_promo_show\":\"on\",\n" +
                "            \"user_dropdown_promo\":\"on\",\n" +
                "            \"wizard-close\":\"on\",\n" +
                "            \"timer_logic\":\"6551\",\n" +
                "            \"mobile_user_agent\":\"smartphone\"\n" +
                "        }\n" +
                "    }";
        SettingsJson.UserParametersWrapper userParametersWrapper =
                gson.fromJson(userParametersWrapperString, SettingsJson.UserParametersWrapper.class);
        SettingsJson.UserParametersWrapper.UserParameters params = userParametersWrapper.getBody();

        assertThat(params.getMobile_open_from_web()).isTrue();
        assertThat(params.getSeasonsModifier()).isEqualTo("whatever");
        assertThat(params.isAdDisabledViaBilling()).isTrue();
    }

    @Test
    public void settingsSetup_shouldDeserializeFromJson() throws IOException {
        final String settingsSetupString =
                "{\n" +
                "        \"body\":{\n" +
                "            \"abook_page_size\":\"50\",\n" +
                "            \"alert_on_empty_subject\":\"on\",\n" +
                "            \"broad_view\":\"\",\n" +
                "            \"close_quoting\":\"\",\n" +
                "            \"collect_addresses\":\"on\",\n" +
                "            \"color_scheme\":\"some_color_scheme\",\n" +
                "            \"copy_smtp_messages_to_sent\":\"\",\n" +
                "            \"daria_welcome_page\":\"\",\n" +
                "            \"default_email\":\"xiva4test02@yandex.ru\",\n" +
                "            \"default_mailbox\":\"yandex.ru\",\n" +
                "            \"disable_social_notification\":\"\",\n" +
                "            \"dnd_enabled\":\"on\",\n" +
                "            \"dont_delete_msg_from_imap\":\"\",\n" +
                "            \"duplicate_menu\":\"\",\n" +
                "            \"enable_autosave\":\"on\",\n" +
                "            \"enable_firstline\":\"on\",\n" +
                "            \"enable_hotkeys\":\"on\",\n" +
                "            \"enable_images\":\"on\",\n" +
                "            \"enable_images_in_spam\":\"\",\n" +
                "            \"enable_imap\":\"on\",\n" +
                "            \"enable_mailbox_selection\":\"on\",\n" +
                "            \"enable_pop\":\"\",\n" +
                "            \"enable_pop3_max_download\":\"\",\n" +
                "            \"enable_quoting\":\"on\",\n" +
                "            \"enable_richedit\":\"on\",\n" +
                "            \"enable_social_notification\":\"on\",\n" +
                "            \"enable_welcome_page\":\"on\",\n" +
                "            \"first_login\":\"\",\n" +
                "            \"folder_thread_view\":\"on\",\n" +
                "            \"from_name\":\"xiva4test02 Hub\",\n" +
                "            \"from_name_eng\":\"\",\n" +
                "            \"have_seen_daria\":\"\",\n" +
                "            \"have_seen_stamp\":\"\",\n" +
                "            \"hide_daria_header\":\"\",\n" +
                "            \"hide_tip_for_video_letter\":\"\",\n" +
                "            \"https_enabled\":\"on\",\n" +
                "            \"imap_rename_enabled\":\"on\",\n" +
                "            \"interface_settings\":\"\",\n" +
                "            \"jump_to_next_message\":\"\",\n" +
                "            \"label_sort\":\"by_count\",\n" +
                "            \"messages_per_page\":\"30\",\n" +
                "            \"ml_to_inbox\":\"on\",\n" +
                "            \"mobile_messages_per_page\":\"15\",\n" +
                "            \"mobile_sign\":\"some signature\",\n" +
                "            \"new_interface_by_default\":\"\",\n" +
                "            \"no_advertisement\":\"\",\n" +
                "            \"no_firstline\":\"\",\n" +
                "            \"no_mailbox_selection\":\"\",\n" +
                "            \"no_news\":\"\",\n" +
                "            \"page_after_delete\":\"current_list\",\n" +
                "            \"page_after_move\":\"current_list\",\n" +
                "            \"page_after_send\":\"done\",\n" +
                "            \"pop3_archivate\":\"on\",\n" +
                "            \"pop3_makes_read\":\"\",\n" +
                "            \"pop3_max_download\":\"50\",\n" +
                "            \"pop_spam_enable\":\"on\",\n" +
                "            \"pop_spam_subject_mark_enable\":\"on\",\n" +
                "            \"quotation_char\":\">\",\n" +
                "            \"save_sent\":\"on\",\n" +
                "            \"show_advertisement\":\"on\",\n" +
                "            \"show_avatars\":\"on\",\n" +
                "            \"show_chat\":\"\",\n" +
                "            \"show_news\":\"on\",\n" +
                "            \"show_socnet_avatars\":\"\",\n" +
                "            \"show_stocks\":\"\",\n" +
                "            \"show_todo\":\"\",\n" +
                "            \"show_unread\":\"\",\n" +
                "            \"show_weather\":\"\",\n" +
                "            \"signature\":\"\",\n" +
                "            \"signature_eng\":\"\",\n" +
                "            \"signature_top\":\"on\",\n" +
                "            \"skin_name\":\"neo2\",\n" +
                "            \"subs_messages_per_page\":\"20\",\n" +
                "            \"subs_show_informer\":\"on\",\n" +
                "            \"subs_show_item\":\"on\",\n" +
                "            \"subs_show_line\":\"on\",\n" +
                "            \"subs_show_unread\":\"\",\n" +
                "            \"suggest_addr_maxnum\":\"500\",\n" +
                "            \"suggest_addresses\":\"on\",\n" +
                "            \"translate\":\"on\",\n" +
                "            \"use_monospace_in_text\":\"\",\n" +
                "            \"use_small_fonts\":\"\",\n" +
                "            \"webchat_turned_off\":\"\",\n" +
                "            \"yandex_sign_enable\":\"\",\n" +
                "            \"reply_to\":{\"item\":[]},\n" +
                "            \"timer_logic\":\"18379\",\n" +
                "            \"mobile_user_agent\":\"smartphone\"\n" +
                "        }\n" +
                "    }";
        SettingsJson.SettingsSetup settingsSetupResponse =
                gson.fromJson(settingsSetupString, SettingsJson.SettingsSetup.class);
        SettingsJson.Body settings = settingsSetupResponse.getBody();

        assertThat(settings.getColor_scheme()).isEqualTo("some_color_scheme");
        assertThat(settings.getDefault_email()).isEqualTo("xiva4test02@yandex.ru");
        assertThat(settings.getFolder_thread_view()).isTrue();
        assertThat(settings.getFrom_name()).isEqualTo("xiva4test02 Hub");
        assertThat(settings.getReply_to()).isNotNull();
        assertThat(settings.getMobile_sign()).isEqualTo("some signature");
        assertThat(settings.getSignature_top()).isEqualTo(MailSettings.SignaturePlace.AFTER_REPLY);
    }

    @Test
    public void accountInformation_shouldDeserializeFromJson() throws IOException {
        final String accountInformationString =
                "{\n" +
                "        \"account-information\":{\n" +
                "            \"ckey\":\"FzLuWwPePYKHuEGc5PNQtm0ZzRXYFla5Fj7X0H1/css=\",\n" +
                "            \"dc\":\"\",\n" +
                "            \"region_id\":\"2\",\n" +
                "            \"region_parents\":\"2,10174,17,225,10001,10000\",\n" +
                "            \"region_code\":\"ru\",\n" +
                "            \"region_phone_code\":\"812\",\n" +
                "            \"country_phone_code\":\"7\",\n" +
                "            \"use_moko\":\"true\",\n" +
                "            \"session_fraud\":\"0\",\n" +
                "            \"password_verification_age\":\"0\",\n" +
                "            \"db\":\"mdb150\",\n" +
                "            \"uid\":\"339873371\",\n" +
                "            \"suid\":\"879100628\",\n" +
                "            \"yandex_account\":\"xiva4test02\",\n" +
                "            \"login\":\"xiva4test02\",\n" +
                "            \"locale\":\"ru\",\n" +
                "            \"reg_country\":\"ru\",\n" +
                "            \"timezone\":\"Europe/Moscow\",\n" +
                "            \"timezone_abbr\":\"MSK\",\n" +
                "            \"tz_offset\":\"180\",\n" +
                "            \"reg_date\":\"1445453264000\",\n" +
                "            \"display_name\":\"xiva4test02\",\n" +
                "            \"domain\":\"\",\n" +
                "            \"birthday\":\"\",\n" +
                "            \"sids\":{\n" +
                "                \"sid\":[\n" +
                "                    \"2\",\n" +
                "                    \"201\",\n" +
                "                    \"203\"\n" +
                "                ]\n" +
                "            },\n" +
                "            \"kstatus\":\"0\",\n" +
                "            \"compose-check\":\"45009312ad1271d419ca12d91d60a07b\",\n" +
                "            \"childAccounts\":{},\n" +
                "            \"emails\":{\n" +
                "                \"email\":[\n" +
                "                    {\n" +
                "                        \"login\":\"xiva4test02\",\n" +
                "                        \"domain\":\"narod.ru\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"login\":\"xiva4test02\",\n" +
                "                        \"domain\":\"ya.ru\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"login\":\"xiva4test02\",\n" +
                "                        \"domain\":\"yandex.ua\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"home\":\"no\",\n" +
                "            \"timer_logic\":\"1022\",\n" +
                "            \"mobile_user_agent\":\"smartphone\"\n" +
                "        }\n" +
                "    }";
        SettingsJson.AccountInformationWrapper accountInformationWrapper =
                gson.fromJson(accountInformationString, SettingsJson.AccountInformationWrapper.class);
        SettingsJson.AccountInformation accountInformation = accountInformationWrapper.getAccountInformation();

        assertThat(accountInformation.getComposeCheck()).isEqualTo("45009312ad1271d419ca12d91d60a07b");
        assertThat(accountInformation.getSuid()).isEqualTo("879100628");
        assertThat(accountInformation.getUid()).isEqualTo("339873371");
        assertThat(accountInformation.getEmails().getEmail()).hasSize(3);
    }
}

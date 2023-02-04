#include <saas/rtyserver_test/cases/indexer/ann.h>

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

#include <saas/rtyserver/components/ann/const.h>

#include <saas/library/robot/ann/prepare_ann.h>

#include <saas/api/action.h>

#include <library/cpp/json/json_reader.h>

using TAnnFormats = NSaas::TAnnFormats;

START_TEST_DEFINE(TestMusicAnn)
private:
    static const TStringBuf Doc1;

public:
    using TFactors = NSaas::TDocFactorsView;
    static constexpr bool TestSoleQrMatch = true;

public:
    bool InitConfig() override {
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".ImitateQrQuorum"] = ToString(TestSoleQrMatch);
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".UseExternalCalcer"] = "true";
        return true;
    }

    void QuerySearchL(TString query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>* resultProps, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        QuerySearch(query, results, resultProps);
    }

    void ReadFactors(TFactors& factors, const TString& query, const TString& kps, bool deleted, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        ReadFactors(factors, query, kps, deleted);
    }

    void ReadFactors(TFactors& factors, const TString& query, const TString& kps, bool deleted) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString textSearch = query;
        Quote(textSearch);
        QuerySearch(textSearch + "&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors&" + kps, results, &resultProps);
        if (deleted) {
            if (results.size() != 0)
                ythrow yexception() << "Documents found when there should be none";
            factors.Clear();
        } else {
            if (results.size() != 1)
                ythrow yexception() << "No documents found";
            factors.AssignFromSearchResult(*resultProps[0]);
        }
        factors.DebugPrint();
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;

        NRTYServer::TMessage mes = NSaas::TAction().ParseFromJson(NJson::ReadJsonFastTree(Doc1)).ToProtobuf();
        //NRTYServer::TMessage mes;i
        //CHECK_WITH_LOG(mes.ParseFromArray(Doc1.Data(), Doc1.Size()));

        auto* doc = mes.MutableDocument();
        doc->SetKeyPrefix(0);
        doc->MutableAnnData()->Clear();

        NIndexAnn::TIndexAnnSiteData annData;
        NIndexAnn::TAnnotationRec& annRec = *annData.AddRecs();
        annRec.SetText("гимн пионеров википедия");
        annRec.SetTextLanguage(1);
        NIndexAnn::TRegionData& annReg = *annRec.AddData();
        annReg.SetRegion(225);
        annReg.MutableUserQueryUrl()->SetCorrectedCtr(0.0296078);
        annReg.MutableClickMachine()->SetOneClick(0.08);
        annReg.MutableClickMachine()->SetLongClick(0.0003);
        NIndexAnn::UnpackToSaasData(*doc->MutableAnnData(), annData, NIndexAnn::GetSaasProfile());

        messages.emplace_back(std::move(mes));
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messages);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

        QuerySearchL("взвейтесь кострами&kps=0", results, &resultProps, __LINE__);
        CHECK_WITH_LOG(results.size() == 1);
        TFactors factors;
        ReadFactors(factors, "взвейтесь кострами", "kps=0", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0); // Quorum in Text, but no hits in ANN
        ReadFactors(factors, "взвейтесь кострами", "kps=0&pron=no_factorann&relev=no_ann_saas", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0); // Quorum in Text, but no hits in ANN. The document is still found (pron has no effect)
        ReadFactors(factors, "взвейтесь кострами", "kps=0&relev=relevgeo=225", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0); // Quorum in Text, but no hits in ANN. The document is still found (relevgeo has no effect)

        ReadFactors(factors, "взвейтесь кострами гимн", "kps=0", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0); // Quorum in Text, but no hits in ANN (relevgeo bans ann usage);

        ReadFactors(factors, "взвейтесь кострами гимн", "kps=0&relev=relevgeo=225", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0.00188214); // Quorum in Text, and a hit in ANN

        ReadFactors(factors, "взвейтесь кострами гимн", "kps=0&relev=relevgeo=225&pron=no_factorann&relev=no_ann_saas", true, __LINE__); // The document is not found due to pron

        const bool expectSoleQrMatch = TestSoleQrMatch;
        QuerySearchL("гимн&kps=0&relev=relevgeo=41", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), expectSoleQrMatch ? 1 : 0);
        QuerySearchL("гимн пионеров&kps=0&relev=relevgeo=41", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), expectSoleQrMatch ? 1 : 0);
        QuerySearchL("\"взвейтесь кострами\"&kps=0&relev=relevgeo=41", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), 1);
        QuerySearchL("\"кострами взвейтесь\"&kps=0", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), 1); //TODO: why??
        QuerySearchL("\"гимн пионеров\"&kps=0&relev=relevgeo=225", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), expectSoleQrMatch ? 1 : 0);
        QuerySearchL("\"гимн пионеров\"&kps=0&relev=relevgeo=225&pron=no_factorann&relev=no_ann_saas", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), 0); // no SoleQrMatch due to pron

        QuerySearchL("\"пионеров гимн\"&kps=0", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), 0);

        if (expectSoleQrMatch) {
            TFactors factors;
            ReadFactors(factors, "гимн пионеров", (TString)"&relev=relevgeo=225" + GetAllKps(messages), /*deleted=*/false, __LINE__);
            factors.CheckFactor("CorrectedCtrBm15V4K5", 0.00564641); // ANN-only hit
        }
        return true;
    }
};

// Obtained from Ferryman full state as:
//     ./rtymsg_reader --proxy hahn --table '//tmp/yrum1msg' --column value --json 1 | jq -c . | sed -e 's:\t:\\011:g' -e 's:":\t:g'
//                      | python -c 'import sys; s=sys.stdin.read(); print s.encode("string-escape");'
//                      | sed -e 's:\\t:\\":g' -e 's:\\n$::'

const TStringBuf TTestMusicAnnCaseClass::Doc1 =
"{\"prefix\":1,\"action\":\"modify\",\"docs\":[{\"IsUser\":[{\"type\":\"#f\",\"value\":0}],\"track_html_autoplay_videoplayer\":[{\"type\":\"#p\",\"value\":\"<iframe src=\\\"//www.youtube.com/embed/sgJotDWQeBc?autoplay=1&amp;enablejsapi=1&amp;wmode=opaque\\\" frameborder=\\\"0\\\" scrolling=\\\"no\\\" allowfullscreen=\\\"1\\\" allow=\\\"autoplay; fullscreen; accelerometer; gyroscope; picture-in-picture\\\" aria-label=\\\"Video\\\"></iframe>\"}],\"track_video_thumbnail\":[{\"type\":\"#p\",\"value\":\"https://avatars.mds.yandex.net/get-vthumb/901237/2c88f6d0c9f2f167d49ffb163d60cd60/%%x%%\"}],\"z_track_name\":{\"value\":\"\xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8. \xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xb2\xd0\xbe\xd0\xb3\xd0\xbd\xd0\xb8\xd1\x89\xd0\xb0\xd0\xbc\xd0\xb8\",\"type\":\"#z\"},\"tracks_total_likes\":[{\"type\":\"#f\",\"value\":0}],\"alb_name\":[{\"type\":\"#p\",\"value\":\"\xd0\xa8\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd0\xa3\xd1\x80\xd0\xbe\xd0\xba \xd0\xbe \xd0\xa0\xd0\xbe\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xb5\"}],\"track_id\":[{\"type\":\"#p\",\"value\":\"32708382\"}],\"artists_artist_likes\":[{\"type\":\"#f\",\"value\":0}],\"UserId\":[{\"type\":\"#f\",\"value\":0}],\"artists_count_of_albums\":[{\"type\":\"#f\",\"value\":0}],\"rank\":[{\"type\":\"#p\",\"value\":\"0.554523929057\"}],\"tracks_total_listening\":[{\"type\":\"#f\",\"value\":0.00063394}],\"type\":[{\"type\":\"#p\",\"value\":\"1\"}],\"IsTrack\":[{\"type\":\"#f\",\"value\":1}],\"explicit\":[{\"type\":\"#p\",\"value\":\"0\"}],\"Explicit\":[{\"type\":\"#f\",\"value\":0}],\"tracks_recom_listening\":[{\"type\":\"#f\",\"value\":0.000553471}],\"_SerpData\":[{\"type\":\"#p\",\"value\":\"{\\\"track_id\\\":\\\"32708382\\\",\\\"track_minus\\\":\\\"0\\\",\\\"track_name\\\":\\\"\xd0\x92\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8\\\",\\\"track_remix\\\":\\\"0\\\",\\\"track_storage_dir\\\":\\\"143324_75c48e39.48649778.1.32708382\\\",\\\"track_version\\\":\\\"\\\",\\\"track_len\\\":\\\"126620\\\",\\\"track_len_str\\\":\\\"02:06\\\",\\\"track_lyrics\\\":\\\"\\\",\\\"track_youtube_url\\\":\\\"http://www.youtube.com/watch?v=sgJotDWQeBc\\\",\\\"track_html_autoplay_videoplayer\\\":\\\"<iframe src=\\\\\\\"//www.youtube.com/embed/sgJotDWQeBc?autoplay=1&amp;enablejsapi=1&amp;wmode=opaque\\\\\\\" frameborder=\\\\\\\"0\\\\\\\" scrolling=\\\\\\\"no\\\\\\\" allowfullscreen=\\\\\\\"1\\\\\\\" allow=\\\\\\\"autoplay; fullscreen; accelerometer; gyroscope; picture-in-picture\\\\\\\" aria-label=\\\\\\\"Video\\\\\\\"></iframe>\\\",\\\"track_video_thumbnail\\\":\\\"https://avatars.mds.yandex.net/get-vthumb/901237/2c88f6d0c9f2f167d49ffb163d60cd60/%%x%%\\\",\\\"alb_data\\\":\\\"\\\",\\\"alb_data_json\\\":\\\"\\\",\\\"alb_id\\\":\\\"3988854\\\",\\\"alb_name\\\":\\\"\xd0\xa8\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd0\xa3\xd1\x80\xd0\xbe\xd0\xba \xd0\xbe \xd0\xa0\xd0\xbe\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xb5\\\",\\\"alb_track_count\\\":\\\"\\\",\\\"alb_year\\\":\\\"2014\\\",\\\"alb_storage_dir\\\":\\\"d1a35fdc.a.3988854\\\",\\\"alb_cover\\\":\\\"1\\\",\\\"alb_cover_path\\\":\\\"d1a35fdc.a.3988854/1\\\",\\\"alb_image_uri\\\":\\\"avatars.yandex.net/get-music-content/63210/d1a35fdc.a.3988854-1/%%\\\",\\\"grp_data\\\":\\\"\\\",\\\"grp_data_json\\\":\\\"\\\",\\\"grp\\\":[{\\\"name\\\":\\\"\xd0\xa5\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd0\xa6\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2\\\",\\\"tracks\\\":\\\"8\\\",\\\"albums\\\":\\\"0\\\",\\\"id\\\":\\\"2854165\\\"}],\\\"grp_similar\\\":\\\"\\\",\\\"grp_similar_ids\\\":[],\\\"grp_similar_names\\\":[],\\\"pls_data\\\":\\\"\\\",\\\"pls_data_json\\\":\\\"\\\",\\\"pls_kind\\\":\\\"\\\",\\\"pls_name\\\":\\\"\\\",\\\"pls_track_count\\\":\\\"\\\",\\\"pls_user_id\\\":\\\"\\\",\\\"pls_user_login\\\":\\\"\\\",\\\"geo_id\\\":\\\"225\\\",\\\"mus_type\\\":\\\"1\\\",\\\"pop_tr_ids\\\":[],\\\"pop_tr_lens\\\":[],\\\"pop_tr_names\\\":[],\\\"pop_tr_groups\\\":[],\\\"pop_tr_versions\\\":[],\\\"max_tr_cnt\\\":3,\\\"popularity\\\":\\\"1049.0\\\",\\\"radio\\\":\\\"\\\",\\\"rank\\\":\\\"0.554523929057\\\",\\\"various\\\":\\\"0\\\"}\"}],\"url\":\"https://music.yandex.ru/album/3988854/track/32708382?from=serp\",\"dup\":[{\"type\":\"#h\",\"value\":\"955eaa3d38816150bd318a525781307d\"}],\"artists_count_of_non_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"albums_total_listening\":[{\"type\":\"#f\",\"value\":0}],\"MusicLinkRank\":[{\"type\":\"#f\",\"value\":0.554524}],\"_SerpInfo\":[{\"type\":\"#p\",\"value\":\"{\\\"type\\\":\\\"musicplayer\\\",\\\"format\\\":\\\"json\\\",\\\"flat\\\":\\\"1\\\",\\\"subtype\\\":\\\"track\\\",\\\"remove\\\":\\\"lyrics\\\",\\\"slot\\\":\\\"full\\\"}\"}],\"albums_count_of_albums\":[{\"type\":\"#f\",\"value\":0}],\"i_is_tribute\":[{\"type\":\"#i\",\"value\":0}],\"grp_name\":[{\"type\":\"#p\",\"value\":\"\xd0\xa5\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd0\xa6\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2\"}],\"track_storage_dir\":[{\"type\":\"#p\",\"value\":\"143324_75c48e39.48649778.1.32708382\"}],\"tracks_count_of_non_skip_listening\":[{\"type\":\"#f\",\"value\":0.000489449}],\"tracks_count_of_albums\":[{\"type\":\"#f\",\"value\":0.0015674}],\"albums_count_of_fast_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"artists_count_of_playlists\":[{\"type\":\"#f\",\"value\":0}],\"tracks_count_of_playlists\":[{\"type\":\"#f\",\"value\":0.000543627}],\"IsAlbum\":[{\"type\":\"#f\",\"value\":0}],\"options\":{\"mime_type\":\"text/plain\",\"modification_timestamp\":1559547381,\"realtime\":false},\"i_is_remix\":[{\"type\":\"#i\",\"value\":0}],\"regions\":[{\"type\":\"#p\",\"value\":\"149\"},{\"type\":\"#p\",\"value\":\"149mp\"},{\"type\":\"#p\",\"value\":\"159\"},{\"type\":\"#p\",\"value\":\"159mp\"},{\"type\":\"#p\",\"value\":\"167\"},{\"type\":\"#p\",\"value\":\"167mp\"},{\"type\":\"#p\",\"value\":\"168\"},{\"type\":\"#p\",\"value\":\"168mp\"},{\"type\":\"#p\",\"value\":\"169\"},{\"type\":\"#p\",\"value\":\"169mp\"},{\"type\":\"#p\",\"value\":\"170\"},{\"type\":\"#p\",\"value\":\"170mp\"},{\"type\":\"#p\",\"value\":\"171\"},{\"type\":\"#p\",\"value\":\"171mp\"},{\"type\":\"#p\",\"value\":\"181\"},{\"type\":\"#p\",\"value\":\"181mp\"},{\"type\":\"#p\",\"value\":\"187\"},{\"type\":\"#p\",\"value\":\"187mp\"},{\"type\":\"#p\",\"value\":\"207\"},{\"type\":\"#p\",\"value\":\"207mp\"},{\"type\":\"#p\",\"value\":\"208\"},{\"type\":\"#p\",\"value\":\"208mp\"},{\"type\":\"#p\",\"value\":\"209\"},{\"type\":\"#p\",\"value\":\"209mp\"},{\"type\":\"#p\",\"value\":\"225\"},{\"type\":\"#p\",\"value\":\"225mp\"}],\"i_release_year\":[{\"type\":\"#i\",\"value\":2014}],\"alb_id\":[{\"type\":\"#p\",\"value\":\"3988854\"}],\"track_name\":[{\"type\":\"#p\",\"value\":\"\xd0\x92\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8\"}],\"z_grp_name\":{\"value\":\"\xd1\x85\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd1\x86\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2. \xd1\x85\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8e \xd1\x86\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb1\xd1\x83\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xba\xd1\x83 \xd0\xb4\xd1\x96\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb7\xd0\xb0\xd0\xbb\xd1\x96\xd0\xb7\xd0\xbd\xd0\xb8\xd1\x87\xd0\xbd\xd0\xb8\xd0\xba\xd1\x96\xd0\xb2\",\"type\":\"#z\"},\"alb_storage_dir\":[{\"type\":\"#p\",\"value\":\"d1a35fdc.a.3988854\"}],\"various\":[{\"type\":\"#p\",\"value\":\"0\"}],\"IsRemix\":[{\"type\":\"#f\",\"value\":0}],\"playlists_count_of_fast_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"IsTribute\":[{\"type\":\"#f\",\"value\":0}],\"IsLive\":[{\"type\":\"#f\",\"value\":0}],\"tracks_count_of_fast_skip_listening_on_recom\":[{\"type\":\"#f\",\"value\":0.00059988}],\"tracks_count_of_users\":[{\"type\":\"#f\",\"value\":0.00183829}],\"title\":{\"value\":\"\xd0\xa5\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd0\xa6\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2 \xd0\x92\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8\",\"type\":\"#z\"},\"tracks_radio_likes\":[{\"type\":\"#f\",\"value\":0.00141155}],\"artists_track_likes\":[{\"type\":\"#f\",\"value\":0}],\"z_performer\":{\"value\":\"\xd1\x85\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd1\x86\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2. \xd1\x85\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8e \xd1\x86\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb1\xd1\x83\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xba\xd1\x83 \xd0\xb4\xd1\x96\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb7\xd0\xb0\xd0\xbb\xd1\x96\xd0\xb7\xd0\xbd\xd0\xb8\xd1\x87\xd0\xbd\xd0\xb8\xd0\xba\xd1\x96\xd0\xb2. \xd1\x85\xd0\xbe\xd1\x80 \xd1\x86\xd0\xb4\xd0\xb4\xd0\xb6\",\"type\":\"#z\"},\"IsUgc\":[{\"type\":\"#f\",\"value\":0}],\"track_len\":[{\"type\":\"#p\",\"value\":\"126620\"}],\"artists_count_of_fast_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"IsPlaylist\":[{\"type\":\"#f\",\"value\":0}],\"alb_year\":[{\"type\":\"#p\",\"value\":\"2014\"}],\"track_youtube_url\":[{\"type\":\"#p\",\"value\":\"http://www.youtube.com/watch?v=sgJotDWQeBc\"}],\"AlbumVersionRank\":[{\"type\":\"#f\",\"value\":0}],\"i_type\":[{\"type\":\"#i\",\"value\":1}],\"FreshAlbum\":[{\"type\":\"#f\",\"value\":0}],\"TrackVersionRank\":[{\"type\":\"#f\",\"value\":0.5}],\"s_rights\":[{\"type\":\"#l\",\"value\":\"149\"},{\"type\":\"#l\",\"value\":\"149mp\"},{\"type\":\"#l\",\"value\":\"159\"},{\"type\":\"#l\",\"value\":\"159mp\"},{\"type\":\"#l\",\"value\":\"167\"},{\"type\":\"#l\",\"value\":\"167mp\"},{\"type\":\"#l\",\"value\":\"168\"},{\"type\":\"#l\",\"value\":\"168mp\"},{\"type\":\"#l\",\"value\":\"169\"},{\"type\":\"#l\",\"value\":\"169mp\"},{\"type\":\"#l\",\"value\":\"170\"},{\"type\":\"#l\",\"value\":\"170mp\"},{\"type\":\"#l\",\"value\":\"171\"},{\"type\":\"#l\",\"value\":\"171mp\"},{\"type\":\"#l\",\"value\":\"181\"},{\"type\":\"#l\",\"value\":\"181mp\"},{\"type\":\"#l\",\"value\":\"187\"},{\"type\":\"#l\",\"value\":\"187mp\"},{\"type\":\"#l\",\"value\":\"207\"},{\"type\":\"#l\",\"value\":\"207mp\"},{\"type\":\"#l\",\"value\":\"208\"},{\"type\":\"#l\",\"value\":\"208mp\"},{\"type\":\"#l\",\"value\":\"209\"},{\"type\":\"#l\",\"value\":\"209mp\"},{\"type\":\"#l\",\"value\":\"225\"},{\"type\":\"#l\",\"value\":\"225mp\"}],\"IsJunk\":[{\"type\":\"#f\",\"value\":0}],\"NormalizedPopularity\":[{\"type\":\"#f\",\"value\":0.46377}],\"z_title\":{\"value\":\"\xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8 \xd1\x81\xd0\xb8\xd0\xbd\xd0\xb8\xd0\xb5 \xd0\xbd\xd0\xbe\xd1\x87\xd0\xb8. \xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xb2\xd0\xbe\xd0\xb3\xd0\xbd\xd0\xb8\xd1\x89\xd0\xb0\xd0\xbc\xd0\xb8 \xd1\x81\xd0\xb8\xd0\xbd\xd1\x96 \xd0\xbd\xd0\xbe\xd1\x87\xd1\x96. \xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xba\xd0\xbe\xd1\x81\xd1\x82\xd1\x80\xd0\xb0\xd0\xbc\xd0\xb8. \xd0\xb2\xd0\xb7\xd0\xb2\xd0\xb5\xd0\xb9\xd1\x82\xd0\xb5\xd1\x81\xd1\x8c \xd0\xb2\xd0\xbe\xd0\xb3\xd0\xbd\xd0\xb8\xd1\x89\xd0\xb0\xd0\xbc\xd0\xb8\",\"type\":\"#z\"},\"IsRadio\":[{\"type\":\"#f\",\"value\":0}],\"tracks_count_of_non_skip_listening_on_recom\":[{\"type\":\"#f\",\"value\":0.00038593}],\"playlists_total_listening\":[{\"type\":\"#f\",\"value\":0}],\"z_album\":{\"value\":\"\xd1\x88\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd1\x83\xd1\x80\xd0\xbe\xd0\xba \xd0\xbe \xd1\x80\xd0\xbe\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xb5. \xd1\x88\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd1\x83\xd1\x80\xd0\xbe\xd0\xba \xd0\xbf\xd1\x80\xd0\xbe \xd0\xb1\xd0\xb0\xd1\x82\xd1\x8c\xd0\xba\xd1\x96\xd0\xb2\xd1\x89\xd0\xb8\xd0\xbd\xd1\x83\",\"type\":\"#z\"},\"s_lyrics_language\":[{\"type\":\"#l\",\"value\":\"ru\"}],\"albums_album_likes\":[{\"type\":\"#f\",\"value\":0}],\"IsArtist\":[{\"type\":\"#f\",\"value\":0}],\"IsGenericRadio\":[{\"type\":\"#f\",\"value\":0}],\"playlists_count_of_non_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"alb_cover\":[{\"type\":\"#p\",\"value\":\"1\"}],\"i_track_len\":[{\"type\":\"#i\",\"value\":126620}],\"z_alb_name\":{\"value\":\"\xd1\x88\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd1\x83\xd1\x80\xd0\xbe\xd0\xba \xd0\xbe \xd1\x80\xd0\xbe\xd0\xb4\xd0\xb8\xd0\xbd\xd0\xb5. \xd1\x88\xd0\xba\xd0\xbe\xd0\xbb\xd0\xb0. \xd1\x83\xd1\x80\xd0\xbe\xd0\xba \xd0\xbf\xd1\x80\xd0\xbe \xd0\xb1\xd0\xb0\xd1\x82\xd1\x8c\xd0\xba\xd1\x96\xd0\xb2\xd1\x89\xd0\xb8\xd0\xbd\xd1\x83\",\"type\":\"#z\"},\"artists_total_listening\":[{\"type\":\"#f\",\"value\":0}],\"albums_count_of_playlists\":[{\"type\":\"#f\",\"value\":0}],\"grp_id\":[{\"type\":\"#p\",\"value\":\"2854165\"}],\"i_genre\":[{\"type\":\"#i\",\"value\":11}],\"albums_count_of_non_skip_listening\":[{\"type\":\"#f\",\"value\":0}],\"IsGenre\":[{\"type\":\"#f\",\"value\":0}],\"grp_ex\":[{\"type\":\"#p\",\"value\":\"<artist id=\\\"2854165\\\" ><name>\xd0\xa5\xd0\xbe\xd1\x80 \xd0\xb0\xd0\xbd\xd1\x81\xd0\xb0\xd0\xbc\xd0\xb1\xd0\xbb\xd1\x8f \xd0\xa6\xd0\xb5\xd0\xbd\xd1\x82\xd1\x80\xd0\xb0\xd0\xbb\xd1\x8c\xd0\xbd\xd0\xbe\xd0\xb3\xd0\xbe \xd0\xb4\xd0\xbe\xd0\xbc\xd0\xb0 \xd0\xb4\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb9 \xd0\xb6\xd0\xb5\xd0\xbb\xd0\xb5\xd0\xb7\xd0\xbd\xd0\xbe\xd0\xb4\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb6\xd0\xbd\xd0\xb8\xd0\xba\xd0\xbe\xd0\xb2</name></artist>\"}],\"albums_track_likes\":[{\"type\":\"#f\",\"value\":0}],\"s_genre_bound_id\":[{\"type\":\"#l\",\"value\":\"pop\"}],\"tracks_count_of_fast_skip_listening\":[{\"type\":\"#f\",\"value\":0.00103428}]}]}"sv
;

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

static const TString textUkr("Інколи результати пошуку зручніше отримати не у вигляді списку "
                            "сторінок, а у вигляді зображень. Наприклад, коли ви шукаєте гарні "
                            "шпалери на робочий стіл, схеми для вишивання, ілюстрацію для "
                            "доповіді. У Яндекс.Зображеннях можна дізнатися, як виглядає "
                            "росомаха, або, скажімо, профітролі. Або місце, куди ви "
                            "збираєтеся поїхати в подорож."
                            "Щоб почати пошуку, необхідно просто набрати запит у пошуковому "
                            "рядку і натиснути на кнопку «Знайти!». Також ви можете перейти "
                            "на пошука за зображеннями з пошуку по вебу, клікнувши на посилання "
                            "«Зображення» над пошуковим рядком або на зображення праворуч "
                            "від результатів пошуку. ");

static const TString textKz( "Қыс қандай әсем еді, қара, далам, "
                            "Бір арман-ақ боз жеккен шана маған. "
                            "Ағызып жұлдыздайын "
                            "Дүниенің "
                            "Не жетсін күніне бір аралаған. "
                            "Қар қандай! "
                            "Қарып көзді шағылады, "
                            "Жамылған тау, "
                            "даласы, "
                            "бағы-бәрі. "
                            "Тоқтау жоқ, "
                            "Кедергі жоқ "
                            "Аппақ дүние! "
                            "Жер үсті аппақ нұрға малынады. "
                            "Күн ұзын жапалақтап тынбады қар, "
                            "Астында ақ мамықтың тұрғаным бар. "
                            "Ақ төсін жайып салып жатыр дала, "
                            "Кеудемде сол даланың ырғағы бар.");

static const TString textEng("The ad's headline and copy in Russian should be grammatically "
                            "correct and free of spelling errors. If the advertised website is in "
                            "any language other than Russian, the ad should explicitly inform about "
                            "this. The key words, to which the ad will be displayed, or catalogue "
                            "categories, where the ad will be placed, should match the ad's content. "
                            "Destination URL in the ad should match its content and should function "
                            "properly. You cannot use IP-address for domain name. The contact "
                            "information should be valid and correct, and the telephone information "
                            "should not differ from the information in the ad. If applicable, the "
                            "headline, the copy, or the destination URL cannot not contain: "
                            "spaces between letters in w o r d s;");

static const TString textTur("Ana sayfa "
                            "Yandex ana sayfasında nasıl listesi oluşturulur "
                            "Yandex ana sayfasında, farklı yöntemlerle seçilmiş popüler videolar "
                            "gösterilir. Bunları belirleyen algoritmalar, hostinglerde bulunan "
                            "videoların izlenme ve aldıklarımı yorum verileri, bloglarda alıntılanma "
                            "sayısı, kullanıcıların ilgisi ve benzeri diğer parametreler dikkate alınarak oluşturulur. "
                            "Widget "
                            "İlginç yeni videolardan her zaman haberdar olmak istiyorsanız Yandex "
                            "ana sayfasına küçük bir bilgi bloğu ekleyebilirsiniz: Yandex widgeti. "
                            "Widget verileri otomatik olarak günde bir kez güncellenecektir. ");

static const TString attrTur("Konu Dışı");

static const TString textRus("Поиск видео основан на информации, получаемой нами самостоятельно "
                            "в соответствии с лицензией на поиск, а также на данных, поступающих "
                            "от партнеров. Обработка видео-контента производится автоматически. "
                            "Это происходит с использованием аналогичных веб-поиску алгоритмов "
                            "ранжирования, разбора запросов, подавления спама и т.п., на основе "
                            "анализа названий, описаний, меток (тэгов) и прочих атрибутов роликов.");

START_TEST_DEFINE(TestRussianMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textRus);
    messages.back().MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckExistsByText("информация&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail";
    }
    return true;
}

bool InitConfig() override {
    SetMorphologyParams("ru");
    return true;
}
};

START_TEST_DEFINE(TestUkrainianMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textUkr);
    messages.back().MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckExistsByText("пошук&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail";
    }
    return true;
}

bool InitConfig() override {
    SetMorphologyParams("ukr");
    return true;
}
};

START_TEST_DEFINE(TestKazakhhMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textKz);
    messages.back().MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckExistsByText("жеккен&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail";
    }
    return true;
}
bool InitConfig() override {
    SetMorphologyParams("kaz");
    return true;
}
};

START_TEST_DEFINE(TestEnglishMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textEng);
    messages.back().MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckExistsByText("word&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail";
    }
    return true;
}
bool InitConfig() override {
    SetMorphologyParams("en");
    return true;
}
};

START_TEST_DEFINE(TestTurkishMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textTur);
    messages.back().MutableDocument()->SetLanguage("");
    auto attr = messages.back().MutableDocument()->AddSearchAttributes();
    attr->SetName("tag");
    attr->SetValue(attrTur);
    attr->SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckExistsByText("video&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail";
    }

    if (!CheckExistsByText("yontemlerle&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "epic fail number 2";
    }

    if (!CheckExistsByText("tag:\"Konu Dışı\"&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), false, messages)) {
        ythrow yexception() << "more epic fails";
    }
    return true;
}
bool InitConfig() override {
    SetMorphologyParams("tur");
    return true;
}
};

START_TEST_DEFINE(TestTurkishSynonyms)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), textTur);
    messages[0].MutableDocument()->SetBody("Derya Ozbey");
    messages[1].MutableDocument()->SetBody("Derya Özbey");
    messages[0].MutableDocument()->SetLanguage("");
    messages[1].MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("aaa&qtree=cHic7VfNixxFFK%2Fqj5numumxHVlIGoTJHmQIEScBYfEgGnKIIBrjRfZkD9067WZmQ7eHnTkteFn3YiBBQYOCEI%2FBCZKbFx3Bg%2F4jggdvIkJ8XV01XdVd272TXXATtk%2F19V69ev177%2FceuULaluN2zqAe7msD1EUeWkfn0SX0SttCLoJ11EcD9Jp51byG3kPvoxG%2BhdGXGH2L0QOMfsQIPu9Oh7xNMgELBFJF5pUwnvoe7i3VmYI6dBWl6kYP9xpcXSaw1DlAG%2FjyA8fCLvLMIN1aR304PsAg%2Bm5mSfx3k2R7oBqnijfQdby3qd27P7RALr3tuSGBkRb4MNaz8VZEx7areXhChy0Y6ot5zCbI02%2F4fNLIJzbsYK4I1yklMNQW81wyZsu6p93IJB04Yo4X81%2B%2BybcSvyTdBmkj4PY5cMocBz678RnX8JqL%2BWy5YLtNbsKqBuuywRN2H%2FbMXH0b1BvTrXBSeg29eCxYzEVaYKLOJVogAb6e1Z2zwfPMgnptXAR7uFYx%2FaWJ0rU2aDtO10kG6eC2gJ2hgFP5kLimp019dgh7eiBq3crXc0zq3On0Nv5GlDu5BTqVlxXgJwGf2ptwz5b1Lebj4hOp7Yl%2FVHOpGvHVxYtGaOdC%2FKuhDH396%2BOL%2FQ4ApQGToBzEWQzWBq6TJq%2BJEJk2f430YgF2bL0EH4rzpIwwBc4lP55CqR5KL3Ak4TOojymStL37m%2FheDqQR3tHiczni%2BDkkHLF20EyzLoyaN5u7GAGtef%2B0yTsFWmz8%2FHA2DKfAsrhXwYt%2F%2FPUS50UmIRPj521KjGwPmBEEKDN%2B0mXU2CeN7TDdlKylkM6EuM3x903ClupZNKyKJDqM1P8srPpnVF8kxIIYjEtJoIMwl4zyI0FUUp6FRchsJBAWXL%2B8UyAGU6QfQzKv7um6wrqDmOBwFtCdacCPlQgjzG%2Be1Z0Rqo%2BVf2Nc8ULJ%2F6ww4S%2FrgP2NaLacC3XJcXiUlSVhRVnCLzCW4zZcYIwjKW3zHVPaSRFZSkvUwxyRWUqOyhAmrrVU%2B%2FiBkIG2tE4z1m1DHbJK9jtpMSsk4pISkdRU0XsYtFMT4kCN4Ihhh%2FJyjp3GU4AKmXzWXevRI93d3TWAXIwUIuf%2FxOQyoyO9p1W2e5yHVM3eXUyGTI3NWK11M%2Fkw3B76SRgFVKN58eLLgw2qtnWgWlEK%2Bj8qItNc39LSP0V31lsD%2BtEJEBrOdKVkN0P5u1O5fLT5GbF2rTVq9782eavYpFI%2BF3xQ16MG5R71K1vuUTPLbmuMh%2BuLhvgns1zKant3KAFLCVaqqhwArZlWp1VAV%2BVnv5Cfj9w3Cvc9TttIeW4cJWzSSCeHa%2BIm0Sp9HwERLTmobji%2Bhxer9goK5w4w81OrVbtyAjUVKbaYp7ijK3OZX5PLHMhlZuJPBEq6patBTCnpiCgutFEMIwrAUIMVqMhSeulPyWAReeZp%2BCUFPjDSzNc3Nu86y4z4qUOuHdifVKXEuvbku2J7guXm5JKyOeFZUVe2KT%2Bo2hSeJh1wtLnNz5%2FQwue0WTltVlZpVgrJIa9KqecPlTX%2B96I0%2FkLVqiyJ4YmI25PXsEgM%2BIQio0RPmqv1DZjt43OEzrpnLeytWdra77%2B9%2BerzKbn00IsZx%2BzjsyQlNK37rGVddyzctV8PPnoj8T8IP97HbbqHuobViVE2TdXRKdz1H8wQWiE%3D", results);
    CHECK_TEST_EQ(results.size(), 1);

    messages[0].MutableDocument()->SetLanguage("tur");
    messages[1].MutableDocument()->SetLanguage("tur");

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    QuerySearch("aaa&qtree=cHic7VfNixxFFK%2Fqj5numumxHVlIGoTJHmQIEScBYfEgGnKIIBrjRfZkD9067WZmQ7eHnTkteFn3YiBBQYOCEI%2FBCZKbFx3Bg%2F4jggdvIkJ8XV01XdVd272TXXATtk%2F19V69ev177%2FceuULaluN2zqAe7msD1EUeWkfn0SX0SttCLoJ11EcD9Jp51byG3kPvoxG%2BhdGXGH2L0QOMfsQIPu9Oh7xNMgELBFJF5pUwnvoe7i3VmYI6dBWl6kYP9xpcXSaw1DlAG%2FjyA8fCLvLMIN1aR304PsAg%2Bm5mSfx3k2R7oBqnijfQdby3qd27P7RALr3tuSGBkRb4MNaz8VZEx7areXhChy0Y6ot5zCbI02%2F4fNLIJzbsYK4I1yklMNQW81wyZsu6p93IJB04Yo4X81%2B%2BybcSvyTdBmkj4PY5cMocBz678RnX8JqL%2BWy5YLtNbsKqBuuywRN2H%2FbMXH0b1BvTrXBSeg29eCxYzEVaYKLOJVogAb6e1Z2zwfPMgnptXAR7uFYx%2FaWJ0rU2aDtO10kG6eC2gJ2hgFP5kLimp019dgh7eiBq3crXc0zq3On0Nv5GlDu5BTqVlxXgJwGf2ptwz5b1Lebj4hOp7Yl%2FVHOpGvHVxYtGaOdC%2FKuhDH396%2BOL%2FQ4ApQGToBzEWQzWBq6TJq%2BJEJk2f430YgF2bL0EH4rzpIwwBc4lP55CqR5KL3Ak4TOojymStL37m%2FheDqQR3tHiczni%2BDkkHLF20EyzLoyaN5u7GAGtef%2B0yTsFWmz8%2FHA2DKfAsrhXwYt%2F%2FPUS50UmIRPj521KjGwPmBEEKDN%2B0mXU2CeN7TDdlKylkM6EuM3x903ClupZNKyKJDqM1P8srPpnVF8kxIIYjEtJoIMwl4zyI0FUUp6FRchsJBAWXL%2B8UyAGU6QfQzKv7um6wrqDmOBwFtCdacCPlQgjzG%2Be1Z0Rqo%2BVf2Nc8ULJ%2F6ww4S%2FrgP2NaLacC3XJcXiUlSVhRVnCLzCW4zZcYIwjKW3zHVPaSRFZSkvUwxyRWUqOyhAmrrVU%2B%2FiBkIG2tE4z1m1DHbJK9jtpMSsk4pISkdRU0XsYtFMT4kCN4Ihhh%2FJyjp3GU4AKmXzWXevRI93d3TWAXIwUIuf%2FxOQyoyO9p1W2e5yHVM3eXUyGTI3NWK11M%2Fkw3B76SRgFVKN58eLLgw2qtnWgWlEK%2Bj8qItNc39LSP0V31lsD%2BtEJEBrOdKVkN0P5u1O5fLT5GbF2rTVq9782eavYpFI%2BF3xQ16MG5R71K1vuUTPLbmuMh%2BuLhvgns1zKant3KAFLCVaqqhwArZlWp1VAV%2BVnv5Cfj9w3Cvc9TttIeW4cJWzSSCeHa%2BIm0Sp9HwERLTmobji%2Bhxer9goK5w4w81OrVbtyAjUVKbaYp7ijK3OZX5PLHMhlZuJPBEq6patBTCnpiCgutFEMIwrAUIMVqMhSeulPyWAReeZp%2BCUFPjDSzNc3Nu86y4z4qUOuHdifVKXEuvbku2J7guXm5JKyOeFZUVe2KT%2Bo2hSeJh1wtLnNz5%2FQwue0WTltVlZpVgrJIa9KqecPlTX%2B96I0%2FkLVqiyJ4YmI25PXsEgM%2BIQio0RPmqv1DZjt43OEzrpnLeytWdra77%2B9%2BerzKbn00IsZx%2BzjsyQlNK37rGVddyzctV8PPnoj8T8IP97HbbqHuobViVE2TdXRKdz1H8wQWiE%3D", results);
    CHECK_TEST_EQ(results.size(), 2);

    return true;
}
bool InitConfig() override {
    SetMorphologyParams("tur");
    return true;
}
};

START_TEST_DEFINE(TestMultiLangMorphology)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textUkr);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textKz);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textEng);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textTur);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textRus);
    for(TVector<NRTYServer::TMessage>::iterator i = messages.begin(), e = messages.end(); i != e; ++i)
        i->MutableDocument()->SetLanguage("");
    IndexMessages(messages, DISK, 1);

    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    TString keyPrefix = "&kps=" + ToString(messages[4].GetDocument().GetKeyPrefix());
    QuerySearch("информация" + keyPrefix, results);
    if (results.size() != 1) {
        ythrow yexception() << "epic fail Russian";
    }

    keyPrefix = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    QuerySearch("пошук" + keyPrefix, results);
    if (results.size() != 1) {
        ythrow yexception() << "epic fail Ukrainian";
    }

    keyPrefix = "&kps=" + ToString(messages[2].GetDocument().GetKeyPrefix());
    QuerySearch("word" + keyPrefix, results);
    if (results.size() != 1) {
        ythrow yexception() << "epic fail English";
    }

    keyPrefix = "&kps=" + ToString(messages[1].GetDocument().GetKeyPrefix());
    QuerySearch("жеккен" + keyPrefix, results);
    if (results.size() != 1) {
        ythrow yexception() << "epic fail Kazakh";
    }

    keyPrefix = "&kps=" + ToString(messages[3].GetDocument().GetKeyPrefix());
    QuerySearch("video" + keyPrefix, results);
    if (results.size() != 1) {
        ythrow yexception() << "epic fail Turkish";
    }
    return true;
}
bool InitConfig() override {
    SetMorphologyParams("ukr,kaz,tur,en,rus");
    (*ConfigDiff)["PreferedMorphologyLanguages"] = "ukr,kaz,tur,en,rus";
    (*SPConfigDiff)["Service.MetaSearch.PreferedMorphologyLanguages"] = "ukr,kaz,tur,en,rus";
    return true;
}
};

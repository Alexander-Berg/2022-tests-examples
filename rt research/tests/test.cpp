#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <rt-research/broadmatching/scripts/cpp-source/xml_parser/xml_parser.h>
#include <rt-research/broadmatching/scripts/cpp-source/xml_parser/ampersand_parser.h>
#include <rt-research/broadmatching/scripts/cpp-source/xml_parser/utf8_parser.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/streams/factory/factory.h>

#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/string/split.h>

#include <sstream>

void AssertNoErrorNoWarning(const TMaybe<TErrorOrWarning>& data) {
    UNIT_ASSERT(!data.Defined());
}

void AssertContains(const TMaybe<TErrorOrWarning>& data, const TStringBuf messageLike, const TStringBuf messageRuLike) {
    UNIT_ASSERT(data.Defined());
    UNIT_ASSERT_STRING_CONTAINS(data->message.en, messageLike);
    UNIT_ASSERT_STRING_CONTAINS(data->message.ru, messageRuLike);
}

void AssertErrorContains(const TMaybe<TErrorOrWarning>& data, const TStringBuf messageLike, const TStringBuf messageRuLike) {
    AssertContains(data, messageLike, messageRuLike);
    UNIT_ASSERT_C(!data->is_warning, "expected error");
}

void AssertWarningContains(const TMaybe<TErrorOrWarning>& data, const TStringBuf messageLike, const TStringBuf messageRuLike) {
    AssertContains(data, messageLike, messageRuLike);
    UNIT_ASSERT_C(data->is_warning, "expected warning");
}

void CompareFiles(const TString& outputPath, const TString& expectedPath, const ui64 lines=0) {
    TFileInput output(outputPath);
    TFileInput expected(expectedPath);

    bool outputRead = true;
    bool expectedRead = true;
    ui64 line = 1;

    for (; outputRead && expectedRead; ++line) {
        TString outputLine;
        TString expectedLine;

        outputRead = output.ReadLine(outputLine);
        expectedRead = expected.ReadLine(expectedLine);

        UNIT_ASSERT_EQUAL(outputRead, expectedRead);
        UNIT_ASSERT_EQUAL_C(outputLine, expectedLine, (TStringBuilder{} << line));
    }

    if (lines) {
        UNIT_ASSERT_EQUAL(line, lines);
    }
}

Y_UNIT_TEST_SUITE(BaseSuite) {
    Y_UNIT_TEST(CategoryTest) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/input.xml"
        );
        TString outputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/categs.tsv"
        );

        const TVector<TString> search = {"category"};
        AssertNoErrorNoWarning(XMLParser::Grep(inputPath.Data(), "categs.tsv", search.begin(), search.end()));

        CompareFiles(outputPath, "categs.tsv");
    }
    Y_UNIT_TEST(OfferTest) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/input.xml"
        );
        TString outputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/offer.tsv"
        );

        const TVector<TString> search = {"offer"};
        AssertNoErrorNoWarning(XMLParser::Grep(inputPath.Data(), "offer.tsv", search.begin(), search.end()));

        CompareFiles(outputPath, "offer.tsv");
    }
}

Y_UNIT_TEST_SUITE(AmpersandTestSuite) {
    Y_UNIT_TEST(AmpersandTestWeak) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/bad_ampersand.xml"
        );
        TString outputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/bad_ampersand_offer.tsv"
        );

        const TVector<TString> search = {"offer"};
        AssertWarningContains(
            XMLParser::Grep(inputPath.Data(), "bad_ampersand_offer.tsv", search.begin(), search.end(), false),
            "&amp;",
            ""
        );

        CompareFiles(outputPath, "bad_ampersand_offer.tsv");
    }
    Y_UNIT_TEST(AmpersandTestStrong) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/bad_ampersand.xml"
        );
        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::Grep(inputPath.Data(), "offer.tsv", search.begin(), search.end(), true),
            "&amp;",
            ""
        );
    }
    Y_UNIT_TEST(InCData) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/bad_ampersand2.xml"
        );
        const TVector<TString> search = StringSplitter("offer").Split(',');
        AssertNoErrorNoWarning(XMLParser::Grep(inputPath.Data(), "offer.tsv", search.begin(), search.end(), true));
    }
}

Y_UNIT_TEST_SUITE(Utf8TestSuite) {
    const TVector<std::pair<TString, bool /* shouldPanic */>> UTF8_TEST_STRINGS = {
        // Wrong strings
        {"\xFE", true}, // Wrong start
        {"\xE7.23", true}, // Wrong continuation
        {"\xF7\xEA\x01\x25\xDD", true}, // Wrong continuation
        {"\xF7", true}, // Wrong size

        // Correct strings
        {"\xC4\xA5", false},
        {"\xE9\xB1\x89", false},
        {"\xF0\x96\xBC\x9A", false}
    };

    const TString XML_FILE_START = NResource::Find("bad_utf8.xml.start");
    const TString XML_FILE_END = NResource::Find("bad_utf8.xml.end");

    void TestFile(const TString& content, bool shouldPanic) {
        const TVector<TString> search = {"offer"};

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        if (shouldPanic) {
            AssertErrorContains(
                XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"),
                "Feed's file must be UTF-8 encoded, but violation of that encoding found: these bytes represent wrong UTF-8 sequence",
                "Файл фида должен быть закодирован в UTF-8, но обнаружено нарушение этой кодировки: данные байты представляют неправильную UTF-8 последовательность"
            );
        } else {
            AssertNoErrorNoWarning(XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"));
        }
    }

    Y_UNIT_TEST(InDescription) {
        for (auto& [string, shouldPanic] : UTF8_TEST_STRINGS) {
            TestFile(XML_FILE_START + string + XML_FILE_END, shouldPanic);
        }
    }

    Y_UNIT_TEST(OnBufferBorder) {
        const unsigned long BUF_SIZE = 8192;  
        TString bufSizeSpaces = TString(BUF_SIZE - (XML_FILE_START.size() % BUF_SIZE) - 5, ' ');

        for (auto& [string, shouldPanic] : UTF8_TEST_STRINGS) {
            for (int i = 0; i < 10; ++i) {
                TestFile(XML_FILE_START + bufSizeSpaces + TString(i, ' ') + string + XML_FILE_END, shouldPanic);
            }
        }
    }

    Y_UNIT_TEST(InEnd) {
        for (auto& [string, shouldPanic] : UTF8_TEST_STRINGS) {
            if (shouldPanic) {
                TestFile(XML_FILE_START + string, true /* shouldPanic */);
            }
        }
    }

    Y_UNIT_TEST(Utf8WrongRealFeed) {
        TestFile(NResource::Find("bad_utf8_2.xml"), true /* shouldPanic */);
    }
}

bool CheckAmp(const TString& input) {
    TAmpersandParser parser = TAmpersandParser();
    for (char ch : input) {
        auto result = parser.ProcessChar(ch);
        if (result.Defined()) {
            return false;
        }
    }
    auto result = parser.Finish();
    return !result.Defined();
}

void AssertAmpErr(const TString& input, const TString& err) {
    TAmpersandParser parser = TAmpersandParser();
    for (char ch : input) {
        auto result = parser.ProcessChar(ch);
        if (result.Defined()) {
            UNIT_ASSERT_NO_DIFF(err, result->en);
            return;
        }
    }
    auto result = parser.Finish();
    if (result.Defined()) {
        UNIT_ASSERT_NO_DIFF(err, result->en);
        return;
    }
    UNIT_FAIL("No error occured.");
}

Y_UNIT_TEST_SUITE(AmpersandParserTestSuite) {
    Y_UNIT_TEST(BadAmpersandCode) {
        UNIT_ASSERT(!CheckAmp("&"));
        UNIT_ASSERT(!CheckAmp("&amp"));
        UNIT_ASSERT(!CheckAmp("&am;"));
        UNIT_ASSERT(!CheckAmp("&a;"));
        UNIT_ASSERT(!CheckAmp("&#;"));
        UNIT_ASSERT(!CheckAmp("&aposapos;"));
        UNIT_ASSERT(!CheckAmp("&&amp;"));
        UNIT_ASSERT(!CheckAmp("&amp&amp;&amp"));
        UNIT_ASSERT(!CheckAmp("amp&amp;&amp"));
        UNIT_ASSERT(!CheckAmp("&lt;lol&"));
        UNIT_ASSERT(!CheckAmp("&gto;"));
        UNIT_ASSERT(!CheckAmp("&ndash;")); // defined in html, but not in xml
        AssertAmpErr("&#xC;", "Forbidden char with code 12 found in &#xHHHH; element."); // Non printable symbol by xml spec
        AssertAmpErr("&#11;", "Forbidden char with code 11 found in &#NNNN; element."); // Non printable symbol by xml spec
        AssertAmpErr("&#xFFFFFFFF;", "Expected number smaller than 1114111 in &#xHHHH; element."); // Too big number
        AssertAmpErr("&#999999999;", "Expected number smaller than 1114111 in &#NNNN; element."); // Too big number
    }
    Y_UNIT_TEST(GoodAmpersandCode) {
        UNIT_ASSERT(CheckAmp(""));
        UNIT_ASSERT(CheckAmp("qwerty"));
        UNIT_ASSERT(CheckAmp("&amp;"));
        UNIT_ASSERT(CheckAmp("&amp;lol"));
        UNIT_ASSERT(CheckAmp("&quot;"));
        UNIT_ASSERT(CheckAmp("&lt;"));
        UNIT_ASSERT(CheckAmp("&lt;lol&amp;"));
        UNIT_ASSERT(CheckAmp("&#xD;&#xD;"));
        UNIT_ASSERT(CheckAmp("&#xA;"));
        UNIT_ASSERT(CheckAmp("&#xABC;"));
        UNIT_ASSERT(CheckAmp("&#123456;"));
        UNIT_ASSERT(CheckAmp("&#32;"));
    }
}

Y_UNIT_TEST_SUITE(CycleTestSuite) {
    Y_UNIT_TEST(RealData) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/cycle.xml"
        );

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::Grep(inputPath.Data(), "bad_ampersand_offer.tsv", search.begin(), search.end(), true),
            "it must not contain itself as a parent",
            "категория не должна являться собственным родителем"
        );
    }

    Y_UNIT_TEST(ComplexCycle) {
        TString inputPath = JoinFsPaths(
            ArcadiaSourceRoot(),
            "rt-research/broadmatching/scripts/cpp-source/xml_parser/tests/cycle2.xml"
        );

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::Grep(inputPath.Data(), "bad_ampersand_offer.tsv", search.begin(), search.end(), true),
            "Detected cycle in category tree",
            "Обнаружен цикл в дереве категорий"
        );
    }
}

bool CheckUtf8(const TString& input) {
    auto parser = TUtf8Parser();
    for (char ch : input) {
        auto result = parser.ProcessChar(static_cast<unsigned char>(ch));
        if (result.GetError().Defined()) {
            return false;
        }
    }
    return std::holds_alternative<int>(parser.Finish());
}

Y_UNIT_TEST_SUITE(Utf8ParserTestSuite) {
    Y_UNIT_TEST(BadUtf8) {
        UNIT_ASSERT(!CheckUtf8("Test \xFE")); // Wrong start
        UNIT_ASSERT(!CheckUtf8("\xE7.23")); // Wrong continuation
        UNIT_ASSERT(!CheckUtf8("Ascii \xF7\xEA\x01\x25\xDD")); // Wrong continuation
        UNIT_ASSERT(!CheckUtf8("Русский \xF7")); // Unexpected end
        UNIT_ASSERT(!CheckUtf8("Text \xF7 continue"));
        UNIT_ASSERT(!CheckUtf8("\x03")); // Non printable symbol by xml spec
    }
    Y_UNIT_TEST(GoodUtf8) {
        UNIT_ASSERT(CheckUtf8(""));
        UNIT_ASSERT(CheckUtf8("ascii string test"));
        UNIT_ASSERT(CheckUtf8("Русский текст тоже валидный UTF-8"));
        UNIT_ASSERT(CheckUtf8("\xC4\xA5")); // One two-byte symbol
        UNIT_ASSERT(CheckUtf8("Текст \xE9\xB1\x89 что-то"));
        UNIT_ASSERT(CheckUtf8("\xF0\x96\xBC\x9A")); // One four-byte symbol
        UNIT_ASSERT(CheckUtf8("😎😆😆🌚🌚😔🤡👩‍🏫👩‍🌾👬👨‍👩‍👧💑🕴"));
        UNIT_ASSERT(CheckUtf8("漢語又稱華語"));
    }
}

Y_UNIT_TEST_SUITE(DuplicatesTestSuite) {
    Y_UNIT_TEST(DuplicatePrice) {
        TString content = NResource::Find("duplicates.xml");

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"), 
            "Found duplicate tag",
            "Обнаружен дубликат тега"
        );
    }

    Y_UNIT_TEST(NoDuplicates) {
        TString content = NResource::Find("duplicates-no.xml");

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        const TVector<TString> search = {"offer"};
        AssertNoErrorNoWarning(
            XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket")
        );
    }
}

Y_UNIT_TEST_SUITE(UnknownTagsSuite) {
    Y_UNIT_TEST(FirstTag) {
        TString content = NResource::Find("tag_yml.xml");

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"), 
            "Wrong path to tag `offer`: `yml/shop/offers/offer`, it must be: `yml_catalog/shop/offers/offer`.",
            "Неверный путь к тегу `offer`: `yml/shop/offers/offer`, должен быть: `yml_catalog/shop/offers/offer`."
        );
    }

    Y_UNIT_TEST(SecondTag) {
        TString content = NResource::Find("tag_shop.xml");

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"), 
            "Wrong path to tag `offer`: `yml_catalog/offers/offer`, it must be: `yml_catalog/shop/offers/offer`.",
            "Неверный путь к тегу `offer`: `yml_catalog/offers/offer`, должен быть: `yml_catalog/shop/offers/offer`."
        );
    }

    Y_UNIT_TEST(ThirdTag) {
        TString content = NResource::Find("tag_offers.xml");

        std::istringstream inputStream(content);
        std::ostringstream outputStream;

        const TVector<TString> search = {"offer"};
        AssertErrorContains(
            XMLParser::GrepGeneral(&inputStream, &outputStream, search.begin(), search.end(), true, "YandexMarket"), 
            "Wrong path to tag `offer`: `yml_catalog/shop/offer`, it must be: `yml_catalog/shop/offers/offer`.",
            "Неверный путь к тегу `offer`: `yml_catalog/shop/offer`, должен быть: `yml_catalog/shop/offers/offer`."
        );
    }
}

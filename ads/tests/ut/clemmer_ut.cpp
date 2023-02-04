#include <library/cpp/testing/unittest/registar.h>

#include "ads/clemmer/lib/clemmer.h"

struct TTestItem {
    char const* const* mRes;
    clemmer_result mVal;

    inline TTestItem(char const* const* _test)
        : mRes(_test)
    {
        char const* src = *(mRes++);
        mVal = clemmer_analyze(src, strlen(src), nullptr, nullptr);
    }

    int Test() const;

    inline ~TTestItem() {
        clemmer_free_result(mVal);
    }
};

#define _CLEMMER_UNIT_TEST_DATA(name, strs...) \
    static char const* Data##name[] = {strs}

_CLEMMER_UNIT_TEST_DATA(TestRus, "цветов", "цветов", "цветок", "цвет", NULL);
_CLEMMER_UNIT_TEST_DATA(TestUkr, "їжею", "їжею", "їжа", NULL);
_CLEMMER_UNIT_TEST_DATA(TestEng, "sent", "sent", "send", NULL);

int TTestItem::Test() const {
    char const* const* d = mRes;

    if (!mVal)
        return 1;
    if (strcmp(mVal->text, *(d++)))
        return 2;
    if (mVal->next)
        return 3;

    for (clemmer_lemma* lemma = mVal->lemmas; lemma; lemma = lemma->next) {
        if (!*d)
            return 4;
        if (strcmp(lemma->text, *(d++)))
            return 5;
    }

    if (*d)
        return 6;

    return 0;
}

#define _CLEMMER_UNIT_TEST_IMPL(name) \
    inline void name() {              \
        Test(Data##name);             \
    }

class TClemmerTest: public TTestBase {
    UNIT_TEST_SUITE(TClemmerTest);
    UNIT_TEST(TestRus);
    UNIT_TEST(TestUkr);
    UNIT_TEST(TestEng);
    UNIT_TEST_SUITE_END();

    inline void Test(char const* const* data) {
        TTestItem Item(data);
        int res = Item.Test();
        UNIT_ASSERT_VALUES_EQUAL(res, 0);
    }

public:
    _CLEMMER_UNIT_TEST_IMPL(TestRus);
    _CLEMMER_UNIT_TEST_IMPL(TestUkr);
    _CLEMMER_UNIT_TEST_IMPL(TestEng);
};

UNIT_TEST_SUITE_REGISTRATION(TClemmerTest);

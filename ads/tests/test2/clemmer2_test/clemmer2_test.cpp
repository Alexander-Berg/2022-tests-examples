#include "cstdio"
#include "cstring"
#include "cstdlib"

#include "ads/clemmer/lib/clemmer.h"

int main(int argc, char* argv[]) {
    char _buf[4096] = {};
    char *buf = _buf;
    size_t len = sizeof(_buf);
    ssize_t res;
    int i;
    clemmer2_lang lang = LEM_LANG_RUS;

    for (i = 1; i < argc; i++)
        if (!strcmp(argv[i], "unk") || !strcmp(argv[i], "none"))
            lang = LEM_LANG_UNK;
        else if (!strcmp(argv[i], "rus") || !strcmp(argv[i], "ru"))
            lang = LEM_LANG_RUS;
        else if (!strcmp(argv[i], "ukr") || !strcmp(argv[i], "uk"))
            lang = LEM_LANG_UKR;
        else if (!strcmp(argv[i], "eng") || !strcmp(argv[i], "en"))
            lang = LEM_LANG_ENG;
        else if (!strcmp(argv[i], "blr") || !strcmp(argv[i], "by"))
            lang = LEM_LANG_BEL;
        else if (!strcmp(argv[i], "kaz") || !strcmp(argv[i], "kz"))
            lang = LEM_LANG_KAZ;
        else if (!strcmp(argv[i], "tat") || !strcmp(argv[i], "tt"))
            lang = LEM_LANG_TAT;
        else if (!strcmp(argv[i], "tur") || !strcmp(argv[i], "tr"))
            lang = LEM_LANG_TUR;
        else if (!strcmp(argv[i], "uzb") || !strcmp(argv[i], "uz"))
            lang = LEM_LANG_UZB;
        else {
            fprintf(stderr, "Unknown language '%s'\n", argv[i]);
            return 1;
        }

    while ((res = getline(&buf, &len, stdin)) >= 0) {
        unsigned int l = strlen(buf);
        if (l && buf[l - 1] == '\n')
            buf[l - 1] = '\0';

        printf("** Phrase:\n    \"%s\"\n", buf);

        clemmer2_result result = clemmer2_analyze(buf, res, lang);

        for (clemmer2_word* word = result; word; word = word->next) {
            printf("** Word:\n    %s%s\"%s\"\n",
                   word->flags & FLAG_MINUS ? "-" : "",
                   word->flags & FLAG_EXACT ? "!" : "",
                   word->text);

            printf("** Lemmas:\n");

            for (clemmer2_lemma* lemma = word->lemmas; lemma; lemma = lemma->next)
                printf("    \"%s\"\n", lemma->text);

            printf("** Formas:\n");
            for (clemmer2_forma* forma = word->formas; forma; forma = forma->next)
                printf("    \"%s\"\n", forma->text);
        }
        clemmer2_free_result(result);
    }
}

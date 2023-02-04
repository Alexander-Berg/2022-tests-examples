#include "ads/clemmer/lib/clemmer_1.h"

static bool clemmerCacheUse = true;

static void clemmer_cache_print(clemmer_cache* cache) {
    fprintf(stderr, "Cache stats: num = %lu, total = %lu, hits = %lu\n", cache->num, cache->total, cache->hits);

    clemmer_cache::node_t** nodep = cache->nodes;
    size_t info[5] = {0, 0, 0, 0, 0};

    for (size_t i = 0; i < cache->size; ++i) {
        clemmer_cache::node_t* node = *(nodep++);
        size_t n = 0;
        while (node) {
            ++n;
            node = node->next;
        }

        if (n > 4)
            n = 4;
        info[n]++;
    }

    fprintf(stderr, "%lu, %lu, %lu, %lu, %lu\n", info[0], info[1], info[2], info[3], info[4]);
}

int main(int argc, char* argv[]) {
    char* buf = (char*)malloc(4096);
    size_t len = 4096;
    ssize_t res;

    for (int i = 1; i < argc; ++i) {
        if (!strcmp(argv[i], "notrace"))
            clemmerTrace = false;
        else if (!strcmp(argv[i], "nocache"))
            clemmerCacheUse = false;
        else if (!strcmp(argv[i], "extcheck"))
            clemmerExtCheck = true;
        else {
            fprintf(stderr, "Usage: %s [notrace] [nocache] [extcheck]\n", argv[0]);
            return 1;
        }
    }

    clemmer_cache* cache = nullptr;
    if (clemmerCacheUse)
        cache = clemmer_cache_create(1024 * 1024);

    while ((res = getline(&buf, &len, stdin)) >= 0) {
        if (res && buf[res - 1] == '\n')
            buf[--res] = '\0';

        printf("Phrase: \"%s\"\n", buf);

        clemmer_result result = clemmer_analyze_impl<true>(buf, res, "-!~", cache);

        for (clemmer_word* word = result; word; word = word->next) {
            char const* m = word->flags & (1 << 0) ? "-" : "";
            char const* e = word->flags & (1 << 1) ? "!" : "";
            char const* t = word->flags & (1 << 2) ? "~" : "";

            printf("%s%s%s\"%s\"\n", m, e, t, word->text);
            for (clemmer_lemma* lemma = word->lemmas; lemma; lemma = lemma->next) {
                printf("    \"%s\"\n", lemma->text);
            }
        }

        clemmer_free_result(result);
    }

    if (cache) {
        clemmer_cache_print(cache);
        clemmer_cache_destroy(cache);
    }

    free(buf);

    return 0;
}

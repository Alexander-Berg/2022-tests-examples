#include <cstdint>
#include <cstdlib>
#include <cstring>

int main(int argc, char* argv[]) {
    (void)(argc);
    (void)(argv);

    const size_t size = 32 << 10;

    uint8_t* ptr = (uint8_t*)malloc(size);
    if (!ptr) {
        return 1;
    }

    memset(ptr, 0x55, size);

    ptr = (uint8_t*)realloc(ptr, size * 2);
    if (!ptr) {
        return 2;
    }

    for (size_t i = 0; i < size; i++) {
        if (ptr[i] != 0x55) {
            return 3;
        }
    }

    memset(ptr, 0xAA, size * 2);

    for (size_t i = 0; i < size * 2; i++) {
        if (ptr[i] != 0xAA) {
            return 4;
        }
    }

    free(ptr);

    ptr = (uint8_t*)calloc(1, size);
    if (!ptr) {
        return 5;
    }

    for (size_t i = 0; i < size; i++) {
        if (ptr[i] != 0x00) {
            return 6;
        }
    }

    free(ptr);
}

#include <kernel/matrixnet/mn_dynamic.h>

#include <util/stream/file.h>

int main(int argc, const char* argv[]) {
    try {
        NMatrixnet::TMnSseDynamic mn;
        if (argc < 2) {
            mn.Load(&Cin);
        } else {
            TFileInput input(argv[1]);
            mn.Load(&input);
        }
        return 0;

    } catch (...) {
        Cerr << CurrentExceptionMessage() << Endl;
        return 1;
    }
}


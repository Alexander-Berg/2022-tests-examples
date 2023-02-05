#include <iostream>
#include <sstream>
#include <fstream>
#include <thread>
#include <vector>
#include <chrono>

#include <yandex/maps/fontutils/glyph.h>
#include <yandex/maps/fontutils/font_face.h>
#include <yandex/maps/fontutils/exception.h>

namespace fu = maps::fontutils;

fu::Glyph renderGlyph(
    const fu::FontFace& face,
    size_t index)
{
    return face.render(
        index,
        50,
        fu::RenderType::MonochromeWhiteOnBlack
    )
        .addPaddings(15)
        .convertToSignedDistanceField()
        .scale(0.5);
}

void f(const std::string& path, int threadIndex, int threadCount)
{
    fu::FontFace face(path);

    for (size_t index = 0; index < face.glyphsCount(); ++index) {
        if (index % threadCount != (size_t)threadIndex) {
            continue;
        }
        try {
            auto glyph = renderGlyph(face, index);
            glyph.width();
        } catch (...) {
            std::cout << "error rendering glyph with index " << index << std::endl;
        }
    }

}

void check(size_t threadCount)
{
    // std::string path = "/usr/share/yandex/maps/renderer5/fonts/LiberationSans-Regular.ttf";
    std::string path = "/usr/share/yandex/maps/renderer5/fonts/arial.ttf";

    fu::FontFace face(path);
    std::cout << "number of glyphs: " << face.glyphsCount() << std::endl;
    std::cout << "testing on " << threadCount << " threads" << std::endl;

    auto begin = std::chrono::high_resolution_clock::now();

    std::vector<std::thread> threads;
    for (size_t i = 0; i < threadCount; ++i) {
        threads.push_back(std::thread([i, threadCount, &path](){
            f(path, i, threadCount);
        }));
    }
    for (auto& th: threads) {
        th.join();
    }
    auto end = std::chrono::high_resolution_clock::now();
    auto total = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();
    std::cout << "Total time is " << total << " ms " << std::endl;
}

int main()
{
    for (size_t threadCount: {1, 2, 4, 8, 16}) {
        check(threadCount);
    }
    return 0;
}

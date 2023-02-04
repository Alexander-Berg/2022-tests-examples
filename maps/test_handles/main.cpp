#include <maps/libs/fuzzing/include/handle_tester.h>
#include <maps/infra/yacare/include/yacare.h>
#include <maps/infra/yacare/include/params/scale.h>
#include <maps/infra/yacare/include/params/tile.h>
#include <fmt/format.h>


using namespace maps::fuzzing;


YCR_QUERY_PARAM(id,     std::string);
YCR_QUERY_PARAM(zmin,   size_t);
YCR_QUERY_PARAM(zmax,   size_t);
YCR_QUERY_PARAM(range,  std::vector<size_t>);


YCR_RESPOND_TO("GET /handle1", x, y, z, l, v)
{
}

YCR_RESPOND_TO("GET /handle2", id, range, scale)
{
}

YCR_RESPOND_TO("GET /handle3", x, y, z, zmin, zmax)
{
}

YCR_RESPOND_TO("GET /handle4", ll)
{
}

std::string ll(FuzzedDataProvider& fdp)
{
    return fmt::format("{:.6f},{:.6f}",
                       fuzzed<float>(fdp, -180.0, 180.0),
                       fuzzed<float>(fdp, -90.0, 90.0));
}


extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)
{
    HandleTester t(data, size);
    t.addHandle("/handle1")
        .addParamInt("x", 0, 32000)
        .addParamInt("y", 0, 32000)
        .addParamInt("z", 0, 32)
        .addParam("l", {"map", "skl", "map,skl"})
        .addParamString("v")
    ;

    t.addHandle("/handle2")
        .addParam<std::string>("id")
        .addParamList<size_t>("range", 0, 1024, 2)
        .addParam<double>("scale", 0.0, 20.0)
    ;

    t.addHandle("/handle4")
        .addParamGen("ll", ll)
    ;

    static const std::vector<std::string> excludeHandles{"/handle3"};

    t.checkAllHandlesPresent(excludeHandles);

    t.chooseAndTestHandle();

    testYacareHandle(Handle(data, size)
        .setPath("/handle3")
        .addParamInt("x", 0, 32000)
        .addParamInt("y", 0, 32000)
        .addParamInt("z", 0, 32)
        .addParamInt("zmin", 0, 32)
        .addParamInt("zmax", 0, 32)
    );

    return 0;
}

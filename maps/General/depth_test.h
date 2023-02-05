#pragma once

namespace yandex::maps::runtime::vulkan {

enum class CompareOp {
    Never,
    Less,
    Equal,
    LessOrEqual,
    Greater,
    NotEqual,
    GreaterOrEqual,
    Always
};

struct DepthTest {
    bool isWriteEnabled;
    CompareOp compareOp;
};

} // namespace yandex::maps::runtime::vulkan

#pragma once

#include <string>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

struct TestDataBase {
    void setName(const std::string& name) { name_ = name; }
    const std::string& name() const { return name_; }
protected:
    std::string name_;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

#pragma once

#include <maps/libs/fuzzing/include/fuzzed.h>
#include <maps/libs/stringutils/include/join.h>

#include <deque>
#include <initializer_list>
#include <memory>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

namespace maps::fuzzing {

class Handle
{
public:
    Handle(const uint8_t* data, size_t size);
    ~Handle();

    Handle& setPath(const std::string& path)
    {
        path_ = path;
        return *this;
    }

    Handle& addParamInt(const std::string& name)
    {
        return addParam(name, fuzzed<int32_t>(*fdp_));
    }

    Handle& addParamInt(const std::string& name, int32_t min, int32_t max)
    {
        return addParam(name, fuzzed<int32_t>(*fdp_, min, max));
    }

    Handle& addParamDouble(const std::string& name)
    {
        return addParam(name, fuzzed<double>(*fdp_));
    }

    Handle& addParamDouble(const std::string& name, double min, double max)
    {
        return addParam(name, fuzzed<double>(*fdp_, min, max));
    }

    Handle& addParamString(const std::string& name, size_t maxLenght = DEFAULT_STRING_LENGTH)
    {
        return addParam(name, fuzzed<std::string>(*fdp_, maxLenght));
    }

    Handle& addParam(const std::string& name)
    {
        return addParamString(name);
    }

    template <typename T = std::string>
    Handle& addParam(const std::string& name)
    {
        return addParam(name, fuzzed<T>(*fdp_));
    }

    template <typename T>
    Handle& addParam(const std::string& name, const T& min, const T& max)
    {
        return addParam(name, fuzzed<T>(*fdp_, min, max));
    }

    template <typename T>
    Handle& addParamList(
        const std::string& name,
        size_t length = 2, const std::string& sep = ",")
    {
        std::vector<std::string> values(length);
        for (auto& v : values) {
            v = std::to_string(fuzzed<T>(*fdp_));
        }
        return addParam(name, stringutils::join(values, sep));
    }

    template <>
    Handle& addParamList<std::string>(
        const std::string& name,
        size_t length, const std::string& sep)
    {
        std::vector<std::string> values(length);
        for (auto& v : values) {
            v = fuzzed<std::string>(*fdp_);
        }
        return addParam(name, stringutils::join(values, sep));
    }

    template <typename T>
    Handle& addParamList(
        const std::string& name, const T& min, const T& max,
        size_t length = 2, const std::string& sep = ",")
    {
        std::vector<std::string> values(length);
        for (auto& v : values) {
            v = std::to_string(fuzzed<T>(*fdp_, min, max));
        }
        return addParam(name, stringutils::join(values, sep));
    }

    std::string path() const
    {
        return path_;
    }

    std::string pathWithArgs() const
    {
        std::stringstream ss;
        ss << path_;
        if (!args_.empty() || trash_) {
            ss << "?" <<  stringutils::join(args_, "&");
        }
        if (trash_)
            ss << trash_.value();
        return ss.str();
    }

    template <typename T>
    Handle& addParam(const std::string& name, const std::vector<T>& values)
    {
        size_t num = fuzzed<size_t>(*fdp_, 0, values.size());
        if (num == values.size()) { // param may be random string
            addParam<std::string>(name, fuzzed<std::string>(*fdp_));
        } else {
            addParam<T>(name, values[num]);
        }
        return *this;
    }

    template <typename T>
    Handle& addParam(const std::string& name, std::initializer_list<T> values)
    {
        return addParam(name, std::vector<T>(values));
    }

    template <typename T>
    Handle& addParam(const std::string& name, const T& value)
    {
        // param may be missing
        if (fuzzed<bool>(*fdp_)) {
            return *this;
        }
        // param may be random string
        std::stringstream ss;
        if (fuzzed<bool>(*fdp_)) {
            ss << name << "=" << fuzzed<std::string>(*fdp_);
        } else {
            ss << name << "=" << value;
        }
        args_.push_back(ss.str());
        return *this;
    }

    Handle& addParamGen(const std::string& name, const std::function<std::string(FuzzedDataProvider&)>& gen)
    {
        return addParam(name, gen(*fdp_));
    }

private:
    std::unique_ptr<FuzzedDataProvider> fdp_;
    std::string path_;
    std::deque<std::string> args_;
    std::optional<std::string> trash_;
};


class HandleTester
{
public:
    HandleTester(const uint8_t* data, size_t size);
    ~HandleTester();

    Handle& addHandle();
    Handle& addHandle(const std::string& name);

    // check that all declared yacare handles should be tested
    void checkAllHandlesPresent(const std::vector<std::string>& exclude = {});

    void chooseAndTestHandle();

private:
    size_t randomNum_; /// for choosing handle
    std::vector<uint8_t> bufferForFdp_;
    std::deque<Handle> handles_;
};


void testYacareHandle(const Handle& handle);
void testYacareHandle(const std::string& path);

} // maps::fuzzing

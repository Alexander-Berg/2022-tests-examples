#pragma once

#include <ostream>
#include <optional>

namespace std {

template<class CharType, class CharTrait>
std::basic_ostream<CharType, CharTrait>&
operator<<(std::basic_ostream<CharType, CharTrait>& out, std::nullopt_t)
{
    if (out.good()) {
        out << "--";
    }
    return out;
}

template<class CharType, class CharTrait, class T>
std::basic_ostream<CharType, CharTrait>&
operator<<(std::basic_ostream<CharType, CharTrait>& out, const std::optional<T>& v)
{
    if (out.good()) {
        if (!v) {
            out << "--";
        } else {
            out << ' ' << *v;
        }
    }
    return out;
}

} // namespace std

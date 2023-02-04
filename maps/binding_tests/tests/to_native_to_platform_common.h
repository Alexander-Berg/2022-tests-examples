#pragma once

#include <yandex/maps/runtime/color.h>
#include <yandex/maps/runtime/internal/test_support/test_types.h>
#include <yandex/maps/runtime/time.h>

#include <boost/optional.hpp>

#include <ostream>
#include <typeinfo>
#include <vector>

namespace std {

template <typename T>
ostream& operator<<(
    ostream& out,
    const vector<T>& v)
{
    out << "Vector(" << v.size() << ") [ ";
    for (const auto& item : v) {
        out << item << ' ';
    }
    return out << "]";
}

template <typename T>
ostream& operator<<(
    ostream& out,
    const boost::optional<T>& o)
{
    if (o) {
        return out << "Just " << typeid(T).name() << " { " << *o << " }";
    } else {
        return out << "Nil " << typeid(T).name();
    }
}

inline ostream& operator<<(ostream& out, const yandex::maps::runtime::TimeInterval& i)
{
    return out << "TimeInterval(" << i.count() << ")";
}
inline ostream& operator<<(ostream& out, const yandex::maps::runtime::AbsoluteTimestamp& t)
{
    return out << "AbsoluteTimestamp(" << t.time_since_epoch().count() << ")";
}
inline ostream& operator<<(ostream& out, const yandex::maps::runtime::RelativeTimestamp& t)
{
    return out << "RelativeTimestamp(" << t.time_since_epoch().count() << ")";
}

inline ostream& operator<<(ostream& out, const yandex::maps::runtime::Color& c)
{
    return out << "Color(a" << c.alpha() << " r" << c.red() <<
        " g" << c.green() << " b" << c.blue() << ")";
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::TestEnum& e)
{
    return out << "TestEnum (" << int(e) << ")";
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::TestBitfieldEnum& e)
{
    return out << "TestBitfieldEnum (" << int(e) << ")";
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::LiteTestStructure& s)
{
    return out << "LiteTestStructure {" <<
        s.b << ' ' <<
        s.text << ' ' <<
        s.optionalText << ' ' <<
        s.interval << ' ' <<
        s.timestamp << '}';
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::OptionsTestStructure& s)
{
    return out << "OptionsTestStructure {" <<
        s.b() << ' ' <<
        s.text() << ' ' <<
        s.interval() << ' ' <<
        s.timestamp() << '}';
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::TestStructure& s)
{
    return out << "TestStructure {" <<
        s.b << ' ' <<
        s.text << ' ' <<
        s.optionalText << ' ' <<
        *s.intVector << ' ' <<
        s.interval << ' ' <<
        s.timestamp << '}';
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::FullTestStructure& s)
{
#define FIELD(f) \
    s.f << ' ' << s.o##f << ' ' << s.v##f << ' ' << s.vo##f << ' ' << s.d##f << ' ' << s.do##f << ' '

    return out << "FullTestStructure {" <<
        FIELD(b) <<
        FIELD(i) << FIELD(ui) << FIELD(i64) << FIELD(fl) << FIELD(d) <<
        FIELD(s) <<
        FIELD(ti) << FIELD(at) << FIELD(rt) <<
        s.by << ' ' << s.oby << ' ' <<
        FIELD(c) << FIELD(p) <<
        FIELD(e) << FIELD(be) << FIELD(ts) << FIELD(lts) << FIELD(opts) << '}';

#undef FIELD
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::FullLiteTestStructure& s)
{
#define FIELD(f) s.f << ' ' << s.o##f << ' '
    return out << "FullLiteTestStructure {" <<
        FIELD(b) <<
        FIELD(i) << FIELD(ui) << FIELD(i64) << FIELD(fl) << FIELD(d) <<
        FIELD(s) <<
        FIELD(ti) << FIELD(at) << FIELD(rt) <<
        FIELD(by) <<
        FIELD(c) << FIELD(p) <<
        FIELD(e) << FIELD(be) << '}';
#undef FIELD
}

inline ostream& operator<<(
    ostream& out,
    const yandex::maps::runtime::internal::test_support::FullOptionsTestStructure& s)
{
#define FIELD(f) s.f() << ' '
    return out << "FullLiteTestStructure {" <<
        FIELD(b) <<
        FIELD(i) << FIELD(ui) << FIELD(i64) << FIELD(fl) << FIELD(d) <<
        FIELD(s) <<
        FIELD(ti) << FIELD(at) << FIELD(rt) <<
        FIELD(by) <<
        FIELD(c) << FIELD(p) <<
        FIELD(e) << FIELD(be) << '}';
#undef FIELD
}

} // namespace std

namespace yandex::maps::runtime::internal::test_support {

inline bool operator==(const LiteTestStructure& a, const LiteTestStructure& b)
{
    return a.b == b.b &&
        a.text == b.text && a.optionalText == b.optionalText &&
        a.interval == b.interval && a.timestamp == b.timestamp;
}

inline bool operator==(const OptionsTestStructure& a, const OptionsTestStructure& b)
{
    return a.b() == b.b() &&
        a.text() == b.text() &&
        a.interval() == b.interval() && a.timestamp() == b.timestamp();
}

inline bool operator==(const TestStructure& a, const TestStructure& b)
{
    return a.b == b.b &&
        a.text == b.text && a.optionalText == b.optionalText &&
        *a.intVector == *b.intVector &&
        a.interval == b.interval && a.timestamp == b.timestamp;
}

inline bool operator==(const FullTestStructure& a, const FullTestStructure& b)
{
#define FIELD(f) \
    a.f == b.f && a.o##f == b.o##f && a.v##f == b.v##f && a.vo##f == b.vo##f && a.d##f == b.d##f && a.do##f == b.do##f

    return FIELD(b) &&
        FIELD(i) && FIELD(ui) && FIELD(i64) && FIELD(fl) && FIELD(d) &&
        FIELD(s) &&
        FIELD(ti) && FIELD(at) && FIELD(rt) &&
        a.by == b.by && a.oby == b.oby &&
        FIELD(c) && FIELD(p) &&
        FIELD(e) && FIELD(be) && FIELD(ts) && FIELD(lts) && FIELD(opts);

#undef FIELD
}

inline bool operator==(const FullLiteTestStructure& a, const FullLiteTestStructure& b)
{
#define FIELD(f) a.f == b.f && a.o##f == b.o##f
    return FIELD(b) &&
        FIELD(i) && FIELD(ui) && FIELD(i64) && FIELD(fl) && FIELD(d) &&
        FIELD(s) &&
        FIELD(ti) && FIELD(at) && FIELD(rt) &&
        FIELD(by) &&
        FIELD(c) && FIELD(p) &&
        FIELD(e) && FIELD(be);
#undef FIELD
}

inline bool operator==(const FullOptionsTestStructure& a, const FullOptionsTestStructure& b)
{
#define FIELD(f) a.f() == b.f()
    return FIELD(b) &&
        FIELD(i) && FIELD(ui) && FIELD(i64) && FIELD(fl) && FIELD(d) &&
        FIELD(s) &&
        FIELD(ti) && FIELD(at) && FIELD(rt) &&
        FIELD(by) &&
        FIELD(c) && FIELD(p) &&
        FIELD(e) && FIELD(be);
#undef FIELD
}

} // namespace yandex::maps::runtime::internal::test_support
void runToNativeToPlatformTests();

#pragma once

#include <maps/analyzer/libs/ml/features.h>

namespace ml = maps::analyzer::ml;

struct TargetNormalization {
    enum class Type: std::size_t { SEC, LOG_SPKM };

    struct Params;

    static constexpr auto names = ml::reprs<Type>(
        ml::repr<Type, Type::SEC>("SEC"),
        ml::repr<Type, Type::LOG_SPKM>("LOG_SPKM")
    );
};

struct TargetNormalization::Params {
    double length;
};

struct Common {
    static constexpr std::string_view name = "common";
    enum class Cat: std::size_t { THREAD, ROUTE, VEHICLE };
    enum class Num: std::size_t { LENGTH };

    struct Data;

    static constexpr auto cats = ml::reprs<Cat>(
        ml::repr<Cat, Cat::THREAD>("THREAD"),
        ml::repr<Cat, Cat::ROUTE>("ROUTE"),
        ml::repr<Cat, Cat::VEHICLE>("VEHICLE")
    );
    static constexpr auto nums = ml::reprs<Num>(
        ml::repr<Num, Num::LENGTH>("LENGTH")
    );
};

struct Common::Data {
    double length;
};

struct Signal {
    static constexpr std::string_view name = "signal";
    enum class Cat: std::size_t { REGION };
    enum class Num: std::size_t { LOCAL_DAY };

    struct Data;

    static constexpr auto cats = ml::reprs<Cat>(
        ml::repr<Cat, Cat::REGION>("REGION")
    );
    static constexpr auto nums = ml::reprs<Num>(
        ml::repr<Num, Num::LOCAL_DAY>("LOCAL_DAY")
    );
};

struct Signal::Data {
    double day;
};

template <>
double ml::normalize<TargetNormalization, TargetNormalization::Type::SEC>(
    double prediction,
    const typename TargetNormalization::Params&
) {
    return prediction;
}

template <>
double ml::denormalize<TargetNormalization, TargetNormalization::Type::SEC>(
    double time,
    const typename TargetNormalization::Params&
) {
    return time;
}

template <>
double ml::normalize<TargetNormalization, TargetNormalization::Type::LOG_SPKM>(
    double prediction,
    const typename TargetNormalization::Params& p
) {
    return std::exp(prediction) * p.length / 1000.;
}

template <>
double ml::denormalize<TargetNormalization, TargetNormalization::Type::LOG_SPKM>(
    double time,
    const typename TargetNormalization::Params& p
) {
    return std::log(time / p.length * 1000.);
}

template <>
std::string ml::feature<Common, Common::Cat::THREAD>(const Common::Data&) {
    return "thread";
}

template <>
std::string ml::feature<Common, Common::Cat::ROUTE>(const Common::Data&) {
    return "route";
}

template <>
std::string ml::feature<Common, Common::Cat::VEHICLE>(const Common::Data&) {
    return "vehicle";
}

template <>
double ml::feature<Common, Common::Num::LENGTH>(const Common::Data& d) {
    return d.length;
}

template <>
std::string ml::feature<Signal, Signal::Cat::REGION>(const Signal::Data&) {
    return "region";
}

template <>
double ml::feature<Signal, Signal::Num::LOCAL_DAY>(const Signal::Data& d) {
    return d.day;
}

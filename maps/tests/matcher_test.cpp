#include <maps/analyzer/libs/mapmatching/include/matcher.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <random>
#include <iostream>
#include <climits>

namespace mapmatching = maps::analyzer::mapmatching;

std::default_random_engine randomEngine;
std::uniform_real_distribution<double> randomLikelihood(-5.0, 0.0);
std::uniform_int_distribution<std::size_t> randomCandidatesCount(1, 5);
constexpr double SKIP_LOG_LIKELIHOOD = -3.0;

struct CandidateState {
    std::size_t id;
    std::size_t time;
};

inline bool operator == (const CandidateState& lhs, const CandidateState& rhs) {
    return lhs.id == rhs.id;
}

inline bool operator != (const CandidateState& lhs, const CandidateState& rhs) {
    return !(lhs == rhs);
}

std::ostream& operator << (std::ostream& out, const CandidateState& st) {
    return out << st.id << "[" << st.time << "]";
}

struct Offset {
    Offset& operator += (const Offset& other) {
        offset += other.offset;
        return *this;
    }
    std::size_t offset = 0;
};

struct Signal {
    std::size_t time;
};

struct NullType {};

using Matcher = mapmatching::Matcher<CandidateState, Signal, Offset>;

Y_UNIT_TEST_SUITE(TestMatcher) {
    Y_UNIT_TEST(TestMatches) {
        using mapmatching::Weighted;

        std::cerr << "=== random candidates ===" << std::endl;

        constexpr std::uint64_t LAYERS_COUNT = 40;
        constexpr std::uint64_t FORCE_CLEAR_LAYER = 20;

        std::vector<std::vector<CandidateState>> matched;
        const auto onMatch = [&](const Signal&, const Signal&, const CandidateState& prev, const CandidateState& cur, std::uint64_t from, std::uint64_t to) {
            const auto skipped = to - from - 1;
            std::cerr << "matched: " << prev << "→" << cur << std::endl;
            if (skipped) {
                std::cerr << "skipped signals: " << skipped << std::endl;
            }
            if (!matched.empty() && matched.back().back() == prev) {
                matched.back().push_back(cur);
            } else {
                matched.push_back({prev, cur});
            }
        };
        const auto dumpMatched = [&]() {
            std::cerr << "==[ matched ]==" << std::endl;
            for (const auto& p: matched) {
                std::cerr << p.front();
                for (auto it = p.begin() + 1; it != p.end(); ++it) {
                    std::cerr << "→" << *it;
                }
                std::cerr << std::endl;
            }
        };

        std::size_t currentId = 0;
        Matcher matcher{
            2,
            {}, // no candidates limits
            // generate candidates
            [&](const Signal& s, const Offset&) {
                const std::size_t candidatesCount = randomCandidatesCount(randomEngine);
                std::vector<CandidateState> candidates;
                for (std::size_t i = 0; i < candidatesCount; ++i) {
                    candidates.push_back({currentId++, s.time});
                }
                return candidates;
            },
            // weigh candidate
            [&](const Signal&, const Offset&, const CandidateState&) {
                return randomLikelihood(randomEngine);
            },
            // weigh transition
            [&](const Signal&, const Signal&, const Offset& o, const CandidateState&, const CandidateState& to) {
                const auto skippedLikelihood = SKIP_LOG_LIKELIHOOD * static_cast<double>(o.offset - 1);
                const auto logLikelihood = randomLikelihood(randomEngine) + skippedLikelihood;
                return Weighted{logLikelihood, to};
            },
            // weigh head
            [&](const Signal&, const Offset& o, const CandidateState&) {
                return SKIP_LOG_LIKELIHOOD * static_cast<double>(o.offset);
            },
            onMatch
        };

        const auto dumpState = [&]() {
            std::cerr << "signals deque size: " << matcher.signals.size() << std::endl;
            auto best = matcher.bestHead();
            auto top = matcher.topHeads();
            EXPECT_TRUE(!best || best == top[0].item);

            for (const auto& l: matcher.cgraph.layers) {
                for (const auto& c: l.candidates) {
                    if (best == c.head) {
                        std::cerr << "*";
                    }
                    std::cerr << c.head.value().state << "(" << c.logLikelihood << ")";
                    auto n = c.head.node->parent();
                    while (n) {
                        std::cerr << "→" << n->value.state;
                        n = n->parent();
                    }
                    std::cerr << std::endl;
                }
            }
        };

        for (std::uint64_t i = 0; i < LAYERS_COUNT; ++i) {
            std::cerr << "--- --- ---" << std::endl;
            std::cerr << "adding layer " << i << " ..." << std::endl;
            if (matcher.add({i}, {1})) {
                matcher.matchDefinite();
            }

            if (i == FORCE_CLEAR_LAYER) {
                matcher.matchUntil(i);
                matcher.removeUntil(i);
            }

            // force match old candidates
            matcher.matchIf([&](const CandidateState& c, const Signal&) { return i - c.time > 4; });

            dumpState();
        }
        matcher.match();
        dumpMatched();

        const auto& pool = matcher.root->nodePool();
        std::cerr << "Node pool: creations=" << pool.creations() << ", allocations=" << pool.allocations() << std::endl;
    }
}

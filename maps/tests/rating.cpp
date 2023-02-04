#include <maps/infopoint/lib/misc/rating.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;

TEST(rating, min_max)
{
    EXPECT_NEAR(mergedRating(RATING_MIN, RATING_MIN), RATING_MIN, 0.01);
    EXPECT_NEAR(mergedRating(RATING_MAX, RATING_MAX), RATING_MAX, 0.01);
    EXPECT_NEAR(mergedRating(RATING_MIN, RATING_MAX), 0.0, 0.01);
}

TEST(rating, merge_permutations)
{
    auto ratings = std::vector<double>();
    ratings.push_back(0.1);
    ratings.push_back(-0.2);
    ratings.push_back(0.4);
    ratings.push_back(0.8);
    ratings.push_back(-0.6);
    ratings.push_back(-0.9);
    ratings.push_back(0.3);
    ratings.push_back(0.0);

    auto correct = 0.0;
    for (size_t i = 0; i < ratings.size(); ++i) {
        correct = mergedRating(correct, ratings[i]);
    }

    while (std::next_permutation(ratings.begin(), ratings.end())) {
        auto rating = 0.0;
        for (size_t i = 0; i < ratings.size(); ++i) {
            rating = mergedRating(rating, ratings[i]);
        }
        EXPECT_NEAR(correct, rating, 0.01);
    }
}

TEST(rating, merge_reversibility)
{
    auto ratings = std::vector<double>();
    ratings.push_back(0.1);
    ratings.push_back(-0.2);
    ratings.push_back(0.4);
    ratings.push_back(0.8);
    ratings.push_back(-0.6);
    ratings.push_back(-0.9);
    ratings.push_back(0.3);
    ratings.push_back(0.0);

    for (size_t i = 0; i < ratings.size(); ++i) {
        for (size_t j = 0; j < ratings.size(); ++j) {
            EXPECT_NEAR(
                ratings[i],
                mergedRating(mergedRating(ratings[i], ratings[j]), -ratings[j]),
                0.01);
        }
    }
}

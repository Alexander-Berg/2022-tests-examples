#include <maps/factory/libs/common/covariance.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(online_covariance_should) {

using RowArray4d = Eigen::Array<double, 1, 4>;
using Eigen::Matrix4d;
using Eigen::MatrixX4d;
using Eigen::MatrixXd;
using Eigen::VectorXd;

constexpr double EPS = 1e-12;

/// Return {mean, Cov} of the given column matrix.
template <class T>
static auto meanAndCov(const T& mat)
{
    auto mean = mat.colwise().mean().eval();
    auto centered = mat.rowwise() - mean;
    auto Cov = ((centered.transpose() * centered) / mat.rows()).eval();
    return std::make_tuple(mean, Cov);
}

Y_UNIT_TEST(calculate_mean_and_std_of_vector)
{
    VectorXd data = VectorXd::Random(200);
    OnlineCovariance<1> oc;
    oc.addRows(data);
    auto[expectedMean, std] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_NEAR(oc.mean()(0), expectedMean(0), EPS);
    EXPECT_NEAR(oc.cov()(0, 0), std(0), EPS);
}

Y_UNIT_TEST(calculate_means_and_covariances)
{
    MatrixX4d data = MatrixX4d::Random(200, 4);
    OnlineCovariance<4> oc;
    oc.addRows(data);
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(add_batches)
{
    MatrixXd data = MatrixXd::Random(300, 3);
    OnlineCovariance<3> oc;
    oc.addRows(data.middleRows<100>(0));
    oc.addRows(data.middleRows<100>(100));
    oc.addRows(data.middleRows<100>(200));
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(add_row_by_row)
{
    MatrixXd data = MatrixXd::Random(100, 5);
    OnlineCovariance<5> oc;
    for (long r = 0; r < data.rows(); ++r) {
        oc.addRow(data.row(r));
    }
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(merge_covariances)
{
    MatrixXd data = MatrixXd::Random(400, 4);
    OnlineCovariance<4> oc;
    oc.addRows(data.middleRows<100>(0));
    oc = OnlineCovariance<4>().addRows(data.middleRows<100>(100)) + oc;
    oc = oc + OnlineCovariance<4>().addRows(data.middleRows<100>(200));
    oc += OnlineCovariance<4>().addRows(data.middleRows<100>(300));
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(scale)
{
    MatrixXd data = MatrixXd::Random(100, 4);
    RowArray4d scale{1.5, 3.1415, 100.0, 0.1};
    OnlineCovariance<4> oc;
    oc.addRows(data);
    oc *= scale;
    data.array().rowwise() *= scale;
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(shift)
{
    MatrixX4d data = MatrixX4d::Random(100, 4);
    OnlineCovariance<4> oc;
    oc.addRows(data);
    RowArray4d shift{1.1, 3.3, -10.5, 0};
    oc += shift;
    data.array().rowwise() += shift;
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(oc.weight(), data.rows());
    EXPECT_THAT(oc.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(oc.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(random_scale_and_shift)
{
    MatrixX4d data = MatrixX4d::Random(100, 4);
    OnlineCovariance<4> oc;
    oc.addRows(data);
    RowArray4d shift = RowArray4d::Random();
    RowArray4d scale = RowArray4d::Random() + 2; // Scale should be positive.
    auto result = oc * scale + shift;
    data = (data.array().rowwise() * scale).rowwise() + shift;
    auto[expectedMean, expectedCov] = meanAndCov(data);
    EXPECT_EQ(result.weight(), data.rows());
    EXPECT_THAT(result.mean(), EigEq(expectedMean, EPS));
    EXPECT_THAT(result.cov(), EigEq(expectedCov, EPS));
}

Y_UNIT_TEST(skip_zero_rows_using_weighted_batch_add)
{
    MatrixX4d data = MatrixX4d::Random(100, 4);
    VectorXd weights = (Eigen::ArrayXd::Random(100, 1) / 2 + 0.5).round(); // Zero or one.
    OnlineCovariance<4> oc;
    oc.addRows(data, weights);

    OnlineCovariance<4> expected;
    double weight = 0;
    for (long r = 0; r < data.rows(); ++r) {
        if (weights(r) > 0.99) {
            expected.addRow(data.row(r));
            weight += 1;
        }
    }

    EXPECT_EQ(oc.weight(), weight);
    EXPECT_THAT(oc.mean(), EigEq(expected.mean(), EPS));
    EXPECT_THAT(oc.cov(), EigEq(expected.cov(), EPS));
}

Y_UNIT_TEST(save_to_json)
{
    OnlineCovariance<2> oc;
    oc.addRow(Eigen::RowVector2d{1, 2});
    oc.addRow(Eigen::RowVector2d{3, 3});
    EXPECT_EQ(oc.json(),
        R"({"weight":2,"mean":[2,2.5],"covariance":[2,1,1,0.5]})");
}

Y_UNIT_TEST(load_from_json)
{
    OnlineCovariance<2> oc;
    oc.addRow(Eigen::RowVector2d{1, 2});
    oc.addRow(Eigen::RowVector2d{3, 3});
    auto json = R"({"weight":2,"mean":[2,2.5],"covariance":[2,1,1,0.5]})";
    OnlineCovariance<2> loaded{json::Value::fromString(json)};
    EXPECT_EQ(loaded.weight(), oc.weight());
    EXPECT_THAT(loaded.mean(), EigEq(oc.mean()));
    EXPECT_THAT(loaded.cov(), EigEq(oc.cov()));
}

Y_UNIT_TEST(resize)
{
    OnlineCovariance<1> oc1;
    oc1.addRow(Eigen::Matrix<double, 1, 1>{1});
    oc1.addRow(Eigen::Matrix<double, 1, 1>{3});

    OnlineCovariance<2> oc2;
    oc2.addRow(Eigen::RowVector2d{1, 2});
    oc2.addRow(Eigen::RowVector2d{3, 3});

    OnlineCovariance<3> oc3;
    oc3.addRow(Eigen::RowVector3d{1, 2, 0});
    oc3.addRow(Eigen::RowVector3d{3, 3, 0});

    EXPECT_EQ(oc2.resized<1>().json(),
        "{\"weight\":2,\"mean\":[2],\"covariance\":[2]}");
    EXPECT_EQ(oc2.resized<2>().json(),
        "{\"weight\":2,\"mean\":[2,2.5],\"covariance\":[2,1,1,0.5]}");
    EXPECT_EQ(oc2.resized<3>().json(),
        "{\"weight\":2,\"mean\":[2,2.5,0],\"covariance\":[2,1,0,1,0.5,0,0,0,0]}");

    EXPECT_EQ(oc1.resized<1>(), oc1);
    EXPECT_EQ(oc2.resized<1>(), oc1);
    EXPECT_EQ(oc2.resized<2>(), oc2);
    EXPECT_EQ(oc2.resized<3>(), oc3);
    EXPECT_EQ(oc3.resized<1>(), oc1);
    EXPECT_EQ(oc3.resized<2>(), oc2);
    EXPECT_EQ(oc3.resized<3>(), oc3);
}

Y_UNIT_TEST(get_std)
{
    OnlineCovariance<2> oc;
    oc.addRow(Eigen::RowVector2d{1, 2});
    oc.addRow(Eigen::RowVector2d{3, 3});
    oc.addRow(Eigen::RowVector2d{10, 4});

    EXPECT_THAT(oc.std(), EigEq(Eigen::RowVector2d{3.8586123009301, 0.81649658092773}, 1e-10));
}

} // Y_UNIT_TEST_SUITE

} //namespace maps::factory::tests

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/concave_hull/impl/is_inner.h>
#include <maps/libs/concave_hull/impl/triangulation.h>

namespace maps::concave_hull {

Y_UNIT_TEST_SUITE(TestIsInner)
{

Y_UNIT_TEST(TestExactCircumcircleRadius)
{
    // The circumcircle radius is strictly equal to 10 here
    // But circumcircleRadius returns 10.00000000000003 or 9.99999999999994
    // depending on the edges order because or doubles rounding error.
    Triangulation triang({
        {4181885, 7472669},
        {4181883, 7472667},
        {4181881, 7472661},
    });

    double hullRadius = 10;
    for (int edge : triang.getAllEdges()) {
        int edge2l = triang.getNextLeftEdge(edge);
        int edge3l = triang.getNextLeftEdge(edge2l);
        int edge2r = triang.getNextRightEdge(edge);
        int edge3r = triang.getNextRightEdge(edge2r);

        // isInner should produce the same choice
        // independently of the edges order
        bool isInnerL = isInner(triang, edge, edge2l, edge3l, hullRadius);
        EXPECT_EQ(isInnerL, isInner(triang, edge2l, edge3l, edge, hullRadius));
        EXPECT_EQ(isInnerL, isInner(triang, edge3l, edge, edge2l, hullRadius));

        bool isInnerR = isInner(triang, edge, edge2r, edge3r, hullRadius);
        EXPECT_EQ(isInnerR, isInner(triang, edge2r, edge3r, edge, hullRadius));
        EXPECT_EQ(isInnerR, isInner(triang, edge3r, edge, edge2r, hullRadius));
    }
}

}

} // namespace maps::concave_hull

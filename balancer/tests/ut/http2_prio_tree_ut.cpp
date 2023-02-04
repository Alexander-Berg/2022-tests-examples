#include <balancer/kernel/http2/server/utils/http2_prio_tree.h>
#include <balancer/kernel/http2/server/utils/http2_log.h>

#include <balancer/kernel/http2/server/common/http2_common.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(HTTP2PrioTreeTest) {

    using namespace NSrvKernel::NHTTP2;

    static double AsRelWeight(ui32 rawWeight) {
        return (rawWeight + 1) / double(RFC_PRIO_WEIGHT_MAX);
    }

    struct TNodeDescr {
        const TPrioTreeNode* Parent = nullptr;
        TVector<const TPrioTreeNode*> Children;
        ui32 Depth = 0;
        double RelWeight = 1.;

    public:
        TNodeDescr() = default;

        TNodeDescr(
            TPrioTreeNode* parent,
            std::initializer_list<const TPrioTreeNode*> children,
            ui32 depth,
            std::initializer_list<ui32> weights
        ) {
            WithParent(parent).WithChildren(children).WithDepth(depth).WithWeights(weights);
        }

        void PrintTo(IOutputStream& out) const {
            using namespace NSrvKernel::NHTTP2;
            auto parentPtr = Hex(reinterpret_cast<ptrdiff_t>((const void*)Parent));
            Y_HTTP2_PRINT_OBJ(out, parentPtr, Children.size(), Depth, RelWeight);
        }

        TNodeDescr& WithParent(TPrioTreeNode* parent) {
            Parent = parent;
            return *this;
        }

        TNodeDescr& WithChildren(std::initializer_list<const TPrioTreeNode*> children) {
            Children = children;
            return *this;
        }

        TNodeDescr& WithDepth(ui32 depth) {
            Depth = depth;
            return *this;
        }

        TNodeDescr& WithWeights(std::initializer_list<ui32> rawWeights) {
            RelWeight = 1.;

            for (auto w : rawWeights) {
                RelWeight *= AsRelWeight(w);
            }

            return *this;
        }

        static bool Equal(const TIntrusiveList<TPrioTreeNode>& children, const TVector<const TPrioTreeNode*>& expChildren) {
            auto iter = children.begin();
            for (const auto* child : expChildren) {
                if (&*iter++ != child) {
                    return false;
                }
            }
            return true;
        }


        friend bool operator==(const TPrioTreeNode& node, const TNodeDescr& descr) {
            return node.GetParent() == descr.Parent
                   && node.GetChildren().Size() == descr.Children.size()
                   && TNodeDescr::Equal(node.GetChildren(), descr.Children)
                   && node.GetDepth() == descr.Depth
                   && node.GetRelWeight() == descr.RelWeight;
        }
    };

    Y_UNIT_TEST(TestSingleRoot) {
        using namespace NSrvKernel::NHTTP2;
        TPrioTreeNode root;
        TPrioTreeNode other;
        UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr());
        UNIT_ASSERT(!root.UpdatePrio(root, 1, false));
        UNIT_ASSERT(!root.UpdatePrio(other, 1, false));
        UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr());
    }

    Y_UNIT_TEST(TestOneLevel) {
        using namespace NSrvKernel::NHTTP2;
        TPrioTreeNode root;

        {
            TPrioTreeNode node1(root, 1);
            UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&root, {}, 1, {15u}));
            TPrioTreeNode node3(root, 3);
            UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&root, {}, 1, {15u}));
            TPrioTreeNode node5(root, 5);
            UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&root, {}, 1, {15u}));

            {
                TPrioTreeNode node7(root, 7);
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&root, {}, 1, {15u}));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node1, &node3, &node5, &node7}, 0, {}));

                // root ! A,B,C => root
                // ||||            ||||
                // ABCD            ABCD

                UNIT_ASSERT(node1.UpdatePrio(root, 31, false));

                // second update is no-op

                UNIT_ASSERT(node1.UpdatePrio(root, 31, false));

                UNIT_ASSERT(node3.UpdatePrio(root, 63, false));

                UNIT_ASSERT(node5.UpdatePrio(root, 127, false));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node7, &node1, &node3, &node5}, 0, {}));
                UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&root, {}, 1, {31u}));
                UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&root, {}, 1, {63u}));
                UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&root, {}, 1, {127u}));
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&root, {}, 1, {15u}));

                // root + D/E => root
                // ||||           |
                // ABCD           D
                //               /|
                //              ABC

                UNIT_ASSERT(node7.UpdatePrio(root, 7, true));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node7}, 0, {}));
                UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&node7, {}, 2, {7u, 31u}));
                UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&node7, {}, 2, {7u, 63u}));
                UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&node7, {}, 2, {7u, 127u}));
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&root, {&node1, &node3, &node5}, 1, {7u}));

                //  root => root
                //   |       |
                //   D       D
                //  /|      /|
                // ABC     ABC

                UNIT_ASSERT(node7.UpdatePrio(root, 3, false));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node7}, 0, {}));
                UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&node7, {}, 2, {3u, 31u}));
                UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&node7, {}, 2, {3u, 63u}));
                UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&node7, {}, 2, {3u, 127u}));
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&root, {&node1, &node3, &node5}, 1, {3u}));

                //  root ^ B => root
                //   |           | |
                //   D           D B
                //  /|          /|
                // ABC         A C

                UNIT_ASSERT(node3.UpdatePrio(root, 63, false));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node7, &node3}, 0, {}));
                UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&node7, {}, 2, {3u, 31u}));
                UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&root, {}, 1, {63u}));
                UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&node7, {}, 2, {3u, 127u}));
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&root, {&node1, &node5}, 1, {3u}));

                //  root ^ A => root => root
                //   | |         |||     | |
                //   D B         DBA     A B
                //  /|           |       |
                // A C           C       D
                //                       |
                //                       C

                UNIT_ASSERT(node7.UpdatePrio(node1, 3u, true));

                UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node3, &node1}, 0, {}));
                UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&root, {&node7}, 1, {31u}));
                UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&root, {}, 1, {63u}));
                UNIT_ASSERT_VALUES_EQUAL(node7, TNodeDescr(&node1, {&node5}, 2, {31u, 3u}));
                UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&node7, {}, 3, {31u, 3u, 127u}));
            }

            //  root - D => root
            //   | |         | |
            //   A B         A B
            //   |           |
            //   D           C
            //   |
            //   C

            UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {&node3, &node1}, 0, {}));
            UNIT_ASSERT_VALUES_EQUAL(node1, TNodeDescr(&root, {&node5}, 1, {31u}));
            UNIT_ASSERT_VALUES_EQUAL(node3, TNodeDescr(&root, {}, 1, {63u}));
            UNIT_ASSERT_VALUES_EQUAL(node5, TNodeDescr(&node1, {}, 2, {31u, 127u}));
        }

        //  root - C,B,A => root
        //   | |
        //   A B
        //   |
        //   C

        UNIT_ASSERT_VALUES_EQUAL(root, TNodeDescr(nullptr, {}, 0, {}));
    }
};

template <>
void Out<NTestSuiteHTTP2PrioTreeTest::TNodeDescr>(IOutputStream& out, const NTestSuiteHTTP2PrioTreeTest::TNodeDescr& descr) {
    descr.PrintTo(out);
}

#include <maps/libs/common/include/pimpl_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <memory>
#include <type_traits>
#include <climits>

namespace maps::tests::copyable {

class CopyableFacade
{
public:
    CopyableFacade(int data);

    int data() const;

    COPYABLE_PIMPL_DECLARATIONS(CopyableFacade)
};


class CopyableFacade::Impl
{
public:
    int data;
};


CopyableFacade::CopyableFacade(int data)
    : impl_(new Impl{data})
{
}

int CopyableFacade::data() const
{
    return impl_->data;
}

COPYABLE_PIMPL_DEFINITIONS(CopyableFacade);

TEST(Pimpl_tests, test_copyable_pimpl)
{
    CopyableFacade origin(31337);

    //can copy
    CopyableFacade copy(origin);
    EXPECT_EQ(copy.data(), origin.data());

    //can move
    CopyableFacade move(std::move(origin));
    EXPECT_EQ(move.data(), copy.data());

    EXPECT_TRUE((std::is_constructible<CopyableFacade, int>::value));
    EXPECT_TRUE(std::is_copy_assignable<CopyableFacade>::value);
    EXPECT_TRUE(std::is_copy_constructible<CopyableFacade>::value);
    EXPECT_TRUE(std::is_move_assignable<CopyableFacade>::value);
    EXPECT_TRUE(std::is_move_constructible<CopyableFacade>::value);
}

} //namespace maps::tests::copyable

namespace maps::tests::movable {

class MovableFacade
{
public:
    MovableFacade(int data);

    int data() const;

    MOVABLE_PIMPL_DECLARATIONS(MovableFacade)
};


class MovableFacade::Impl
{
public:
    std::unique_ptr<int> data;
};

MovableFacade::MovableFacade(int data)
    : impl_(new Impl{
        std::unique_ptr<int>(new int(data))
    })
{
}

int MovableFacade::data() const
{
    return *(impl_->data);
}

MOVABLE_PIMPL_DEFINITIONS(MovableFacade)

TEST(Pimpl_tests, test_movable_pimpl)
{
    MovableFacade origin(31337);

    MovableFacade move(std::move(origin));
    EXPECT_EQ(move.data(), 31337);

    EXPECT_TRUE((std::is_constructible<MovableFacade, int>::value));
    EXPECT_FALSE(std::is_copy_assignable<MovableFacade>::value);
    EXPECT_FALSE(std::is_copy_constructible<MovableFacade>::value);
    EXPECT_TRUE(std::is_move_assignable<MovableFacade>::value);
    EXPECT_TRUE(std::is_move_constructible<MovableFacade>::value);
}

} //namespace maps::tests::movable

namespace maps::tests::noncopyable {

class NoncopyableFacade
{
public:
    NoncopyableFacade(int data);
    int data() const;
    NONCOPYABLE_PIMPL_DECLARATIONS(NoncopyableFacade)
};

class NoncopyableFacade::Impl
{
public:
    int data;
};

NoncopyableFacade::NoncopyableFacade(int data)
    : impl_(new Impl{data})
{
}

int NoncopyableFacade::data() const
{
    return impl_->data;
}

NONCOPYABLE_PIMPL_DEFINITIONS(NoncopyableFacade)

TEST(Pimpl_tests, test_noncopyable_pimpl)
{
    NoncopyableFacade origin(31337);
    EXPECT_EQ(origin.data(), 31337);

    EXPECT_TRUE((std::is_constructible<NoncopyableFacade, int>::value));
    EXPECT_FALSE(std::is_copy_assignable<NoncopyableFacade>::value);
    EXPECT_FALSE(std::is_copy_constructible<NoncopyableFacade>::value);
    EXPECT_FALSE(std::is_move_assignable<NoncopyableFacade>::value);
    EXPECT_FALSE(std::is_move_constructible<NoncopyableFacade>::value);
}

} //namespace maps::tests::noncopyable

namespace maps::tests::nonconstructible {

class NonconstructibleFacade
{
public:
    int data() const;
    MOVABLE_PIMPL_DECLARATIONS(NonconstructibleFacade)
};

class NonconstructibleFacade::Impl
{
public:
    int data;
};

int NonconstructibleFacade::data() const
{
    return impl_->data;
}

MOVABLE_PIMPL_DEFINITIONS(NonconstructibleFacade);

TEST(Pimpl_tests, test_nonconstructible_pimpl)
{
    //such use case doesn't require ctor to be defined in
    auto obj = PImplFactory::create<NonconstructibleFacade>(31337);
    EXPECT_EQ(obj.data(), 31337);
    //object impl is accessible (and modifyable) via PImplFactory::impl()
    PImplFactory::impl(obj).data = 73313;
    EXPECT_EQ(obj.data(), 73313);


    EXPECT_TRUE((!std::is_constructible<NonconstructibleFacade, int>::value));
    EXPECT_FALSE(std::is_copy_assignable<NonconstructibleFacade>::value);
    EXPECT_FALSE(std::is_copy_constructible<NonconstructibleFacade>::value);
    EXPECT_TRUE(std::is_move_assignable<NonconstructibleFacade>::value);
    EXPECT_TRUE(std::is_move_constructible<NonconstructibleFacade>::value);
}

} //namespace maps::tests::nonconstructible

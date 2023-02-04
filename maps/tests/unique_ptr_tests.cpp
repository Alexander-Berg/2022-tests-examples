#include <library/cpp/testing/gtest/gtest.h>

#include <maps/libs/common/include/unique_ptr.h>
#include <memory>
#include <typeinfo>

namespace maps::common::tests {

struct Base {
public:
    virtual ~Base() = default;
};

class A : public Base {};

class B : public Base {};

TEST(UniquePtr_should, do_dynamic_cast) {
    std::unique_ptr<A> aPtr = std::make_unique<A>();
    A* aRawPtr = aPtr.get();
    std::unique_ptr<Base> baseAPtr = dynamic_unique_cast<Base>(std::move(aPtr));
    EXPECT_TRUE(baseAPtr);

    std::unique_ptr<A> newAPtr = dynamic_unique_cast<A>(std::move(baseAPtr));
    EXPECT_EQ(newAPtr.get(), aRawPtr);

    EXPECT_THROW(dynamic_unique_cast<B>(std::move(newAPtr)), std::bad_cast);
}

class C : public Base {
public:
    C(int& cnt)
        : instancesCounter_(cnt) { ++instancesCounter_; }

    ~C() override { --instancesCounter_; }

private:
    int& instancesCounter_;
};

static void freeC(C* ptr) { delete ptr; }

static void freeBase(Base* ptr) { delete ptr; }

static void freeVoid(void* ptr) { delete static_cast<Base*>(ptr); }

static int freeVoidWithReturn(void* ptr) {
    delete static_cast<Base*>(ptr);
    return 1;
}

TEST(UniquePtr_should, use_custom_deleter_function) {
    int cnt = 0;
    {
        UniquePtr<C, freeC> ptrA{new C{cnt}};
        UniquePtr<C, freeBase> ptrBase{new C{cnt}};
        UniquePtr<C, freeVoid> ptrVoid{new C{cnt}};
        UniquePtr<C, freeVoidWithReturn> ptrVoidRet{new C{cnt}};
        EXPECT_EQ(cnt, 4);
        Y_UNUSED(ptrA, ptrBase, ptrVoid, ptrVoidRet);
    }
    EXPECT_EQ(cnt, 0);
}

} // namespace maps::common::tests

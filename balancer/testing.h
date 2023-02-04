#pragma once

#include <library/cpp/coroutine/engine/impl.h>

#include <library/cpp/testing/unittest/registar.h>

#include <exception>

#define CORO_TEST_BEGIN_SS(F, E, S)          \
    static void F(TCont*, void*);            \
    Y_UNIT_TEST(F) {                         \
        TContExecutor exec(S);               \
        std::exception_ptr exc;              \
        exec.Execute(F, &exc);               \
        if (exc) {                           \
            try {                            \
                std::rethrow_exception(exc); \
            } catch (...) {                  \
                UNIT_FAIL(CurrentExceptionMessage()); \
            }                                \
        }                                    \
    }                                        \
    static void F(TCont* cont_, void* exc) { \
        auto* const E = cont_->Executor();   \
        try

#define CORO_TEST_BEGIN(F, E) CORO_TEST_BEGIN_SS(F, E, 50000)

#define CORO_TEST_END                                                          \
        catch (...) {                                                          \
            *static_cast<std::exception_ptr*>(exc) = std::current_exception(); \
        }                                                                      \
    }

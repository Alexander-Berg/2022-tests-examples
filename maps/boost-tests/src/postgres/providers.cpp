#include "contexts.hpp"

#include <yandex/maps/renderer5/postgres/SinglePostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/DefaultPostgresTransactionProvider.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;

BOOST_AUTO_TEST_SUITE(providers)

BOOST_AUTO_TEST_SUITE(single)

BOOST_FIXTURE_TEST_CASE(dont_crash_when_provider_is_destroy, TransactionProviderContext<>)
{
    PostgresTransactionProviderPtr singleProvider(
        new SinglePostgresTransactionProvider(provider->getTransaction()));

    PQXXTransactionHolderPtr holder = singleProvider->getTransaction();

    BOOST_CHECK_NO_THROW(holder->get());
    BOOST_CHECK_NO_THROW(singleProvider.reset());
    BOOST_CHECK_THROW(holder->get(), std::exception);
    BOOST_CHECK_NO_THROW(holder.reset());
}

BOOST_FIXTURE_TEST_CASE(multiple_access, TransactionProviderContext<>)
{
    PostgresTransactionProviderPtr singleProvider(
        new SinglePostgresTransactionProvider(provider->getTransaction()));

    PQXXTransactionHolderPtr holder1, holder2;

    BOOST_CHECK_NO_THROW(
        holder1 = singleProvider->getTransaction());
    BOOST_CHECK_THROW(
        holder2 = singleProvider->getTransaction(), std::exception);
}

BOOST_AUTO_TEST_SUITE_END() // single

BOOST_AUTO_TEST_SUITE_END() // providers

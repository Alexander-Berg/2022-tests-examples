#include <yandex/maps/navikit/providers/bookmarks/bookmarks_iterator.h>
#include <yandex/maps/navikit/mocks/mock_bookmarks_provider.h>

#include <boost/test/unit_test.hpp>
#include <iterator>

using namespace testing;

namespace yandex::maps::navikit::providers::bookmarks {

struct Fixture {
    MockBookmarksProvider provider;
};

BOOST_AUTO_TEST_SUITE(BookmarksIteratorSuite)

BOOST_FIXTURE_TEST_CASE(whenBookmarksCollectionsIsNull, Fixture)
{
    EXPECT_CALL(provider, bookmarksCollections());
    BookmarksRange range(provider);

    BOOST_CHECK(range.begin() == range.end());
}

BOOST_FIXTURE_TEST_CASE(whenBookmarksCollectionsIsEmpty, Fixture)
{
    auto emptyCollections = std::make_shared<runtime::bindings::SharedVector<BookmarksCollection>>(
        runtime::bindings::SharedVector<BookmarksCollection>()
        );
    BOOST_CHECK(emptyCollections->size() == 0); // check myself

    EXPECT_CALL(provider, bookmarksCollections()).WillRepeatedly(Return(emptyCollections));
    BookmarksRange range(provider);

    BOOST_CHECK(range.begin() == range.end());
}

BOOST_FIXTURE_TEST_CASE(whenBookmarksCollectionAreEmpty, Fixture)
{
    auto emptyCollections = std::make_shared<runtime::bindings::SharedVector<BookmarksCollection>>(
        runtime::bindings::SharedVector<BookmarksCollection>({
            BookmarksCollection()
        })
    );
    BOOST_CHECK(emptyCollections->size() == 1); // check myself
    BOOST_CHECK(emptyCollections->front().items->empty()); // check myself

    EXPECT_CALL(provider, bookmarksCollections()).WillRepeatedly(Return(emptyCollections));
    BookmarksRange range(provider);

    BOOST_CHECK(range.begin() == range.end());
}

BOOST_FIXTURE_TEST_CASE(traverseOnBookmarksCollections, Fixture)
{
    auto fourBookmarks = std::make_shared<runtime::bindings::SharedVector<BookmarksCollection>>(
        runtime::bindings::SharedVector<BookmarksCollection>({
            BookmarksCollection("one item inside", { BookmarkInfo() }, false),
            BookmarksCollection("two items inside", { BookmarkInfo(), BookmarkInfo() }, false),
            BookmarksCollection("one item inside", { BookmarkInfo() }, false),
        })
    );

    EXPECT_CALL(provider, bookmarksCollections()).WillRepeatedly(Return(fourBookmarks));
    BookmarksRange range(provider);

    const size_t expectedCount = 4;
    BOOST_CHECK_EQUAL(std::distance(range.begin(), range.end()), expectedCount);
}

BOOST_AUTO_TEST_SUITE_END()

}

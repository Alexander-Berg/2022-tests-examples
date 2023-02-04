#include <ads/bsyeti/big_rt/lib/serializable_profile/watcher.h>

#include <ads/bsyeti/caesar/libs/profiles/banner.h>  // as profile sample
#include <ads/bsyeti/big_rt/lib/serializable_profile/helpers.h>

#include <library/cpp/framing/unpacker.h>

#include <yt/yt/client/ypath/rich.h>
#include <yt/yt/client/table_client/helpers.h>

#include <yt/yt/client/unittests/mock/client.h>
#include <yt/yt/client/unittests/mock/transaction.h>

#include <library/cpp/testing/gtest/gtest.h>

using ::testing::_;
using ::testing::Return;
using ::testing::StrictMock;

using TStrictMockTransaction = StrictMock<NYT::NApi::TMockTransaction>;

namespace {
    NYT::TIntrusivePtr<TStrictMockTransaction> CreateTransactionMock() {
        auto mockTx = NYT::New<TStrictMockTransaction>();
        mockTx->Client = NYT::New<NYT::NApi::TMockClient>();
        return mockTx;
    }

    template <class T>
    auto CheckTransactionRow(const T& assertion) {
        return [=](const auto& /*path*/, auto /*nameTable*/, auto modifications, const auto& /*options*/) {
            ASSERT_EQ(1u, modifications.Size());
            NYT::NTableClient::TUnversionedRow row(modifications[0].Row);
            ASSERT_EQ(3u, row.GetCount());
            EXPECT_EQ(1u, NYT::NTableClient::FromUnversionedValue<i64>(row[0]));

            NFraming::TUnpacker unpacker(
                NFraming::EFormat::Lenval,
                NYT::NTableClient::FromUnversionedValue<TStringBuf>(row[1]));

            NCSR::TBannerProfileProto banner;
            TStringBuf skip;
            unpacker.NextFrame(banner, skip);
            EXPECT_TRUE(skip.empty());
            assertion(banner);
        };
    }
}

TEST(TWatcherTest, NoQueueNoWrite) {
    auto mockTx = CreateTransactionMock();
    auto watcher = NYT::New<NBigRT::TWatcher>(NBigRT::TWatcherConfig{});
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.Mutable()->MutableResources()->SetHref("test");
    ASSERT_TRUE(banner.IsDirty());

    auto mutation = banner.Flush();
    ASSERT_FALSE(mutation.Empty());
    EXPECT_FALSE(banner.IsDirty());
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

TEST(TWatcherTest, SkipUnchanged) {
    auto mockTx = CreateTransactionMock();
    NBigRT::TWatcherConfig config;
    config.SetQueuePath("//testQueue");
    config.SetSkipUnchanged(true);
    auto watcher = NYT::New<NBigRT::TWatcher>(config);
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.SetUpdateAlgorithm(NBigRT::EDiffAlgorithm::ZeroPatch);
    banner.Mutable()->MutableResources()->SetHref("test");
    Y_UNUSED(banner.Flush());
    banner.MarkDirty();
    ASSERT_TRUE(banner.IsDirty());
    auto mutation = banner.Flush();
    ASSERT_TRUE(mutation.Empty());
    EXPECT_FALSE(banner.IsDirty());
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

TEST(TWatcherTest, ForceEmptyChanges) {
    auto mockTx = CreateTransactionMock();
    EXPECT_CALL(*mockTx, ModifyRows(NYT::NYPath::TYPath("//testQueue/queue"), _, _, _)).Times(1);

    NBigRT::TWatcherConfig config;
    config.SetQueuePath("//testQueue");
    auto watcher = NYT::New<NBigRT::TWatcher>(config);
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.SetUpdateAlgorithm(NBigRT::EDiffAlgorithm::ZeroPatch);
    banner.Mutable()->MutableResources()->SetHref("test");
    Y_UNUSED(banner.Flush());
    banner.MarkDirty();
    ASSERT_TRUE(banner.IsDirty());
    auto mutation = banner.Flush();
    ASSERT_TRUE(mutation.Empty());
    EXPECT_FALSE(banner.IsDirty());
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

TEST(TWatcherTest, ModifiedColumnsMask) {
    auto mockTx = CreateTransactionMock();
    EXPECT_CALL(*mockTx, ModifyRows(NYT::NYPath::TYPath("//testQueue/queue"), _, _, _)).WillOnce(CheckTransactionRow(
        [&] (const NCSR::TBannerProfileProto& change) {
            ui64 expected = 0u;
            expected |= 1ull << static_cast<ui64>(NCSR::TBannerProfile::EColumns::Resources);
            expected |= 1ull << static_cast<ui64>(NCSR::TBannerProfile::EColumns::OrderID);
            EXPECT_EQ(expected, change.GetServiceFields().GetChangedColumns());
            EXPECT_EQ(0, change.GetServiceFields().GetFlags());

            EXPECT_TRUE(NBigRT::IsColumnChanged(change, NCSR::TBannerProfile::EColumns::Resources));
            EXPECT_TRUE(NBigRT::IsColumnChanged(change, NCSR::TBannerProfile::EColumns::OrderID));
            EXPECT_FALSE(NBigRT::IsColumnChanged(change, NCSR::TBannerProfile::EColumns::Flags));
        }));

    NBigRT::TWatcherConfig config;
    config.SetQueuePath("//testQueue");
    auto watcher = NYT::New<NBigRT::TWatcher>(config);
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.Mutable()->MutableResources()->SetHref("test");
    banner.Mutable()->SetOrderID(1);
    ASSERT_TRUE(banner.IsDirty());

    auto mutation = banner.Flush();
    ASSERT_FALSE(mutation.Empty());
    EXPECT_FALSE(banner.IsDirty());
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

TEST(TWatcherTest, OnDeleteProfile) {
    auto mockTx = CreateTransactionMock();
    EXPECT_CALL(*mockTx, ModifyRows(NYT::NYPath::TYPath("//testQueue/queue"), _, _, _)).WillOnce(CheckTransactionRow(
        [&] (const NCSR::TBannerProfileProto& banner) {
            EXPECT_EQ(0u, banner.GetServiceFields().GetChangedColumns());
            EXPECT_EQ(NBigRT::PSF_DELETED, banner.GetServiceFields().GetFlags());
        }));

    NBigRT::TWatcherConfig config;
    config.SetQueuePath("//testQueue");
    auto watcher = NYT::New<NBigRT::TWatcher>(config);
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.Clear();
    ASSERT_TRUE(banner.IsDirty());

    auto mutation = banner.Flush();
    ASSERT_TRUE(mutation.IsClearing);
    EXPECT_FALSE(banner.IsDirty());
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

TEST(TWatcherTest, AddDeleted) {
    auto mockTx = CreateTransactionMock();
    EXPECT_CALL(*mockTx, ModifyRows(NYT::NYPath::TYPath("//testQueue/queue"), _, _, _)).Times(1);

    NBigRT::TWatcherConfig config;
    config.SetQueuePath("//testQueue");
    auto watcher = NYT::New<NBigRT::TWatcher>(config);
    auto changes = watcher->CreateChangeList(1);
    NCSR::TBannerProfile banner(11);
    banner.MarkDirty();
    auto mutation = banner.Flush();
    ASSERT_TRUE(mutation.IsClearing);
    watcher->Add<NCSR::TBannerProfile>(11, mutation, changes);
    watcher->Write(mockTx, changes);
}

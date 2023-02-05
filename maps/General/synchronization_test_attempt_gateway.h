#pragma once

#include <maps/goods/lib/goods_db/entities/synchronization_test_attempt.h>

#include <maps/goods/lib/chemistry_helpers/chemistry_helpers.h>
#include <maps/libs/sql_chemistry/include/gateway.h>

namespace maps::goods {

struct SynchronizationTestAttemptsTable : sql_chemistry::Table<SynchronizationTestAttempt> {
    static constexpr std::string_view name_{"synchronization_test_attempts"};

    static constexpr sql_chemistry::BigSerialKey id{"id", name_};
    static constexpr sql_chemistry::Int64Column organizationId{"organization_id", name_};
    static constexpr sql_chemistry::Int64Column authorUid{"author_uid", name_};
    static constexpr sql_chemistry::StringColumn url{"url", name_};
    static constexpr sql_chemistry::EnumColumn<ImportFileType> fileType{"file_type", name_};
    static constexpr chemistry_helper::TimePointWithNowAsDefault syncTime{"sync_time", name_};
    static constexpr sql_chemistry::EnumColumn<SynchronizationStatus> syncState{"sync_state", name_};
    static constexpr sql_chemistry::NullableStringColumn message{"message", name_};
    static constexpr sql_chemistry::NullableInt32Column totalItemsCount{"total_items_count", name_};
    static constexpr sql_chemistry::NullableInt32Column parsedItemsCount{"parsed_items_count", name_};
    static constexpr sql_chemistry::NullableInt32Column itemsWithPhotoCount{"items_with_photo_count", name_};

    static constexpr auto columns_()
    {
        return std::tie(
            id,
            organizationId,
            authorUid,
            url,
            fileType,
            syncTime,
            syncState,
            message,
            totalItemsCount,
            parsedItemsCount,
            itemsWithPhotoCount
        );
    }
};

using SynchronizationTestAttemptsGateway = sql_chemistry::Gateway<SynchronizationTestAttemptsTable>;

} // namespace maps::goods

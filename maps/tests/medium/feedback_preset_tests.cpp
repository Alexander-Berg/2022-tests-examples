#include <yandex/maps/wiki/social/feedback/preset.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::feedback::tests {

namespace {

bool operator == (const PresetEntries& lhs, const PresetEntries& rhs)
{
    return
        lhs.types == rhs.types &&
        lhs.workflows == rhs.workflows &&
        lhs.sources == rhs.sources &&
        lhs.ageTypes == rhs.ageTypes &&
        lhs.status == rhs.status &&
        lhs.hidden == rhs.hidden;
}

PresetRoles makeRoles(TId readRoleId, TId allRightsRoleId)
{
    PresetRoles roles;
    roles.setId(RoleKind::Read, readRoleId);
    roles.setId(RoleKind::AllRights, allRightsRoleId);
    return roles;
}

} // unnamed namespace

Y_UNIT_TEST_SUITE(feedback_preset_tests) {

Y_UNIT_TEST_F(add, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    {
        NewPreset newPreset{
            .name = "preset_1",
            .roles = makeRoles(123219, 12321),
        };
        auto preset = addPreset(socialTxn, newPreset);

        UNIT_ASSERT_EQUAL(preset.id, 1);
        UNIT_ASSERT_STRINGS_EQUAL(preset.name, newPreset.name);

        UNIT_ASSERT_EQUAL(preset.entries, newPreset.entries);
    }
    {
        NewPreset newPreset{
            .name = "preset_2",
            .roles = makeRoles(123229, 12322),
            .entries = {
                .types = Types{},
                .workflows = Workflows{},
                .sources = std::vector<std::string>{},
                .ageTypes = AgeTypes{},
                .hidden = false,
                .status = UIFilterStatus::Opened
            }
        };
        auto preset = addPreset(socialTxn, newPreset);

        UNIT_ASSERT_EQUAL(preset.entries, newPreset.entries);
        UNIT_ASSERT_EQUAL(preset.roles, makeRoles(123229, 12322));
    }
    {
        NewPreset newPreset{
            .name = "preset_3",
            .roles = makeRoles(19, 1),
            .entries = {
                .types = Types{Type::Road},
                .workflows = Workflows{Workflow::Task},
                .sources = {{"gps"}},
                .ageTypes = AgeTypes{AgeType::New},
                .hidden = true,
                .status = UIFilterStatus::Resolved
            }
        };
        auto preset = addPreset(socialTxn, newPreset);

        UNIT_ASSERT_EQUAL(preset.entries, newPreset.entries);
        UNIT_ASSERT_EQUAL(preset.roles, newPreset.roles);
    }
    {
        NewPreset newPreset{
            .name = "preset_4",
            .roles = makeRoles(29,2),
            .entries = {
                .types = Types{Type::Road, Type::Address},
                .workflows = Workflows{Workflow::Task, Workflow::Feedback},
                .sources = {{"gps", "fbapi"}},
                .ageTypes = AgeTypes{AgeType::New},
                .hidden = true,
                .status = UIFilterStatus::NeedInfo
            }
        };
        auto preset = addPreset(socialTxn, newPreset);

        UNIT_ASSERT_EQUAL(preset.entries, newPreset.entries);
    }
}

Y_UNIT_TEST_F(name_unique, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    NewPreset preset1{
        .name = "preset_1",
        .roles = makeRoles(19, 1),
    };

    addPreset(socialTxn, preset1);

    NewPreset preset2{
        .name = "preset_1",
        .roles = makeRoles(29, 2),
    };

    UNIT_ASSERT_EXCEPTION(addPreset(socialTxn, preset2), UniquePresetNameError);
}

Y_UNIT_TEST_F(update, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    NewPreset newPreset{
        .name = "preset_1",
        .roles = makeRoles(19, 1),
        .entries = {
            .types = Types{Type::Road},
            .workflows = Workflows{Workflow::Task},
            .sources = {{"gps"}},
            .ageTypes = AgeTypes{AgeType::New},
            .hidden = true,
            .status = UIFilterStatus::Resolved
        }
    };

    auto preset = addPreset(socialTxn, newPreset);

    preset.name = "preset_2";
    preset.roles = PresetRoles{};
    preset.entries.types = Types{Type::Barrier};
    preset.entries.workflows = std::nullopt;
    preset.entries.sources = {{"route-lost"}};
    preset.entries.ageTypes = std::nullopt;
    preset.entries.hidden = std::nullopt;
    preset.entries.status = UIFilterStatus::Opened;

    updatePreset(socialTxn, preset);

    auto presetsUpdated = getPresets(socialTxn);
    UNIT_ASSERT_EQUAL(presetsUpdated.size(), 1);

    const auto& updated = presetsUpdated.front();

    UNIT_ASSERT_STRINGS_EQUAL(updated.name, "preset_2");
    UNIT_ASSERT_EQUAL(updated.entries, preset.entries);
}

Y_UNIT_TEST_F(update_non_existing, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    Preset preset;
    preset.id = 1;
    preset.name = "preset 1";

    UNIT_ASSERT_EXCEPTION(updatePreset(socialTxn, preset), PresetDoesntExistError);
}

Y_UNIT_TEST_F(get, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    NewPreset newPreset{
        .name = "preset",
        .roles = makeRoles(19, 1),
        .entries = {
            .types = Types{Type::Road},
            .workflows = Workflows{Workflow::Task},
            .sources = {{"gps", "multiword, unusual source"}},
            .ageTypes = AgeTypes{AgeType::New},
            .hidden = false,
            .status = UIFilterStatus::Opened
        }
    };
    addPreset(socialTxn, newPreset);

    auto presets = getPresets(socialTxn);
    UNIT_ASSERT_EQUAL(presets.size(), 1);

    const auto& preset = presets.front();

    UNIT_ASSERT_EQUAL(preset.name, newPreset.name);
    UNIT_ASSERT_EQUAL(preset.entries, newPreset.entries);

    const auto& singlePreset = getPreset(socialTxn, preset.id);
    UNIT_ASSERT(singlePreset);
    UNIT_ASSERT_EQUAL(singlePreset->name, preset.name);
    UNIT_ASSERT_EQUAL(singlePreset->entries, preset.entries);

    const auto& nonExistentPreset = getPreset(socialTxn, 1232112321);
    UNIT_ASSERT(!nonExistentPreset);
}

Y_UNIT_TEST_F(get_order, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    NewPreset preset1{
        .name = "b",
        .roles = makeRoles(19, 1),
    };
    addPreset(socialTxn, preset1);

    NewPreset preset2{
        .name = "a",
        .roles = makeRoles(29, 2),
    };
    addPreset(socialTxn, preset2);

    auto presets = getPresets(socialTxn);
    UNIT_ASSERT_EQUAL(presets.size(), 2);

    UNIT_ASSERT_STRINGS_EQUAL(presets[0].name, "a");
    UNIT_ASSERT_STRINGS_EQUAL(presets[1].name, "b");
}

Y_UNIT_TEST_F(delete_preset, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    NewPreset newPreset{
        .name = "preset",
        .roles = makeRoles(19, 1),
    };
    addPreset(socialTxn, newPreset);

    auto presetsBeforeDelete = getPresets(socialTxn);
    UNIT_ASSERT_EQUAL(presetsBeforeDelete.size(), 1);

    deletePreset(socialTxn, presetsBeforeDelete.front().id);

    auto presetsAfterDelete = getPresets(socialTxn);
    UNIT_ASSERT_EQUAL(presetsAfterDelete.size(), 0);
}

}

} // namespace maps::wiki::social::feedback::tests

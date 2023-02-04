package ru.yandex.infra.stage.podspecs.revision.model;

import java.util.Arrays;

public class DummyRevisionSchemeTestData {

    public static final PatcherClassInfo FIRST_V1, SECOND_V1, SECOND_V2, THIRD_V1, THIRD_V2, FOURTH_V1, FOURTH_V2;

    public static final Revision REVISION_1, REVISION_2, REVISION_3;
    public static final Revision[] REVISIONS;

    public static final RevisionScheme DUMMY_REVISION_SCHEME;

    private static CompositeInfo compositeInfoWithOrder(PatcherClassInfo... patcherClassInfos) {
        var orderNames = Arrays.stream(patcherClassInfos)
                .map(PatcherClassInfo::getClassName)
                .toArray(String[]::new);

        return new CompositeInfo(orderNames);
    }

    static {
        FIRST_V1 = new PatcherClassInfo("dummy.first", "FirstDummyPatcherV1");
        SECOND_V1 = new PatcherClassInfo("dummy.second", "SecondDummyPatcherV1");
        SECOND_V2 = new PatcherClassInfo("dummy.second", "SecondDummyPatcherV2");
        THIRD_V1 = new PatcherClassInfo("dummy.third", "ThirdDummyPatcherV1");
        THIRD_V2 = new PatcherClassInfo("dummy.third", "ThirdDummyPatcherV2");

        FOURTH_V1 = new PatcherClassInfo("dummy.four", "EndpointSetPatcherV1");
        FOURTH_V2 = new PatcherClassInfo("dummy.four", "EndpointSetPatcherV2");


        SpecInfo podSpecInfo1 = new SpecInfo(new PatcherClassInfo[]{FIRST_V1, SECOND_V1, THIRD_V1},
                compositeInfoWithOrder(FIRST_V1, SECOND_V1, THIRD_V1));
        SpecInfo endpointSetSpecInfo1 = new SpecInfo(new PatcherClassInfo[]{FOURTH_V1, FOURTH_V2},
                compositeInfoWithOrder(FOURTH_V1, FOURTH_V2));
        REVISION_1 = new Revision(1, "All versions 1", podSpecInfo1, endpointSetSpecInfo1);

        SpecInfo podSpecInfo2 = new SpecInfo(new PatcherClassInfo[]{FIRST_V1, SECOND_V1, THIRD_V2},
                compositeInfoWithOrder(SECOND_V1, THIRD_V2, FIRST_V1));
        SpecInfo endpointSetSpecInfo2 = new SpecInfo(new PatcherClassInfo[]{FOURTH_V1, FOURTH_V2},
                compositeInfoWithOrder(FOURTH_V1, FOURTH_V2));
        REVISION_2 = new Revision(2, "Third patcher V2 (with new base class)",
                podSpecInfo2, endpointSetSpecInfo2);

        SpecInfo podSpecInfo3 = new SpecInfo(new PatcherClassInfo[]{FIRST_V1, SECOND_V2, THIRD_V2},
                compositeInfoWithOrder(THIRD_V2, FIRST_V1, SECOND_V2));
        SpecInfo endpointSetSpecInfo3 = new SpecInfo(new PatcherClassInfo[]{FOURTH_V1, FOURTH_V2},
                compositeInfoWithOrder(FOURTH_V1, FOURTH_V2));
        REVISION_3 = new Revision(3, "Second patcher V2 (same base class as V1)"
                , podSpecInfo3, endpointSetSpecInfo3
        );

        REVISIONS = new Revision[]{
                REVISION_1, REVISION_2, REVISION_3
        };

        DUMMY_REVISION_SCHEME = new RevisionScheme(
                REVISIONS, REVISION_2.getId()
        );
    }
}

package ru.yandex.intranet.d;

import ru.yandex.intranet.d.model.TenantId;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;

/**
 * TestResourceTypes.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 07.12.2020
 */
public class TestResourceTypes {
    public static final String YP_HDD = "44f93060-e367-44e6-b069-98c20d03dd81";
    public static final String YDP_HDD = "5fb2d884-614d-44e7-9aca-bed438b6e73d";
    public static final String YP_SSD = "135df7d8-c814-4878-9bfa-9e56b082c51f";
    public static final String YP_CPU = "86407d21-0bd9-48ca-9a81-5e40fb3d8477";
    public static final String YP_RAM = "954f1e61-8f58-46a1-bf9d-a3ef6f03c9bb";
    public static final String YP_VIRTUAL = "241022f9-ed4b-ecd0-d1b0-558e95cd8b63";

    public static final ResourceTypeModel YP_HDD_MODEL = new ResourceTypeModel(
            "44f93060-e367-44e6-b069-98c20d03dd81",
            new TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "hdd",
            "HDD",
            "HDD",
            "HDD",
            "HDD",
            false,
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            null,
            3L,
            null
    );

    public static final ResourceTypeModel YP_CPU_MODEL = new ResourceTypeModel(
            "86407d21-0bd9-48ca-9a81-5e40fb3d8477",
            new TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "cpu",
            "CPU",
            "CPU",
            "CPU",
            "CPU",
            false,
            "c2807482-a3b9-4e16-822c-64ff47154ee2",
            null,
            1L,
            null
    );

    private TestResourceTypes() {
    }
}

package ru.yandex.partner.core.entity.queue;


public enum TestTaskType {
    TEST_NON_CONCURRENT_TASK(254),
    TEST_TASK(255);

    private final int typeId;

    TestTaskType(int typeId) {
        this.typeId = typeId;
    }

    public int getTypeId() {
        return typeId;
    }

}

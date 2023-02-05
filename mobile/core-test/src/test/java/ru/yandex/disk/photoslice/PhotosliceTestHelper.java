package ru.yandex.disk.photoslice;

class PhotosliceTestHelper {

    static Moment.Builder newMomentBuilder() {
        return Moment.Builder.newBuilder()
                .setItemsCount(0)
                .setFromDate(0L)
                .setToDate(0L)
                .setLocalityEn("Moscow")
                .setLocalityRu("Москва")
                .setLocalityTr("Moskova")
                .setLocalityUk("Москва")
                .setIsInited(false);
    }
}

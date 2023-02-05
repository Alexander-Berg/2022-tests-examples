package ru.yandex.disk.autoupload.observer;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class StorageListProviderImplTest {

    @Test
    public void shouldGetExternalFilesDir() throws Exception {
        File in = new File("/storage/16E9-0F0F/Android/data/ru.yandex.disk/files");
        File out = StorageListProviderImpl.getStorageRoot(in);
        assertThat(out, equalTo(new File("/storage/16E9-0F0F")));
    }
}

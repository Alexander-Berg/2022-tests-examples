package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

public class FolderTestFakeDataRule extends FakeAccountRule {

    public FolderTestFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addArchiveFolder();
        List<String> userFolders = Arrays.asList(
                "!abc",
                "1",
                "123",
                "9",
                "AB",
                "Ab",
                "aB",
                "ab",
                "TEST",
                "test",
                "zed's dead",
                "атест",
                "Скидки",
                "тест"
        );
        List<String> userLabels = Arrays.asList(
                "hoho",
                "label"
        );
        account.addFolders(userFolders.stream().map(name -> account.newFolder(name).build()).collect(Collectors.toList()));
        account.addLabels(userLabels.stream().map(name -> account.newLabel(name).build()).collect(Collectors.toList()));
    }
}

package ru.yandex.disk.mocks;

import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.test.TestObjectsFactory;

import javax.annotation.NonnullByDefault;
import javax.annotation.Nullable;

@NonnullByDefault
public class CredentialsManagerWithUser extends CredentialsManager {

    @Nullable
    private Credentials activeAccount;

    public CredentialsManagerWithUser(final String user) {
        this(createCredentials(user, 0L));
    }

    public CredentialsManagerWithUser(final Credentials credentials) {
        super(null, null, null, null, null, null, null);
        activeAccount = credentials;
    }

    @Override
    public void login(final String user, final long uid) {
        activeAccount = createCredentials(user, uid);
    }

    @Override
    public boolean isUsedInAppCredentials(Credentials credentials) {
        return credentials.equals(activeAccount);
    }

    @Override
    public Credentials getActiveAccountCredentials() {
        return activeAccount;
    }

    @Override
    public boolean hasActiveAccount() {
        return getActiveAccountCredentials() != null;
    }

    @Override
    public void logout(final LogoutCause cause) {
        activeAccount = null;
    }

    private static Credentials createCredentials(final String user, final long uid) {
        return new Credentials(user, uid);
    }
}

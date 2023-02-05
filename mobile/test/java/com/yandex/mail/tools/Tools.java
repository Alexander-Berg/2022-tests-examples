package com.yandex.mail.tools;

import android.net.Uri;
import android.util.SparseArray;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.util.ContentResolverCallbacksForTests;
import com.yandex.mail.util.IOUtil;
import com.yandex.mail.util.NonInstantiableException;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import kotlin.Unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.robolectric.Shadows.shadowOf;

public final class Tools {

    private Tools() {
        throw new NonInstantiableException();
    }

    /*
        The following method is used to bypass the crypto security policy on a desktop JVM.
        It seems like a simplest way, not requiring JVM patching or Sun's approval.
        It is a really dirty hack and I'd be happy to do it some other way.
        Borrowed from http://stackoverflow.com/questions/1179672/unlimited-strength-jce-policy-files
    */
    public static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            return;
        }
        try {
        /*
          * Do the following, but with reflection to bypass access checks:
          *
          * JceSecurity.isRestricted = false;
          * JceSecurity.defaultPolicy.perms.clear();
          * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
        */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            setFinalStatic(isRestrictedField, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));
        } catch (final Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private static boolean isRestrictedCryptography() {
        // This simply matches the Oracle JRE, but not OpenJDK.
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

    public static String getFileContents(String fname) throws IOException {
        File file = new File(Tools.class.getResource("/" + fname).getFile());
        return IOUtil.readStreamToString(new FileInputStream(file));
    }

    public static Uri registerFile(File file) throws FileNotFoundException {
        Uri uri = Uri.fromFile(file);
        shadowOf(IntegrationTestRunner.app().getContentResolver()).registerInputStream(uri, new FileInputStream(file));
        return uri;
    }

    public static void withinFileInputStreamAutoRegistrationContext(Runnable runnable) throws FileNotFoundException {
        assertThat(ContentResolverCallbacksForTests.getWillOpenInputStreamCallback()).isNull();
        ContentResolverCallbacksForTests.setWillOpenInputStreamCallback(uri -> {
            assertThat(uri.getScheme()).isEqualTo("file");
            assertThat(uri.getPath()).isNotNull();
            File file = new File(uri.getPath());
            try {
                registerFile(file);
            } catch (IOException e) {
                fail("Exception was thrown during file registration: " + file, e);
            }
            return Unit.INSTANCE;
        });

        runnable.run();

        ContentResolverCallbacksForTests.setWillOpenInputStreamCallback(null);
    }

    @NonNull
    public static File createFileWithContent(@NonNull byte[] bytes) throws IOException {
        File attFile = File.createTempFile("attachment", null);
        FileOutputStream fo = new FileOutputStream(attFile);
        IOUtils.write(bytes, fo);
        fo.close();
        return attFile;
    }

    public static File createRandomFile(int n) throws IOException {
        return createRandomFile(n, 1);
    }

    public static File createRandomFile(int n, long seed) throws IOException {
        byte[] bytes = new byte[n];
        new Random(seed).nextBytes(bytes);
        return createFileWithContent(bytes);
    }

    @NonNull
    public static <T> List<T> enrichSparseArray(@NonNull SparseArray<T> array) {
        final List<T> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(array.get(array.keyAt(i)));
        }
        return result;
    }

    // http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection
    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}

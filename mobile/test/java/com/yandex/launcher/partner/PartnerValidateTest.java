package com.yandex.launcher.partner;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;
import com.yandex.launcher.common.util.ExternalContextFactory;
import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.preferences.Preference;
import com.yandex.launcher.preferences.providers.partner.PartnerValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PartnerValidateTest extends BaseRobolectricTest {

    public PartnerValidateTest() {
    }

    @Test
    public void ignoreValidate() {
        for(String p : ignorePackages) {
            PartnerValidator.validate(null, p);
        }
    }

    @Test
    public void validatePartner() {
        ExternalContextFactory.init(getContext());
        final PartnerContext partnerContext = new PartnerContext(getContext());
        PartnerValidator.validate(partnerContext, null);
    }

    private static Set<String> ignorePackages = new HashSet<>();
    static {
        ignorePackages.add("com.yandex.launcherpartner.fly");
        ignorePackages.add("com.yandex.launcherpartner.dexp");
        ignorePackages.add("com.yandex.launcherpartner.apploader");
        ignorePackages.add("com.yandex.launcherpartner.arkdevices");
        ignorePackages.add("com.yandex.launcherpartner.bmobile");
        ignorePackages.add("com.yandex.launcherpartner.multilaser");
        ignorePackages.add("com.yandex.launcherpartner.mobilink");
    }

    private static Context getContext() {
        return ApplicationProvider.getApplicationContext().getApplicationContext();
    }

    private static final String EXPECTED_CHECK_SUM = "c0db4317d62e5f1ea90dacf9fad7f0c7";

    private static class PartnerResource extends Resources {

        private static Map<String, Integer> idMap = new HashMap<>();
        private static Map<Integer, String> stringValuesMap = new HashMap<>();
        private static Map<Integer, String[]> stringArrayValuesMap = new HashMap<>();

        static {
            idMap.put(Preference.CLID_SEARCH.externalKey, Preference.CLID_SEARCH.hashCode());
            idMap.put(Preference.CLID_RECOMMENDATION.externalKey, Preference.CLID_RECOMMENDATION.hashCode());
            idMap.put(Preference.CLID_ZEN.externalKey, Preference.CLID_ZEN.hashCode());
            idMap.put(Preference.CLID_CUSTOM.externalKey, Preference.CLID_CUSTOM.hashCode());
            idMap.put(Preference.CLID_CUSTOM_VALUES.externalKey, Preference.CLID_CUSTOM_VALUES.hashCode());
            idMap.put(Preference.TRACKING_ID.externalKey, Preference.TRACKING_ID.hashCode());
            idMap.put(PartnerValidator.PREFERENCE_CHECKSUM, PartnerValidator.PREFERENCE_CHECKSUM.hashCode());

            stringValuesMap.put(Preference.CLID_SEARCH.hashCode(), "352682");
            stringValuesMap.put(Preference.CLID_RECOMMENDATION.hashCode(), "985647");
            stringValuesMap.put(Preference.CLID_ZEN.hashCode(), "965472");
            stringValuesMap.put(Preference.TRACKING_ID.hashCode(), "457131085348775994");
            stringValuesMap.put(PartnerValidator.PREFERENCE_CHECKSUM.hashCode(), EXPECTED_CHECK_SUM);

            stringArrayValuesMap.put(Preference.CLID_CUSTOM.hashCode(), new String[] { "clid1" });
            stringArrayValuesMap.put(Preference.CLID_CUSTOM_VALUES.hashCode(), new String[] { "946372" });
        }

        public PartnerResource() {
            super(getContext().getAssets(), null, null);
        }

        @Override
        public int getIdentifier(String name, String defType, String defPackage) {
            return idMap.get(name);
        }

        @Override
        public String getString(int id) throws NotFoundException {
            return stringValuesMap.get(id);
        }

        @Override
        public String[] getStringArray(int id) throws NotFoundException {
            return stringArrayValuesMap.get(id);
        }
    }

    private static class PartnerContext extends ContextWrapper {

        private final Resources partnerResource;

        public PartnerContext(Context base) {
            super(base);
            partnerResource = new PartnerResource();
        }

        @Override
        public Resources getResources() {
            return partnerResource;
        }

        @Override
        public Context getApplicationContext() {
            // This is 'partner' apps context, so shouldn't have application context
            return null;
        }
    }
 }

package com.yandex.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Implements(value = PackageManager.class)
public class TestPackageManager extends ShadowPackageManager {

    private final Map<Integer, List<ResolveInfo>> filterHashCodeToResolveInfoMap = new HashMap<>();
    private final Map<String, PackageInfo> packageNameToPackageInfoMap = new HashMap<>();

    @Override
    public void addResolveInfoForIntent(Intent intent, List<ResolveInfo> info) {
        List<ResolveInfo> resolveInfoList = filterHashCodeToResolveInfoMap.get(intent.filterHashCode());
        if (resolveInfoList == null) {
            resolveInfoList = new ArrayList<>();
            filterHashCodeToResolveInfoMap.put(intent.filterHashCode(), resolveInfoList);
        }

        resolveInfoList.addAll(info);
    }

    @Override
    public void addResolveInfoForIntent(Intent intent, ResolveInfo info) {
        addResolveInfoForIntent(intent, Arrays.asList(info));
    }

    @Override
    public void removeResolveInfosForIntent(Intent intent, String packageName) {
        final List<ResolveInfo> resolveInfoList =
                filterHashCodeToResolveInfoMap.get(intent.filterHashCode());
        if (resolveInfoList != null) {
            final Iterator<ResolveInfo> it = resolveInfoList.iterator();
            while (it.hasNext()) {
                final ResolveInfo resolveInfo = it.next();
                if (packageName.equals(resolveInfo.resolvePackageName)) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addPackage(PackageInfo packageInfo) {
        packageNameToPackageInfoMap.put(packageInfo.packageName, packageInfo);
    }

    @Override
    public void addPackage(String packageName) {

    }

    @Override
    public void removePackage(String packageName) {
        packageNameToPackageInfoMap.remove(packageName);
    }

    @Override
    public void setSystemFeature(String name, boolean supported) {

    }

    @Override
    public void addDrawableResolution(String packageName, int resourceId, Drawable drawable) {

    }

    @Override
    public void addActivityIcon(ComponentName component, Drawable d) {

    }

    @Override
    public void addActivityIcon(Intent intent, Drawable d) {

    }

    @Override
    public void setApplicationIcon(String packageName, Drawable d) {

    }
}

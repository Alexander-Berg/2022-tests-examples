IF (OS_ANDROID)
    MOBILE_BOOST_TEST_APK(com.yandex.test.unittests)
    MOBILE_TEST_APK_AAR_MANIFEST(maps/mobile/tools/ya_make_helpers/Manifest.xml)

    MOBILE_TEST_APK_AAR_AARS(
        com.google.android.gms:play-services-location:17.0.0
        com.google.android.gms:play-services-safetynet:17.0.0
        androidx.annotation:annotation:1.1.0
        androidx.core:core:1.1.0
        androidx.car.app:app:1.0.0-rc01
    )
    IF (ARCH_ARM7)
        SET(ARCH_NAME armeabi-v7a)
        SET(LIBCXX_SANDBOX_ID 1537672555)
    ELSEIF (ARCH_ARM64)
        SET(ARCH_NAME arm64-v8a)
        SET(LIBCXX_SANDBOX_ID 1537675168)
    ELSEIF (ARCH_I386)
        SET(ARCH_NAME x86)
        SET(LIBCXX_SANDBOX_ID 1537677223)
    ELSEIF (ARCH_X86_64)
        SET(ARCH_NAME x86_64)
        SET(LIBCXX_SANDBOX_ID 1537678485)
    ELSE()
        MESSAGE(FATAL_ERROR Unsupported platform)
    ENDIF()
    FROM_SANDBOX(FILE ${LIBCXX_SANDBOX_ID} RENAME libc++_shared.so OUT_NOAUTO jni_libs/${ARCH_NAME}/libc++_shared.so)
    DECLARE_EXTERNAL_RESOURCE(EMULATOR_ENV sbr:1241555565)
    ENV(LD_LIBRARY_PATH=$EMULATOR_ENV_RESOURCE_GLOBAL/usr/lib/x86_64-linux-gnu)

    BUNDLE_EXTRA_INPUTS(${BINDIR}/jni_libs/${ARCH_NAME}/libc++_shared.so)

    SIZE(LARGE)
    TAG(ya:fat ya:force_sandbox ya:maps_mobile_android sb:intel_e5_2660v4 sb:intel_gold_6230 ya:exotic_platform)
ELSE()
    BOOSTTEST()
ENDIF()

IF(OS_LINUX OR OS_DARWIN)
    PEERDIR(library/cpp/testing/boost_test_main)
ENDIF()

NO_UTIL()

IF (OS_LINUX AND QT_REQUIRED AND OPENGL_REQUIRED)
    PEERDIR(build/platform/opengl)
    PEERDIR(build/platform/qt)
    ENV(LD_LIBRARY_PATH=$QT_RESOURCE_GLOBAL/lib:$OPENGL_RESOURCE_GLOBAL/usr/lib/x86_64-linux-gnu/mesa:$OPENGL_RESOURCE_GLOBAL/usr/lib/x86_64-linux-gnu)
ELSEIF(OS_DARWIN)
    PEERDIR(build/platform/qt)
    ENV(LD_LIBRARY_PATH=$QT_RESOURCE_GLOBAL/lib)
    ENV(DYLD_FRAMEWORK_PATH=$QT_RESOURCE_GLOBAL/lib)
ENDIF()

IF (OS_IOS AND ARCH_X86_64)
    LDFLAGS(-framework IOSurface)
ENDIF()

IF (OS_DARWIN OR OS_IOS)
    SIZE(LARGE)
    TAG(ya:fat ya:force_sandbox ya:exotic_platform_use_maps_mobile_osx_big_sur_hosts sb:USER_MONOREPO&MOBILE_MONOREPO&OSX_BIG_SUR&force_sandbox_tags=true&xcode=13.2.1)
ENDIF()

package com.yandex.launcher.contacts;

import android.content.Intent;
import android.net.Uri;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.ProgramList;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lapaevm on 04.08.16.
 *
 * WARNING: if you run the test from an IDE and it fails with java.lang.VerifyError
 * add -noverify VM option to the run configuration
 */

public class RawContactsUtilsTest extends BaseRobolectricTest {

    //region ---------------------------------------- Actual mimetype strings

    private static final String VIBER_MIME_MSG = "vnd.android.cursor.item/vnd.com.viber.voip.viber_number_message";
    private static final String VIBER_MIME_CALL_NO_ACC = "vnd.android.cursor.item/vnd.com.viber.voip.viber_out_call_none_viber";
    private static final String VIBER_MIME_CALL = "vnd.android.cursor.item/vnd.com.viber.voip.viber_number_call";
    private static final String VIBER_MIME_VOICE = "vnd.android.cursor.item/vnd.com.viber.voip.google_voice_message";

    private static final String FB_MIME_MSG = "vnd.android.cursor.item/com.facebook.messenger.chat";
    private static final String FB_MIME_CALL = "vnd.android.cursor.item/com.facebook.messenger.audiocall";
    private static final String FB_MIME_VIDEO = "vnd.android.cursor.item/com.facebook.messenger.videocall";

    private static final String WHATSAPP_MIME_MSG = "vnd.android.cursor.item/vnd.com.whatsapp.profile";
    private static final String WHATSAPP_MIME_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call";

    private static final String TLGRM_MIME_MSG = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile";

    //endregion

    public RawContactsUtilsTest() throws NoSuchFieldException, IllegalAccessException {
    }

    //region --------------------------------------- Actual tests

    @Test
    public void testCommunicationsSorting() {

        final String lastUsedPackage = CommApp.WHATSAPP.getPackageName();
        final ProgramList programList = getMockedProgramList();
        final List<Communication> listToSort = getFullUnorderedCommunicationList();
        final List<Communication> listOrdered = getFullOrderedCommunicationList();

        Assert.assertEquals(listOrdered.size(), listToSort.size());
        Assert.assertNotEquals(listOrdered, listToSort);
        RawContactsUtils.sortCommunications(listToSort, lastUsedPackage, programList);

        Assert.assertEquals("Invalid items order", listOrdered, listToSort);

        do {
            final int posToDelete = (int) (Math.random() * (listOrdered.size() - 1));
            listToSort.remove(posToDelete);
            listOrdered.remove(posToDelete);

            Collections.shuffle(listToSort);

            RawContactsUtils.sortCommunications(listToSort, lastUsedPackage, programList);
            Assert.assertEquals("Invalid items order", listOrdered, listToSort);
        } while (listOrdered.size() > 2);
    }

    @Test
    public void testCommWrapperMimeTypeFilter() {

        Assert.assertNotEquals(
                CommFactory.createWrapper(CommApp.TELEGRAM, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.FB, true))
        );

        Assert.assertNotEquals(
                CommFactory.createWrapper(CommApp.FB, Mime.VID),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.FB, true))
        );

        Assert.assertNotEquals(
                CommFactory.createWrapper(CommApp.FB, Mime.VID),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.FB, false))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.FB, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.FB, true))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.FB, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.FB, false))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.TELEGRAM, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.TELEGRAM, false))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.VIBER, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.VIBER, false))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.VIBER, Mime.CALL2),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.VIBER, true))
        );

        Assert.assertEquals(
                CommFactory.createWrapper(CommApp.WHATSAPP, Mime.MSG),
                RawContactsUtils.getNeededWrapper(CommFactory.createWrapperList(CommApp.WHATSAPP, false))
        );

    }

    //endregion

    //region --------------------------------------- Communications preparing

    private ProgramList getMockedProgramList() {
        final ProgramList programList = Mockito.mock(ProgramList.class);
        Mockito.when(programList.getPackageRatingPosition(CommApp.INFO.getPackageName())).thenReturn(1500);
        Mockito.when(programList.getPackageRatingPosition(CommApp.CALLER.getPackageName())).thenReturn(Integer.MAX_VALUE);
        Mockito.when(programList.getPackageRatingPosition(CommApp.SMSER.getPackageName())).thenReturn(1000);

        Mockito.when(programList.getPackageRatingPosition(CommApp.WHATSAPP.getPackageName())).thenReturn(15);
        Mockito.when(programList.getPackageRatingPosition(CommApp.VIBER.getPackageName())).thenReturn(10);
        Mockito.when(programList.getPackageRatingPosition(CommApp.TELEGRAM.getPackageName())).thenReturn(5);
        Mockito.when(programList.getPackageRatingPosition(CommApp.FB.getPackageName())).thenReturn(20);

        Mockito.when(programList.getPackageRatingPosition(CommApp.SOME_OTHER.getPackageName())).thenReturn(Integer.MAX_VALUE / 2);
        Mockito.when(programList.getPackageRatingPosition(CommApp.SOME_OTHER_2.getPackageName())).thenReturn(Integer.MAX_VALUE);

        return programList;
    }

    private List<Communication> getFullUnorderedCommunicationList() {
        final ArrayList<Communication> list = new ArrayList<>(CommApp.values().length);

        list.add(CommFactory.createCommunication(CommApp.SOME_OTHER_2));
        list.add(CommFactory.createCommunication(CommApp.FB));
        list.add(CommFactory.createCommunication(CommApp.VIBER));
        list.add(CommFactory.createCommunication(CommApp.SMSER));
        list.add(CommFactory.createCommunication(CommApp.WHATSAPP));
        list.add(CommFactory.createCommunication(CommApp.CALLER));
        list.add(CommFactory.createCommunication(CommApp.TELEGRAM));
        list.add(CommFactory.createCommunication(CommApp.INFO));
        list.add(CommFactory.createCommunication(CommApp.SOME_OTHER));

        return list;
    }

    private List<Communication> getFullOrderedCommunicationList() {
        final ArrayList<Communication> list = new ArrayList<>(CommApp.values().length);

        list.add(CommFactory.createCommunication(CommApp.INFO));
        list.add(CommFactory.createCommunication(CommApp.CALLER));
        list.add(CommFactory.createCommunication(CommApp.SMSER));

        list.add(CommFactory.createCommunication(CommApp.WHATSAPP));
        list.add(CommFactory.createCommunication(CommApp.TELEGRAM));
        list.add(CommFactory.createCommunication(CommApp.VIBER));
        list.add(CommFactory.createCommunication(CommApp.FB));

        list.add(CommFactory.createCommunication(CommApp.SOME_OTHER));
        list.add(CommFactory.createCommunication(CommApp.SOME_OTHER_2));

        return list;
    }

    //endregion

    //region -------------------------------------- Objects factory

    private enum Mime {
        MSG, CALL, CALL2, VID
    }

    private enum CommApp {

        VIBER ("com.viber.voip"), WHATSAPP ("com.whatsapp"), TELEGRAM ("org.telegram.messenger"),
        FB ("com.facebook.messenger"), SOME_OTHER ("some.other.app"), SOME_OTHER_2 ("some.other.app2"),
        CALLER ("com.android.caller"), SMSER ("com.android.messaging"), INFO ("com.android.contacts");

        private String packageName;

        CommApp(String packageName) {
            this.packageName = packageName;
        }

        String getPackageName() {
            return packageName;
        }
    }

    private static class CommFactory {

        public static Communication createCommunication(CommApp commApp) {
            Intent intent;
            Communication communication;
            switch (commApp) {
                case INFO:
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://some.dummy.content/contact/1"));
                    communication = new Communication(intent, commApp.getPackageName(), null, Communication.DefaultType.INFO);
                    break;

                case SMSER:
                    intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:123321"));
                    communication = new Communication(intent, commApp.getPackageName(), null, Communication.DefaultType.SMS);
                    break;

                case CALLER:
                    intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:123321"));
                    communication = new Communication(intent, commApp.getPackageName(), null, Communication.DefaultType.CALL);
                    break;

                case VIBER:
                case FB:
                case WHATSAPP:
                case TELEGRAM:
                case SOME_OTHER:
                case SOME_OTHER_2:
                    final Uri uri = Uri.parse("content://some.dummy.content/contact/data/"+ commApp.ordinal());
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                    communication = new Communication(intent, commApp.getPackageName(), null, Communication.DefaultType.NONE);
                    break;

                default: throw new IllegalStateException("Invalid CommApp");
            }
            return communication;
        }

        public static List<RawContactsUtils.CommWrapper> createWrapperList(CommApp commApp, boolean singleItem) {
            final List<RawContactsUtils.CommWrapper> list = new ArrayList<>();
            switch (commApp) {
                case FB:
                    list.add(createWrapper(CommApp.FB, Mime.MSG));
                    if (!singleItem) {
                        list.add(createWrapper(CommApp.FB, Mime.CALL));
                        list.add(createWrapper(CommApp.FB, Mime.VID));
                    }
                    break;
                case VIBER:
                    list.add(createWrapper(CommApp.VIBER, Mime.CALL2));
                    if (!singleItem) {
                        list.add(createWrapper(CommApp.VIBER, Mime.CALL));
                        list.add(createWrapper(CommApp.VIBER, Mime.MSG));
                        list.add(createWrapper(CommApp.VIBER, Mime.VID));
                    }
                    break;
                case WHATSAPP:
                    list.add(createWrapper(CommApp.WHATSAPP, Mime.MSG));
                    if (!singleItem) {
                        list.add(createWrapper(CommApp.WHATSAPP, Mime.CALL));
                    }
                    break;
                case TELEGRAM:
                    list.add(createWrapper(CommApp.TELEGRAM, Mime.MSG));
                    break;
                default: throw new IllegalStateException("No wrappers for "+commApp.getPackageName());
            }
            return list;
        }


        public static RawContactsUtils.CommWrapper createWrapper(CommApp commApp, Mime mime) {
            String mimetype;
            switch (commApp) {
                case VIBER:
                    mimetype = getViberMimeString(mime);
                    break;
                case FB:
                    mimetype = getFacebookMimeString(mime);
                    break;
                case WHATSAPP:
                    mimetype = getWhatsAppMimeString(mime);
                    break;
                case TELEGRAM:
                    mimetype = getTelegramMimeString(mime);
                    break;
                default:
                    throw new IllegalStateException("No wrapper for " + commApp.getPackageName());
            }
            final String rawId = String.format("%d%d", commApp.ordinal(), mime.ordinal());
            return new RawContactsUtils.CommWrapper(rawId, mimetype, commApp.getPackageName());
        }



        private static String getViberMimeString(Mime mime) {
            switch (mime) {
                case MSG: return VIBER_MIME_MSG;
                case CALL: return VIBER_MIME_CALL;
                case CALL2: return VIBER_MIME_CALL_NO_ACC;
                case VID: return VIBER_MIME_VOICE;
                default: throw new IllegalStateException("Incorrect mime for Viber wrapper");
            }
        }

        private static String getFacebookMimeString(Mime mime) {
            switch (mime) {
                case MSG: return FB_MIME_MSG;
                case CALL: return FB_MIME_CALL;
                case VID: return FB_MIME_VIDEO;
                default: throw new IllegalStateException("Incorrect mime for Facebook wrapper");
            }
        }

        private static String getWhatsAppMimeString(Mime mime) {
            switch (mime) {
                case MSG: return WHATSAPP_MIME_MSG;
                case CALL: return WHATSAPP_MIME_CALL;
                default: throw new IllegalStateException("Incorrect mime for Facebook wrapper");
            }
        }

        private static String getTelegramMimeString(Mime mime) {
            if (mime.equals(Mime.MSG)) {
                return TLGRM_MIME_MSG;
            }
            throw new IllegalStateException("No such mime for telegram wrapper");
        }

    }

    //endregion

}

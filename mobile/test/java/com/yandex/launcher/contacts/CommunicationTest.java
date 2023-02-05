package com.yandex.launcher.contacts;

import android.content.Intent;
import android.net.Uri;

import com.yandex.launcher.BaseRobolectricTest;

import org.junit.Assert;
import org.junit.Test;

public class CommunicationTest extends BaseRobolectricTest {

    public CommunicationTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void objectEqualityTest() {

        // URIs

        final String number1 = "1234567";
        final String number2 = "1234566";

        final Uri uriSms1 = Uri.parse("smsto:"+number1);
        final Uri uriSms1copy = Uri.parse("smsto:"+number1);
        final Uri uriSms2 = Uri.parse("smsto:"+number2);

        final Uri uriCall1 = Uri.parse("tel:"+number1);
        final Uri uriCall1copy = Uri.parse("tel:"+number1);
        final Uri uriCall2 = Uri.parse("tel:"+number2);

        Assert.assertEquals(uriCall1, uriCall1copy);
        Assert.assertEquals(uriSms1, uriSms1copy);
        Assert.assertNotEquals(uriCall1, uriSms1);
        Assert.assertNotEquals(uriCall1, uriCall2);



        // Intents

        final Intent intentUriSms1SendTo = new Intent(Intent.ACTION_SENDTO, uriSms1);
        final Intent intentUriSms2SendTo = new Intent(Intent.ACTION_SENDTO, uriSms2);
        final Intent intentUriSms1SendToCopy = new Intent(Intent.ACTION_SENDTO, uriSms1);
        final Intent intentUriSms1View = new Intent(Intent.ACTION_VIEW, uriSms1);
        final Intent intentUriCall1 = new Intent(Intent.ACTION_CALL, uriCall1);
        final Intent intentUriCall2 = new Intent(Intent.ACTION_CALL, uriCall2);

        Assert.assertFalse(intentUriSms1View.filterEquals(intentUriSms1SendTo));
        Assert.assertFalse(intentUriSms1SendTo.filterEquals(intentUriSms2SendTo));
        Assert.assertTrue(intentUriSms1SendToCopy.filterEquals(intentUriSms1SendTo));

        // Communications

        final String packageName1 = "packageName1";
        final String packageName2 = "packageName2";

        final Communication commSms1Pk1 = new Communication(intentUriSms1SendTo, packageName1, null, Communication
                .DefaultType.NONE);
        final Communication commSms1Pk2 = new Communication(intentUriSms1SendTo, packageName2, null, Communication
                .DefaultType.NONE);
        final Communication commSms1Pk1Copy = new Communication(intentUriSms1SendTo, packageName1, null, Communication.DefaultType.NONE);
        final Communication commSmsView1Pk1Copy = new Communication(intentUriSms1View, packageName1, null, Communication.DefaultType.NONE);
        final Communication commCall1Pk2 = new Communication(intentUriCall1, packageName2, null, Communication
                .DefaultType.NONE);
        final Communication commCall2Pk2 = new Communication(intentUriCall2, packageName2, null, Communication
                .DefaultType.NONE);
        final Communication commCall2Pk1 = new Communication(intentUriCall2, packageName1, null, Communication
                .DefaultType.NONE);

        Assert.assertEquals(commSms1Pk1, commSms1Pk1Copy);
        Assert.assertNotEquals(commSms1Pk1, commSms1Pk2);
        Assert.assertNotEquals(commSms1Pk1, commSmsView1Pk1Copy);
        Assert.assertNotEquals(commSms1Pk1, commSmsView1Pk1Copy);

        Assert.assertNotEquals(commCall1Pk2, commCall2Pk2);
        Assert.assertNotEquals(commCall2Pk1, commCall2Pk2);

        Assert.assertEquals(commSms1Pk1.hashCode(), commSms1Pk1Copy.hashCode());
        Assert.assertNotEquals(commSms1Pk1.hashCode(), commCall2Pk1.hashCode());
    }

}

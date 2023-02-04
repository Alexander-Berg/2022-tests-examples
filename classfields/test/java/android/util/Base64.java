package android.util;

// this class fixes broken DealerItemConverterTest due to junit issue
// solution stupidly got from https://stackoverflow.com/questions/49109709/how-to-mock-base64-in-android
public class Base64 {
    public static byte[] decode(String str, int flags) {
        return new byte[0];
    }
}

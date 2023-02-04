package com.yandex.maps.testapp.mrc;

import com.yandex.maps.testapp.R;
import com.yandex.runtime.Error;
import com.yandex.runtime.LocalError;
import com.yandex.runtime.auth.PasswordRequiredError;
import com.yandex.runtime.network.ForbiddenError;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.NotFoundError;
import com.yandex.runtime.network.RemoteError;
import com.yandex.runtime.DiskCorruptError;
import com.yandex.runtime.DiskWriteAccessError;
import com.yandex.runtime.DiskFullError;


public class ErrorHandlerHelper {
    public static int getErrorMessageResId(Error error) {
        if (error instanceof NetworkError) {
            return R.string.error_network;
        }
        else if (error instanceof RemoteError) {
            return R.string.error_remote;
        }
        else if (error instanceof ForbiddenError) {
            return R.string.action_forbidden;
        }
        else if (error instanceof NotFoundError) {
            return R.string.not_found;
        }
        else if (error instanceof PasswordRequiredError) {
            return R.string.authorization_failed;
        }
        else if (error instanceof DiskWriteAccessError) {
            return R.string.filesystem_write_access_error;
        }
        else if (error instanceof DiskCorruptError) {
            return R.string.filesystem_corrupt_error;
        }
        else if (error instanceof DiskFullError) {
            return R.string.msg_not_enough_space;
        }
        else if (error instanceof LocalError) {
            return R.string.filesystem_io_error;
        }
        return R.string.error_unknown;
    }
}

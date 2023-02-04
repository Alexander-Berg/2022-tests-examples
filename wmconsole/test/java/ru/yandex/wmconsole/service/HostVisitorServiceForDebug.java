package ru.yandex.wmconsole.service;

import ru.yandex.wmconsole.data.info.UsersHostsInfo;
import ru.yandex.wmtools.common.error.InternalException;

/**
 * Created by IntelliJ IDEA.
 * User: senin
 * Date: 20.03.2007
 * Time: 10:09:24
 */
public class HostVisitorServiceForDebug extends HostVisitorService {

    public UsersHostsInfo checkVerificationForDebug(UsersHostsInfo verInfo) throws InternalException {
        return checkVerification(verInfo);
    }

}
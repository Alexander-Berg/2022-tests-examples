package ru.yandex.spirit.it_tests;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.experimental.UtilityClass;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;

@UtilityClass
public class SshEngineer {
    public static void changeFn(SpiritKKT kkt, String newFn) {
        KKTSshExecute(
                String.format("python3 /kkt/patch_fn_file.py change_fn \"%s\"", newFn),
                kkt.address, 22
        );
    }

    public static void makeFnArchive(SpiritKKT kkt) {
        KKTSshExecute(
                "python3 /kkt/patch_fn_file.py make_archive", kkt.address, 22
        );
    }

    public static void clearQueue(SpiritKKT kkt) {
        KKTSshExecute(
                "python3 /kkt/patch_fn_file.py clear_queue", kkt.address, 22
        );
    }

    private static void KKTSshExecute(String command, String host, Integer port) {
        Session session = null;
        ChannelExec channel = null;

        try {
            session = new JSch().getSession("root", host, port);
            session.setPassword("password");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}

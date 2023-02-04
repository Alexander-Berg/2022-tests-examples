#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestTimeoutIndexerConnect)
bool Run() override {
    const TBackendProxyConfig::TIndexer& indexer = Controller->GetConfig().Indexer;
    TSocket SocketDisk(TNetworkAddress(indexer.Host, indexer.Port));
    int buf = 100;
    SocketDisk.SetSocketTimeout(10);
    int res = SocketDisk.Recv(&buf, sizeof(buf));
    if (res > 0)
        ythrow yexception() << "Incorrect recv result";
    TSocket SocketDiskUnlimit(TNetworkAddress(indexer.Host, indexer.Port));
    SocketDiskUnlimit.Send(&buf, sizeof(buf));
    SocketDiskUnlimit.Send(&buf, sizeof(buf));
    SocketDiskUnlimit.Send(&buf, sizeof(buf));
    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["indexer.disk.ConnectionTimeout"] = 20;
        (*ConfigDiff)["indexer.memory.ConnectionTimeout"] = 20;
        return true;
    }
};

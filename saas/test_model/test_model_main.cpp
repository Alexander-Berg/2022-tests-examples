#include "application.h"
#include <saas/library/daemon_base/daemon/daemon.h>

int main(int argc, char *argv[]) {
    Singleton<TDaemon<TTestModelApplication> >()->Run(argc, argv);
    return 0;
}

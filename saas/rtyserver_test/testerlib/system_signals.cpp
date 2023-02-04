#include "system_signals.h"

bool SendSignal(TProcessId pid, size_t signum){
    DEBUG_LOG << "sending signal '" << signum << "' to process '" << pid << "'" << Endl;
#if defined(_unix_)
        bool ok = kill(pid, signum) == 0;
        return ok;
#else
    ythrow yexception() << "sending " << signum << " to process " << pid << ": not implemented on win";
#endif
    return true;
}

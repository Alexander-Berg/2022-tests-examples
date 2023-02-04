#include <fastcgi2/logger.h>

#define LOGGER DummyLogger

class DummyLogger : public fastcgi::Logger {
public:
    DummyLogger() {}
protected:
   virtual void log(const Level /*level*/, const char* /*format*/, va_list /*args*/) {
   }
};

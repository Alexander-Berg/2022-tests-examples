#include <fastcgi2/handler.h>

class TestHandlerContext: public fastcgi::HandlerContext {
    virtual boost::any getParam(const std::string& /*name*/) const {return false;};
    virtual void setParam(const std::string& /*name*/, const boost::any& /*value*/) {};
};

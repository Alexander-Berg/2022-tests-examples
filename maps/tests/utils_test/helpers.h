#pragma once

#include <maps/fastcgi/keyserv/lib/utils.h>

#include <boost/lexical_cast.hpp>

#include <iostream>
#include <sstream>
#include <string>

void setReqParam(char** res, const std::string& name, const std::string& value)
{
    const std::string content = name + "=" + value;
    *res = new char[content.size() + 1];
    content.copy(*res, content.size());
    (*res)[content.size()] = '\0';
}

size_t getContentLength(std::stringstream& is)
{
    std::string cur = "";
    while (cur.find("Content-Length") == std::string::npos) {
        is >> cur;
    }

    is >> cur;
    return boost::lexical_cast<size_t>(cur);
}

std::string getContent(std::stringstream& is)
{
    const std::string response = is.str();
    return response.substr(response.length() - getContentLength(is));
}

std::ostream& operator <<(std::ostream& os, const maps::keyserv::Subnet& subnet)
{
    os << subnet.str();
    return os;
}


#pragma once

#include <maps/libs/xml/include/xml.h>
#include <maps/analyzer/libs/gpssignal_parser/include/gpssignal_parser.h>
#include <boost/test/floating_point_comparison.hpp>

#include <sstream>

class Helper
{
public:
    Helper& type(const std::string& x)
    {
        type_ = x;
        return *this;
    }

    std::string type() const
    {
        return type_;
    }

    Helper& lat(const std::string& x)
    {
        lat_ = x;
        return *this;
    }

    double lat() const
    {
        return boost::lexical_cast<double>(lat_);
    }

    Helper& lon(const std::string& x)
    {
        lon_ = x;
        return *this;
    }

    double lon() const
    {
        return boost::lexical_cast<double>(lon_);
    }

    Helper& avgSpeed(const std::string& x)
    {
        avgSpeed_ = x;
        return *this;
    }

    double avgSpeed() const
    {
        return boost::lexical_cast<double>(avgSpeed_);
    }

    Helper& direction(const std::string& x)
    {
        direction_ = x;
        return *this;
    }

    int direction() const
    {
        return boost::lexical_cast<double>(direction_);
    }

    Helper& uuid(const std::string& x)
    {
        uuid_ = x;
        return *this;
    }

    std::string uuid() const
    {
        return uuid_;
    }

    Helper& clid(const std::string& x)
    {
        clid_ = x;
        return *this;
    }

    std::string clid() const
    {
        return clid_;
    }

    Helper& time(const std::string& x)
    {
        time_ = x;
        return *this;
    }

    boost::posix_time::ptime time() const
    {
        return maps::analyzer::parseTimeFromRequest(time_);
    }

    void packetId(const std::string& packetId)
    {
        packetId_ = packetId;
    }

    std::string dict() const
    {
        std::string str;
        str += "type=" + type_ +"&";
        str += "latitude=" + lat_ +"&";
        str += "longitude=" + lon_ +"&";
        str += "avg_speed=" + avgSpeed_ +"&";
        str += "direction=" + direction_ +"&";
        str += "uuid=" + uuid_ +"&";
        if (!clid_.empty()) {
            str += "clid=" + clid_ +"&";
        }
        str += "time=" + time_ +"&";
        if (!packetId_.empty()) {
            str += "packetid=" + packetId_;
        }
        return str;
    }

    maps::xml3::Node gpx() const
    {
        std::string str;
        str += "<trkpt  lat=\"" + lat_ + "\" lon=\"" + lon_ + "\">";
        str += "<speed>" + avgSpeed_ + "</speed>";
        str += "<course>" + direction_ + "</course>";
        str += "<time>" + time_ + "<time>";
        str += "</trkpt>";
        doc_.reset(new maps::xml3::Doc(str, maps::xml3::Doc::String));
        return doc_->root();
    }

    maps::xml3::Node xml() const
    {
        std::string str;
        str += "<point ";
        str += "latitude=\"" + lat_ +"\" ";
        str += "longitude=\"" + lon_ +"\" ";
        str += "avg_speed=\"" + avgSpeed_ +"\" ";
        str += "direction=\"" + direction_ +"\" ";
        str += "time=\"" + time_ +"\"";
        str += "/>";
        doc_.reset(new maps::xml3::Doc(str, maps::xml3::Doc::String));
        return doc_->root();
    }

    void checkEqual(const maps::analyzer::data::GpsSignal& signal)
    {
        std::string theClid_ = clid() != "" ? clid() : type();
        BOOST_CHECK_EQUAL(maps::analyzer::VehicleId(theClid_, uuid()),
            signal.vehicleId());
        BOOST_CHECK_CLOSE(lat(), signal.lat(), 1e-6);
        BOOST_CHECK_CLOSE(lon(), signal.lon(), 1e-6);
        BOOST_CHECK_CLOSE(avgSpeed() / 3.6, signal.averageSpeed(), 1e-6);
        BOOST_CHECK_EQUAL(direction(), signal.direction());
        BOOST_CHECK_EQUAL(time(), signal.time());
        //Region is default
        BOOST_CHECK_EQUAL(maps::analyzer::data::GpsSignal().regionId(), signal.regionId());
    }

private:
    std::string lat_;
    std::string lon_;
    std::string avgSpeed_;
    std::string direction_;
    std::string time_;
    std::string type_;
    std::string clid_;
    std::string uuid_;
    std::string packetId_;

    mutable std::auto_ptr<maps::xml3::Doc> doc_;
};

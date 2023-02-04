#pragma once

#include <vector>
#include <iostream>
#include <boost/date_time/posix_time/ptime.hpp>

class Task
{
public:
    Task(size_t type, size_t id, const boost::posix_time::ptime& time)
     :type_(type),
       id_(id),
       time_(time) { }

    size_t type() const
        { return type_; }

    size_t id() const
        { return id_; }

    // Returns time, when task should be processed
    const boost::posix_time::ptime& time() const
        { return time_; }

    const std::vector<size_t>& numbers() const
        { return numbers_; }

    void addNumber(size_t number)
        { numbers_.push_back(number); }

private:
    size_t type_;
    size_t id_;
    boost::posix_time::ptime time_;
    std::vector<size_t> numbers_;
};

inline bool operator == (const Task& lhs, const Task& rhs)
{
    return lhs.id() == rhs.id() &&
        lhs.type() == rhs.type() &&
        lhs.time() == rhs.time() &&
        lhs.numbers() == rhs.numbers();
}

inline std::ostream& operator << (std::ostream& out, const Task& task)
{
    out << task.type() << " " << task.id() << " " /*<< task.time()*/;
    for (size_t number : task.numbers()) {
        out << " " << number;
    }
    return out;
}

typedef std::vector<Task> ProcessedInfo;

const size_t ID_MODULE = 20;
inline size_t selectId(size_t request)
    { return request % ID_MODULE; }

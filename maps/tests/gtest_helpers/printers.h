#pragma once

#include <ostream>

namespace maps::wiki::acl {

class Group;
class Policy;

std::ostream& operator<<(std::ostream& os, const Group& group);
std::ostream& operator<<(std::ostream& os, const Policy& policy);

} // namespace maps::wiki::acl

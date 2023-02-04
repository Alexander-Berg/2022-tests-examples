#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/social_serv_serialize/include/jsonize_aggregated_counter.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::socialsrv::serialize::tests {

using social::feedback::Workflow;
using social::feedback::Type;
using social::feedback::AggregatedCounter;

const json::Value EXPECTED = maps::json::Value::fromString(R"(
{
    "tasksCount": 30,
    "workflows": [
	{
	    "workflow": "feedback",
	    "tasksCount": 20,
	    "sources": [
		{
		    "source": "src1",
		    "tasksCount": 10,
		    "types": [
			{
			    "type": "road",
			    "tasksCount": 5
			},
			{
			    "type": "no-road",
			    "tasksCount": 5
			}
		    ]
		},
		{
		    "source": "src2",
		    "tasksCount": 10,
		    "types": [
			{
			    "type": "road",
			    "tasksCount": 1
			},
			{
			    "type": "no-road",
			    "tasksCount": 1
			},
			{
			    "type": "barrier",
			    "tasksCount": 2
			},
			{
			    "type": "poi",
			    "tasksCount": 6
			}
		    ]
		}
	    ]
	},
	{
	    "workflow": "task",
	    "tasksCount": 10,
	    "sources": [
		{
		    "source": "src1",
		    "tasksCount": 1,
		    "types": [
			{
			    "type": "road",
			    "tasksCount": 1
			}
		    ]
		},
		{
		    "source": "src3",
		    "tasksCount": 9,
		    "types": [
			{
			    "type": "no-road",
			    "tasksCount": 9
			}
		    ]
		}
	    ]
	}
    ]
}
)"
);

Y_UNIT_TEST_SUITE(aggregated_counter)
{

Y_UNIT_TEST(jsonize_test)
{
    std::vector<AggregatedCounter> counters;
    counters.push_back({Workflow::Feedback, "src1", Type::Road, 5});
    counters.push_back({Workflow::Feedback, "src1", Type::NoRoad, 5});

    counters.push_back({Workflow::Feedback, "src2", Type::Road, 1});
    counters.push_back({Workflow::Feedback, "src2", Type::NoRoad, 1});
    counters.push_back({Workflow::Feedback, "src2", Type::Barrier, 2});
    counters.push_back({Workflow::Feedback, "src2", Type::Poi, 6});

    counters.push_back({Workflow::Task, "src1", Type::Road, 1});
    counters.push_back({Workflow::Task, "src3", Type::NoRoad, 9});

    
    json::Builder builder;
    builder << [&](json::ObjectBuilder builder) {
        jsonize(builder, counters);
    };

    UNIT_ASSERT_EQUAL(json::Value::fromString(builder.str()), EXPECTED);
}
 
} // Y_UNIT_TEST_SUITE(aggregated_counter)

} //maps::wiki::socialsrv::serialize::tests

from balance import balance_steps as steps
from btestlib.constants import Export

queue = "EMAIL_MESSAGE"
classname = "EmailMessage"
object_id = 42598467


steps.CommonSteps.export(
    queue,
    classname,
    object_id
)
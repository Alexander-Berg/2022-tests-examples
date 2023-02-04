#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}
#end

import ru.auto.ara.core.robot.Robot
import ru.auto.ara.core.robot.feature

fun perform${NAME}(func: ${NAME}Robot.() -> Unit) = feature(${NAME}Robot(), func)
fun check${NAME}(func: ${NAME}RobotChecker.() -> Unit) = ${NAME}RobotChecker().apply(func)

class ${NAME}Robot : Robot<${NAME}RobotChecker>(${NAME}RobotChecker()) {

}

class ${NAME}RobotChecker {

}

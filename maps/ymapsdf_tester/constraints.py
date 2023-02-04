import logging
import typing as tp

from yt.wrapper.ypath import ypath_join
from yt.wrapper import errors as yt_errors

from maps.garden.sdk.core import GardenError
from maps.garden.sdk.yt import yql_tools

from maps.garden.libs.ymapsdf.lib.common.defs import MAP_OBJECTS_ID_COLUMNS


logger = logging.getLogger('garden.tasks.ymapsdf_tester')


class CheckTableConstraints(yql_tools.YqlTask):
    def __init__(self, make_url: tp.Callable[[int], str]):
        """
        :param make_url: callback to convert object ids to urls
        """
        super().__init__(query=None)
        self._make_url = make_url

    def __call__(self, yt_table, fake_output_resource):
        try:
            extended_schema = yt_table.get_yt_client().get(
                ypath_join(yt_table.path, "@extended_schema"))
        except yt_errors.YtHttpResponseError:
            # This is a new ymapsdf table. It has no schema yet.
            logger.warning(f"Table `{yt_table.path}` has no extended_schema attribute")
            return

        constraints = extended_schema.get("constraints")
        if not constraints:
            logger.warning(f"There is no constraints for the table `{yt_table.path}`")
            return

        matchers = []

        where_condition = "false "
        for constraint in constraints:
            constraint_type = constraint["type"]
            if constraint_type == "expression":
                value = constraint["value"]
                where_condition += f"\nOR NOT ({value})"

            elif constraint_type == "regexp":
                field_name = constraint["field"]
                pattern = constraint["pattern"]

                pattern = pattern.replace("{", "{{")
                pattern = pattern.replace("}", "}}")

                matcher_id = len(matchers)
                matchers.append(f"$match{matcher_id} = Re2::Match('{pattern}');")
                where_condition += f"\nOR (`{field_name}` IS NOT NULL AND NOT $match{matcher_id}(`{field_name}`))"

            else:
                raise RuntimeError(f"Unsupported constraint type '{constraint_type}'")

        all_matchers = "\n".join(matchers)

        query = f"""{all_matchers}\n
SELECT *
FROM `{{yt_table}}`
WHERE {where_condition}
LIMIT 100;"""  # Limit the number of error lines to avoid reading the entire table in case of bugs

        request_result = self._run_query(query, yt_table=yt_table)
        for table in request_result.get_results():
            table.fetch_full_data()
            if table.rows:
                logger.error("Some rows did not pass the checks. The first N results:")
                object_ids = set()
                for row in table.rows:
                    for column_name, value in zip(table.column_names, row):
                        if column_name in MAP_OBJECTS_ID_COLUMNS:
                            object_ids.add(value)
                    logger.error("\t".join([str(cell) for cell in row]))
                object_urls = "\n".join([self._make_url(obj_id) for obj_id in object_ids])
                raise GardenError(
                    f"Constraint checks failed for some objects in table"
                    f" {yt_table.path.split('/')[-1]}\n{object_urls}"
                )

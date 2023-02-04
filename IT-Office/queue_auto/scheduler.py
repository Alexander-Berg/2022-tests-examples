from source.safe_schedule import SafeScheduler, run_threaded
from source.queue_auto.queue_auto import (check_zoo,
                        check_office_and_bu,
                        main_closing_resolve_tickets,
                        mark_printer_down_tasks,
                        check_users_folder_for_dismiss,
                        main_userside_robot)
from source.queue_auto.wait_for_equipment import main_for_equipment_pinging
from source.queue_auto.scheduled_status import scheduled

scheduler = SafeScheduler()

scheduler.every(10).minutes.do(run_threaded, scheduled)
scheduler.every(10).minutes.do(run_threaded, check_zoo)
scheduler.every(1).minutes.do(check_office_and_bu)
scheduler.every(1).hours.do(run_threaded, mark_printer_down_tasks)
scheduler.every(1).hours.do(run_threaded, check_users_folder_for_dismiss)
scheduler.every().day.at("9:00").do(run_threaded, main_closing_resolve_tickets)
scheduler.every().day.at("9:00").do(run_threaded, main_userside_robot)
#scheduler.every().day.at("10:00").do(run_threaded, main_for_equipment_pinging)

jobs_queue_auto = scheduler.jobs
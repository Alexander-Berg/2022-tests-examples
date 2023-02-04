from source.monitoring.monitoring import (
                        main_common_monitoring_collect,
                        main_sip_monitoring,
                        main_sla_monitoring,
                        main_history_actual_model,
                        main_help_eq_monitoring,
                        main_disk_finder,
                        main_tranceivers_finder,
                        main_crm_stat,
                        ext_dismissal_finder)

from source.monitoring.main_big_dashboard import main_unitedashboard_collector
from source.monitoring.equipment_stock_monitoring import monitoring_equipment_stock
from source.monitoring.loader_dashboard import loader_dashboard
from source.monitoring.vendomat_dashboard import vendomat_daily, vendomat_collection_historical
from source.monitoring.software_assets import main_office_assets
from source.monitoring.sip_logs import main_sip_logs

from source.safe_schedule import SafeScheduler, run_threaded

scheduler = SafeScheduler()

scheduler.every(10).minutes.do(run_threaded, main_sip_monitoring)
scheduler.every().day.at("9:00").do(run_threaded, main_sla_monitoring)
scheduler.every().day.at("9:00").do(run_threaded, main_help_eq_monitoring)
scheduler.every().day.at("9:00").do(run_threaded, main_disk_finder)
scheduler.every().monday.do(run_threaded, main_tranceivers_finder)
scheduler.every().day.at("22:00").do(run_threaded, ext_dismissal_finder)
scheduler.every().day.at("3:00").do(run_threaded, main_crm_stat)
scheduler.every().day.at("1:00").do(run_threaded, main_history_actual_model)
scheduler.every(15).minutes.do(run_threaded, main_common_monitoring_collect)
scheduler.every(1).hours.do(run_threaded, main_unitedashboard_collector)
scheduler.every().day.at("1:00").do(run_threaded, monitoring_equipment_stock)
scheduler.every(3).hours.do(run_threaded, loader_dashboard)
scheduler.every().day.at("23:00").do(run_threaded, vendomat_daily)
scheduler.every().sunday.do(run_threaded, main_office_assets)
scheduler.every().day.at("1:00").do(run_threaded, main_sip_logs)

jobs_monitoring = scheduler.jobs
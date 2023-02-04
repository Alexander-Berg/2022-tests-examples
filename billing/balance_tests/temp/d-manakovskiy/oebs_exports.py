from balance import balance_steps as steps

client_id = 1355211971
person_id = 19291490
invoice_id = 147056390
act_id = 155460328

# steps.ExportSteps.export_oebs(client_id=client_id)
steps.ExportSteps.export_oebs(person_id=person_id)
# steps.ExportSteps.export_oebs(invoice_id=invoice_id)
# steps.ExportSteps.export_oebs(act_id=act_id)

# steps.ExportSteps.create_export_record()


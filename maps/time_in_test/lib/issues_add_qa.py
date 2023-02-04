from nile.api.v1 import Record

'''
    Return input issues with qa_engineer field

        issue_rows   |    qa_engineer
    -----------------+-----------------
            ...      |     dalapenko
            ...      |     dalapenko
            ...      |  not_qa_engineer
'''


def issues_add_qa(issues, testers_logins):
    def flatten_by_qa_engineer(rows):
        for row in rows:

            qa_engineer = row.customFields.get(b'qaEngineer')
            if qa_engineer is not None:
                qa_engineer = qa_engineer.get(b'id')

            qa_engineer = qa_engineer \
                if qa_engineer is not None \
                and qa_engineer in testers_logins \
                else str('not_qa_engineer').encode()

            yield Record(
                row,
                qa_engineer=qa_engineer
            )

    return issues.map(flatten_by_qa_engineer)

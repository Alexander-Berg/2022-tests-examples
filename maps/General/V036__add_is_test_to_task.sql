ALTER TABLE ugc.task ADD COLUMN is_test BOOLEAN NOT NULL DEFAULT false;

UPDATE ugc.task SET is_test = skip_evaluation;

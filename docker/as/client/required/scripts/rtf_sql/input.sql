SELECT TOP 100 u.note_id FROM dbo.u01 u WHERE u.rtf_pipeline IN ('U', 'R') AND u.batch=$batch

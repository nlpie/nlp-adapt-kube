SELECT TOP 100 note_id, rtf2plain FROM dbo.u01 WHERE rtf_pipeline like 'P' AND ctakes IN ('U', 'R') AND batch=$batch

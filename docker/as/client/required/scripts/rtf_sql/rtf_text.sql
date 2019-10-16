SELECT rh.content, r.note_id FROM LZ_FV_HL7.hl7_note_hist_reduced_final r INNER JOIN NOTES.rtf_historical rh ON rh.hl7_note_historical_id=r.hl7_note_id WHERE r.note_id=$note_id

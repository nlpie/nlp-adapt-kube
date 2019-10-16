select *
from rdc_dt.dt_note_text
where note_version_id in (
   select note_version_id from rdc_dt.dt_note_meta
   where note_id=$note_id
)

import sqlite3
import sys

conn = sqlite3.connect('schema.db')
in_db = sqlite3.connect(sys.argv[1])
c = conn.cursor()
in_c = in_db.cursor()

ids = [(x[0], 'U', 'U', 'U', 'U', 'U') for x in in_c.execute('select (rowid) from txts')]
c.executemany('insert into source_note (note_id, rtf_pipeline, b9, mm, clamp, ctakes) values (?, ?, ?, ?, ?, ?)', ids)


in_db.commit()
in_db.close()

conn.commit()
conn.close()

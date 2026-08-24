import sqlite3
conn=sqlite3.connect('e:/MY-PROJECTS/android_native_football/platform/android/app/src/main/assets/databases/default/database.sqlite')
c=conn.cursor()
c.execute('PRAGMA table_info(teams)')
print(c.fetchall())

package org.libsdl.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads game data (leagues, teams) from the bundled database.sqlite in assets.
 */
public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String ASSET_DB_PATH = "databases/default/database.sqlite";
    private static final String INTERNAL_DB_NAME = "blurz_game.sqlite";

    private SQLiteDatabase db;
    private Context context;

    public DatabaseHelper(Context context) {
        this.context = context;
        copyDatabaseIfNeeded();
        File dbFile = new File(context.getFilesDir(), INTERNAL_DB_NAME);
        db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private void copyDatabaseIfNeeded() {
        File dbFile = new File(context.getFilesDir(), INTERNAL_DB_NAME);
        
        // Skip copy if already exists to prevent wasteful I/O on every screen
        if (dbFile.exists()) {
            return;
        }

        try {
            InputStream is = context.getAssets().open(ASSET_DB_PATH);
            OutputStream os = new FileOutputStream(dbFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.flush();
            os.close();
            is.close();
            Log.d(TAG, "Database copied to " + dbFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy database", e);
        }
    }

    // ── Data Models ──

    public static class League {
        public int id;
        public String name;
        public String logoUrl; // relative path inside assets, e.g. "databases/default/images_competitions/premierleague.png"

        public League(int id, String name, String logoUrl) {
            this.id = id;
            this.name = name;
            this.logoUrl = logoUrl;
        }
    }

    public static class Team {
        public int id;
        public String name;
        public String logoUrl;   // e.g. "databases/default/images_teams/premierleague/arsenal_logo.png"
        public String kitUrl;    // e.g. "databases/default/images_teams/premierleague/arsenal"
        public String shortName;
        public String color1;
        public String color2;

        public Team(int id, String name, String logoUrl, String kitUrl, String shortName, String color1, String color2) {
            this.id = id;
            this.name = name;
            this.logoUrl = logoUrl;
            this.kitUrl = kitUrl;
            this.shortName = shortName;
            this.color1 = color1;
            this.color2 = color2;
        }
    }

    // ── Queries ──

    public List<League> getLeagues() {
        List<League> leagues = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT id, name, logo_url FROM leagues ORDER BY name", null);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            String logoUrl = "databases/default/" + c.getString(2);
            leagues.add(new League(id, name, logoUrl));
        }
        c.close();
        return leagues;
    }

    public List<Team> getTeams(int leagueId) {
        List<Team> teams = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT id, name, logo_url, kit_url, shortname, color1, color2 FROM teams WHERE league_id = ? ORDER BY name",
            new String[]{ String.valueOf(leagueId) }
        );
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            String logoUrl = "databases/default/" + c.getString(2);
            String kitUrl = "databases/default/" + c.getString(3);
            String shortName = c.getString(4);
            String color1 = c.getString(5);
            String color2 = c.getString(6);
            teams.add(new Team(id, name, logoUrl, kitUrl, shortName, color1, color2));
        }
        c.close();
        return teams;
    }

    public Team getTeam(int teamId) {
        Cursor c = db.rawQuery(
            "SELECT id, name, logo_url, kit_url, shortname, color1, color2 FROM teams WHERE id = ?",
            new String[]{ String.valueOf(teamId) }
        );
        Team team = null;
        if (c.moveToFirst()) {
            team = new Team(
                c.getInt(0), c.getString(1),
                "databases/default/" + c.getString(2),
                "databases/default/" + c.getString(3),
                c.getString(4), c.getString(5), c.getString(6)
            );
        }
        c.close();
        return team;
    }

    public void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}

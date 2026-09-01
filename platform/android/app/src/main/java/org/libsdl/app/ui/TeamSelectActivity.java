package org.libsdl.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Team Selection screen for Blurz Football.
 * Replaces the old C++ TeamSelectPage + ControllerSelectPage.
 * Reads real leagues/teams from database.sqlite and shows actual logos.
 */
public class TeamSelectActivity extends Activity {

    private DatabaseHelper dbHelper;
    private List<DatabaseHelper.League> leagues;

    // Current selections
    private int selectedHomeLeagueIdx = 0;
    private int selectedAwayLeagueIdx = 0;
    private int selectedHomeTeamId = -1;
    private int selectedAwayTeamId = -1;

    // UI references
    private LinearLayout homeLeagueRow;
    private LinearLayout awayLeagueRow;
    private LinearLayout homeTeamGrid;
    private LinearLayout awayTeamGrid;
    private ImageView homeSelectedLogo;
    private ImageView awaySelectedLogo;
    private TextView homeSelectedName;
    private TextView awaySelectedName;
    private TextView nextButton;

    public static TeamSelectActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        BlurzTheme.applyImmersive(this);

        dbHelper = new DatabaseHelper(this);
        leagues = dbHelper.getLeagues();

        int dp4  = BlurzTheme.dpToPx(this, 4);
        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp16 = BlurzTheme.dpToPx(this, 16);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp40 = BlurzTheme.dpToPx(this, 40);

        // ── Root ──
        FrameLayout root = new FrameLayout(this);
        root.setBackground(BlurzTheme.createBackground());

        // ── Main vertical layout (scrollable) ──
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp16, dp8, dp16, dp8);

        // ── Top Bar: Back + Title ──
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        backBtn.setTextColor(BlurzTheme.TEXT_PRIMARY);
        backBtn.setPadding(dp4, dp4, dp16, dp4);
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("SELECT TEAMS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.ACCENT_CYAN);
        title.setLetterSpacing(0.1f);
        topBar.addView(title);

        main.addView(topBar);

        // ── Two-column layout: Home vs Away ──
        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        columns.setWeightSum(2.2f);

        // ── HOME COLUMN ──
        LinearLayout homeCol = createTeamColumn("HOME TEAM", true);

        // ── VS Divider ──
        LinearLayout vsDivider = new LinearLayout(this);
        vsDivider.setOrientation(LinearLayout.VERTICAL);
        vsDivider.setGravity(Gravity.CENTER);
        TextView vsText = new TextView(this);
        vsText.setText("VS");
        vsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        vsText.setTypeface(BlurzTheme.FONT_BOLD);
        vsText.setTextColor(BlurzTheme.ACCENT_CYAN);
        vsText.setShadowLayer(16, 0, 0, BlurzTheme.ACCENT_CYAN);
        vsText.setGravity(Gravity.CENTER);
        vsDivider.addView(vsText);

        // ── AWAY COLUMN ──
        LinearLayout awayCol = createTeamColumn("AWAY TEAM", false);

        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams vsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.2f);
        columns.addView(homeCol, colParams);
        columns.addView(vsDivider, vsParams);
        columns.addView(awayCol, colParams);

        LinearLayout.LayoutParams columnsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        columnsParams.topMargin = dp8;
        main.addView(columns, columnsParams);

        // ── Bottom: NEXT button ──
        nextButton = new TextView(this);
        nextButton.setText("NEXT  →");
        nextButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nextButton.setTypeface(BlurzTheme.FONT_BOLD);
        nextButton.setTextColor(Color.WHITE);
        nextButton.setGravity(Gravity.CENTER);
        nextButton.setBackground(BlurzTheme.createButtonBackground(BlurzTheme.ACCENT_GREEN));
        nextButton.setShadowLayer(12, 0, 4, Color.parseColor("#6600E676"));
        nextButton.setPadding(dp32, dp8, dp32, dp8);
        nextButton.setAlpha(0.4f); // Disabled until both teams selected
        nextButton.setOnClickListener(v -> {
            if (selectedHomeTeamId > 0 && selectedAwayTeamId > 0) {
                Intent intent = new Intent(this, MatchOptionsActivity.class);
                intent.putExtra("homeTeamId", selectedHomeTeamId);
                intent.putExtra("awayTeamId", selectedAwayTeamId);
                startActivity(intent);
            }
        });
        nextButton.setOnTouchListener((v, event) -> {
            if (selectedHomeTeamId <= 0 || selectedAwayTeamId <= 0) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });

        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
            BlurzTheme.dpToPx(this, 180), dp40
        );
        nextParams.gravity = Gravity.CENTER_HORIZONTAL;
        nextParams.topMargin = dp8;
        nextParams.bottomMargin = dp4;
        main.addView(nextButton, nextParams);

        root.addView(main, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);

        // Load initial data
        if (!leagues.isEmpty()) {
            loadTeamsForLeague(true, leagues.get(0).id);
            loadTeamsForLeague(false, leagues.get(0).id);
        }
    }

    private LinearLayout createTeamColumn(String label, boolean isHome) {
        int dp4  = BlurzTheme.dpToPx(this, 4);
        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp32 = BlurzTheme.dpToPx(this, 32);
        int dp40 = BlurzTheme.dpToPx(this, 40);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackground(BlurzTheme.createCardBackground());
        col.setPadding(dp8, dp8, dp8, dp8);

        // ── Header Row ──
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(BlurzTheme.FONT_BOLD);
        title.setTextColor(BlurzTheme.TEXT_DIM);
        title.setLetterSpacing(0.1f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp4);
        col.addView(title);

        // ── Selected Team Display (Horizontal to save space) ──
        LinearLayout selectedRow = new LinearLayout(this);
        selectedRow.setOrientation(LinearLayout.HORIZONTAL);
        selectedRow.setGravity(Gravity.CENTER_VERTICAL);
        selectedRow.setPadding(dp4, 0, dp4, dp8);
        
        ImageView teamImage = new ImageView(this);
        teamImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        teamImage.setAdjustViewBounds(true);
        selectedRow.addView(teamImage, new LinearLayout.LayoutParams(dp32, dp32));

        TextView teamName = new TextView(this);
        teamName.setText("Select a team");
        teamName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        teamName.setTypeface(BlurzTheme.FONT_MEDIUM);
        teamName.setTextColor(BlurzTheme.TEXT_PRIMARY);
        teamName.setPadding(dp8, 0, 0, 0);
        selectedRow.addView(teamName);

        col.addView(selectedRow);

        if (isHome) {
            homeSelectedLogo = teamImage;
            homeSelectedName = teamName;
        } else {
            awaySelectedLogo = teamImage;
            awaySelectedName = teamName;
        }

        // ── League Selector ──
        TextView leagueLabel = new TextView(this);
        leagueLabel.setText("LEAGUE");
        leagueLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        leagueLabel.setTypeface(BlurzTheme.FONT_BOLD);
        leagueLabel.setTextColor(BlurzTheme.TEXT_DIM);
        leagueLabel.setLetterSpacing(0.1f);
        col.addView(leagueLabel);

        HorizontalScrollView leagueScroll = new HorizontalScrollView(this);
        leagueScroll.setHorizontalScrollBarEnabled(false);
        leagueScroll.setPadding(0, dp4, 0, dp4);

        LinearLayout leagueRow = new LinearLayout(this);
        leagueRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < leagues.size(); i++) {
            DatabaseHelper.League league = leagues.get(i);
            final int idx = i;

            FrameLayout leagueItem = new FrameLayout(this);
            leagueItem.setPadding(dp4, dp4, dp4, dp4);

            ImageView leagueIcon = new ImageView(this);
            leagueIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            leagueIcon.setAdjustViewBounds(true);
            AssetImageLoader.load(this, leagueIcon, league.logoUrl);
            leagueItem.addView(leagueIcon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

            if (i == 0) {
                GradientDrawable highlight = new GradientDrawable();
                highlight.setColor(BlurzTheme.CARD_BG_LIGHT);
                highlight.setCornerRadius(12);
                highlight.setStroke(2, BlurzTheme.ACCENT_CYAN);
                leagueItem.setBackground(highlight);
            }

            leagueItem.setOnClickListener(v -> {
                if (isHome) selectedHomeLeagueIdx = idx; else selectedAwayLeagueIdx = idx;
                LinearLayout row = isHome ? homeLeagueRow : awayLeagueRow;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View child = row.getChildAt(j);
                    if (j == idx) {
                        GradientDrawable h = new GradientDrawable();
                        h.setColor(BlurzTheme.CARD_BG_LIGHT);
                        h.setCornerRadius(12);
                        h.setStroke(2, BlurzTheme.ACCENT_CYAN);
                        child.setBackground(h);
                    } else child.setBackground(null);
                }
                loadTeamsForLeague(isHome, league.id);
            });

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp40, dp40);
            itemParams.rightMargin = dp4;
            leagueRow.addView(leagueItem, itemParams);
        }
        if (isHome) homeLeagueRow = leagueRow; else awayLeagueRow = leagueRow;

        leagueScroll.addView(leagueRow);
        col.addView(leagueScroll);

        // ── Team Grid ──
        TextView teamLabel = new TextView(this);
        teamLabel.setText("TEAM");
        teamLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        teamLabel.setTypeface(BlurzTheme.FONT_BOLD);
        teamLabel.setTextColor(BlurzTheme.TEXT_DIM);
        teamLabel.setLetterSpacing(0.2f);
        col.addView(teamLabel);

        ScrollView teamScroll = new ScrollView(this);
        teamScroll.setVerticalScrollBarEnabled(false);
        teamScroll.setFillViewport(true);
        
        LinearLayout teamGrid = new LinearLayout(this);
        teamGrid.setOrientation(LinearLayout.VERTICAL);
        teamScroll.addView(teamGrid);

        if (isHome) homeTeamGrid = teamGrid; else awayTeamGrid = teamGrid;

        col.addView(teamScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return col;
    }

    private void loadTeamsForLeague(boolean isHome, int leagueId) {
        LinearLayout grid = isHome ? homeTeamGrid : awayTeamGrid;
        grid.removeAllViews();

        int dp8  = BlurzTheme.dpToPx(this, 8);
        int dp12 = BlurzTheme.dpToPx(this, 12);
        int dp36 = BlurzTheme.dpToPx(this, 36);

        List<DatabaseHelper.Team> teams = dbHelper.getTeams(leagueId);
        
        android.util.Log.d("TeamSelect", "Loaded " + teams.size() + " teams for league " + leagueId);
        if (teams.isEmpty()) {
            android.widget.Toast.makeText(this, "No teams found for league!", android.widget.Toast.LENGTH_SHORT).show();
        }

        for (DatabaseHelper.Team team : teams) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp8, dp8, dp8, dp8);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(Color.parseColor("#15ffffff"));
            rowBg.setCornerRadius(12);
            row.setBackground(rowBg);

            // Team logo
            ImageView logo = new ImageView(this);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            AssetImageLoader.load(this, logo, team.logoUrl);
            LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp36, dp36);
            logoParams.rightMargin = dp12;
            row.addView(logo, logoParams);

            // Team name
            TextView name = new TextView(this);
            name.setText(team.name);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setTypeface(BlurzTheme.FONT_MEDIUM);
            name.setTextColor(BlurzTheme.TEXT_PRIMARY);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Click handler
            row.setOnClickListener(v -> {
                if (isHome) {
                    selectedHomeTeamId = team.id;
                    AssetImageLoader.load(this, homeSelectedLogo, team.logoUrl);
                    homeSelectedName.setText(team.name);
                    homeSelectedName.setTextColor(BlurzTheme.TEXT_PRIMARY);
                } else {
                    selectedAwayTeamId = team.id;
                    AssetImageLoader.load(this, awaySelectedLogo, team.logoUrl);
                    awaySelectedName.setText(team.name);
                    awaySelectedName.setTextColor(BlurzTheme.TEXT_PRIMARY);
                }
                updateNextButton();

                // Highlight selected
                LinearLayout parent = isHome ? homeTeamGrid : awayTeamGrid;
                for (int j = 0; j < parent.getChildCount(); j++) {
                    View child = parent.getChildAt(j);
                    GradientDrawable bg = new GradientDrawable();
                    if (child == v) {
                        bg.setColor(Color.parseColor("#3300E5FF"));
                        bg.setStroke(1, BlurzTheme.ACCENT_CYAN);
                    } else {
                        bg.setColor(Color.parseColor("#15ffffff"));
                    }
                    bg.setCornerRadius(12);
                    child.setBackground(bg);
                }
            });

            // Touch feedback
            row.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(60).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return false;
            });

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = BlurzTheme.dpToPx(this, 6);
            grid.addView(row, rowParams);
        }
    }

    private void updateNextButton() {
        if (selectedHomeTeamId > 0 && selectedAwayTeamId > 0) {
            nextButton.setAlpha(1f);
        } else {
            nextButton.setAlpha(0.4f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
        if (instance == this) instance = null;
    }
}

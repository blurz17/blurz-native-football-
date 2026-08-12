// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_LEAGUE_STANDINGS
#define _HPP_MENU_LEAGUE_STANDINGS

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/page.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/image.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/slider.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/caption.hpp"
#include "engine/ui/widgets/capturekey.hpp"

using namespace blunted;

class LeagueStandingsPage : public Gui2Page {

  public:
    LeagueStandingsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsPage();

  protected:

};

class LeagueStandingsLeaguePage : public Gui2Page {

  public:
    LeagueStandingsLeaguePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsLeaguePage();

  protected:

};

class LeagueStandingsLeagueTablePage : public Gui2Page {

  public:
    LeagueStandingsLeagueTablePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsLeagueTablePage();

  protected:

};

class LeagueStandingsLeagueStatsPage : public Gui2Page {

  public:
    LeagueStandingsLeagueStatsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsLeagueStatsPage();

  protected:

};

class LeagueStandingsNCupPage : public Gui2Page {

  public:
    LeagueStandingsNCupPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsNCupPage();

  protected:

};

class LeagueStandingsNCupTreePage : public Gui2Page {

  public:
    LeagueStandingsNCupTreePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsNCupTreePage();

  protected:

};

class LeagueStandingsNCupStatsPage : public Gui2Page {

  public:
    LeagueStandingsNCupStatsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsNCupStatsPage();

  protected:

};

class LeagueStandingsICup1Page : public Gui2Page {

  public:
    LeagueStandingsICup1Page(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup1Page();

  protected:

};

class LeagueStandingsICup1GroupTablePage : public Gui2Page {

  public:
    LeagueStandingsICup1GroupTablePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup1GroupTablePage();

  protected:

};

class LeagueStandingsICup1TreePage : public Gui2Page {

  public:
    LeagueStandingsICup1TreePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup1TreePage();

  protected:

};

class LeagueStandingsICup1StatsPage : public Gui2Page {

  public:
    LeagueStandingsICup1StatsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup1StatsPage();

  protected:

};

class LeagueStandingsICup2Page : public Gui2Page {

  public:
    LeagueStandingsICup2Page(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup2Page();

  protected:

};

class LeagueStandingsICup2GroupTablePage : public Gui2Page {

  public:
    LeagueStandingsICup2GroupTablePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup2GroupTablePage();

  protected:

};

class LeagueStandingsICup2TreePage : public Gui2Page {

  public:
    LeagueStandingsICup2TreePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup2TreePage();

  protected:

};

class LeagueStandingsICup2StatsPage : public Gui2Page {

  public:
    LeagueStandingsICup2StatsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueStandingsICup2StatsPage();

  protected:

};

#endif

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main

SDL_PATH := ../SDL
SDL_IMAGE_PATH := ../SDL2_image
SDL_TTF_PATH := ../SDL2_ttf
SDL_GFX_PATH := ../SDL2_gfx

LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/.. $(LOCAL_PATH)/fake_boost $(SDL_PATH)/include $(SDL_IMAGE_PATH) $(LOCAL_PATH)/../gl4es/include $(SDL_TTF_PATH) $(SDL_GFX_PATH) $(LOCAL_PATH)/../ode/include $(LOCAL_PATH)/../sqlite/sqlite-amalgamation-3450300
LOCAL_CPPFLAGS += -std=c++17 -Wno-error=deprecated-declarations

# Retrieve all cpp files
LOCAL_SRC_FILES := base/geometry/aabb.cpp \
base/geometry/line.cpp \
base/geometry/plane.cpp \
base/geometry/triangle.cpp \
base/geometry/trianglemeshutils.cpp \
base/math/bluntmath.cpp \
base/math/matrix3.cpp \
base/math/matrix4.cpp \
base/math/quaternion.cpp \
base/math/vector3.cpp \
base/exception.cpp \
base/log.cpp \
base/properties.cpp \
base/sdl_surface.cpp \
base/utils.cpp \
data/matchdata.cpp \
data/playerdata.cpp \
data/teamdata.cpp \
framework/scheduler.cpp \
framework/tasksequence.cpp \
framework/workerthread.cpp \
hid/gamepad.cpp \
hid/keyboard.cpp \
league/leaguecode.cpp \
loaders/aseloader.cpp \
loaders/imageloader.cpp \
loaders/wavloader.cpp \
managers/environmentmanager.cpp \
managers/resourcemanagerpool.cpp \
managers/scenemanager.cpp \
managers/systemmanager.cpp \
managers/taskmanager.cpp \
managers/usereventmanager.cpp \
menu/ingame/gameover.cpp \
menu/ingame/gamepage.cpp \
menu/ingame/ingame.cpp \
menu/ingame/phasemenu.cpp \
menu/ingame/playerhud.cpp \
menu/ingame/radar.cpp \
menu/ingame/replaymenu.cpp \
menu/ingame/scoreboard.cpp \
menu/ingame/tacticsdebug.cpp \
menu/league/league.cpp \
menu/league/league_calendar.cpp \
menu/league/league_forward.cpp \
menu/league/league_inbox.cpp \
menu/league/league_management.cpp \
menu/league/league_standings.cpp \
menu/league/league_system.cpp \
menu/league/league_team.cpp \
menu/startmatch/loadingmatch.cpp \
menu/startmatch/matchoptions.cpp \
menu/startmatch/teamselect.cpp \
menu/widgets/gameplansubmenu.cpp \
menu/widgets/planmap.cpp \
menu/cameramenu.cpp \
menu/controllerselect.cpp \
menu/credits.cpp \
menu/gameplan.cpp \
menu/mainmenu.cpp \
menu/menuscene.cpp \
menu/menutask.cpp \
menu/pagefactory.cpp \
menu/settings.cpp \
menu/visualoptions.cpp \
misc/hungarian.c \
misc/perlin.cpp \
onthepitch/AIsupport/AIfunctions.cpp \
onthepitch/AIsupport/mentalimage.cpp \
onthepitch/player/controller/strategies/offtheball/default_def.cpp \
onthepitch/player/controller/strategies/offtheball/default_mid.cpp \
onthepitch/player/controller/strategies/offtheball/default_off.cpp \
onthepitch/player/controller/strategies/offtheball/goalie_default.cpp \
onthepitch/player/controller/strategies/special/celebration.cpp \
onthepitch/player/controller/strategies/strategy.cpp \
onthepitch/player/controller/elizacontroller.cpp \
onthepitch/player/controller/humancontroller.cpp \
onthepitch/player/controller/icontroller.cpp \
onthepitch/player/controller/playercontroller.cpp \
onthepitch/player/controller/refereecontroller.cpp \
onthepitch/player/humanoid/animcollection.cpp \
onthepitch/player/humanoid/humanoid.cpp \
onthepitch/player/humanoid/humanoidbase.cpp \
onthepitch/player/humanoid/humanoid_utils.cpp \
onthepitch/player/player.cpp \
onthepitch/player/playerbase.cpp \
onthepitch/player/playerofficial.cpp \
onthepitch/ball.cpp \
onthepitch/humangamer.cpp \
onthepitch/match.cpp \
onthepitch/officials.cpp \
onthepitch/proceduralpitch.cpp \
onthepitch/referee.cpp \
onthepitch/team.cpp \
onthepitch/teamAIcontroller.cpp \
scene/objects/audioreceiver.cpp \
scene/objects/camera.cpp \
scene/objects/geometry.cpp \
scene/objects/image2d.cpp \
scene/objects/joint.cpp \
scene/objects/light.cpp \
scene/objects/skybox.cpp \
scene/objects/sound.cpp \
scene/resources/geometrydata.cpp \
scene/resources/soundbuffer.cpp \
scene/resources/surface.cpp \
scene/scene2d/scene2d.cpp \
scene/scene3d/node.cpp \
scene/scene3d/scene3d.cpp \
scene/object.cpp \
scene/objectfactory.cpp \
scene/scene.cpp \
systems/audio/objects/audio_audioreceiver.cpp \
systems/audio/objects/audio_sound.cpp \
systems/audio/rendering/audio_messages.cpp \
systems/audio/rendering/openal_renderer.cpp \
systems/audio/resources/audio_soundbuffer.cpp \
systems/audio/audio_object.cpp \
systems/audio/audio_scene.cpp \
systems/audio/audio_system.cpp \
systems/graphics/objects/graphics_camera.cpp \
systems/graphics/objects/graphics_geometry.cpp \
systems/graphics/objects/graphics_light.cpp \
systems/graphics/objects/graphics_overlay2d.cpp \
systems/graphics/rendering/opengl_renderer3d.cpp \
systems/graphics/rendering/r3d_messages.cpp \
systems/graphics/resources/texture.cpp \
systems/graphics/resources/vertexbuffer.cpp \
systems/graphics/graphics_object.cpp \
systems/graphics/graphics_scene.cpp \
systems/graphics/graphics_system.cpp \
systems/graphics/graphics_task.cpp \
systems/physics/objects/physics_geometry.cpp \
systems/physics/objects/physics_joint.cpp \
systems/physics/wrappers/ode_physics.cpp \
systems/physics/physics_object.cpp \
systems/physics/physics_scene.cpp \
systems/physics/physics_system.cpp \
systems/physics/physics_task.cpp \
systems/isystemtask.cpp \
types/command.cpp \
types/iusertask.cpp \
types/observer.cpp \
types/refcounted.cpp \
types/spatial.cpp \
types/trianglemesh.cpp \
utils/animationextensions/footballanimationextension.cpp \
utils/gui2/widgets/button.cpp \
utils/gui2/widgets/caption.cpp \
utils/gui2/widgets/capturekey.cpp \
utils/gui2/widgets/dialog.cpp \
utils/gui2/widgets/editline.cpp \
utils/gui2/widgets/filebrowser.cpp \
utils/gui2/widgets/frame.cpp \
utils/gui2/widgets/grid.cpp \
utils/gui2/widgets/iconselector.cpp \
utils/gui2/widgets/image.cpp \
utils/gui2/widgets/menu.cpp \
utils/gui2/widgets/pulldown.cpp \
utils/gui2/widgets/root.cpp \
utils/gui2/widgets/scrollbar.cpp \
utils/gui2/widgets/slider.cpp \
utils/gui2/widgets/text.cpp \
utils/gui2/events.cpp \
utils/gui2/guitask.cpp \
utils/gui2/page.cpp \
utils/gui2/style.cpp \
utils/gui2/view.cpp \
utils/gui2/windowmanager.cpp \
utils/animation.cpp \
utils/console.cpp \
utils/database.cpp \
utils/directoryparser.cpp \
utils/objectloader.cpp \
utils/orbitcamera.cpp \
utils/splitgeometry.cpp \
utils/text2d.cpp \
utils/threadhud.cpp \
utils/xmlloader.cpp \
blunted.cpp \
dbquery.cpp \
gamedefines.cpp \
gametask.cpp \
main.cpp \
utils.cpp

LOCAL_SHARED_LIBRARIES := SDL2 SDL2_image SDL2_ttf SDL2_gfx
LOCAL_STATIC_LIBRARIES := GL ode sqlite3

LOCAL_LDLIBS := -lGLESv3 -lGLESv2 -lEGL -llog -landroid

# C++11 required
LOCAL_CPPFLAGS += -std=c++11 -DGPFB_ANDROID -frtti -fexceptions

include $(BUILD_SHARED_LIBRARY)

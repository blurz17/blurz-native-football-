#include <gtest/gtest.h>
#include "engine/framework/managers/environmentmanager.hpp"
#include <thread>
#include <chrono>

using namespace blunted;

TEST(EnvironmentManagerTest, PausingFreezesTime) {
    // The EnvironmentManager is a singleton, grab instance
    EnvironmentManager& env = EnvironmentManager::GetInstance();
    
    unsigned long start_time = env.GetTime_ms();
    
    // Pause the engine
    env.SetPaused(true);
    
    // Wait real-time (e.g. 250ms) to simulate backgrounding
    std::this_thread::sleep_for(std::chrono::milliseconds(250));
    
    // Resume the engine
    env.SetPaused(false);
    
    unsigned long end_time = env.GetTime_ms();
    unsigned long diff = end_time - start_time;
    
    // Because the engine was paused, the internal game clock should NOT have 
    // advanced by the 250ms we waited. It should be close to 0ms (well under 50ms).
    EXPECT_LT(diff, 50) << "Engine time skipped by " << diff << "ms while paused!";
}

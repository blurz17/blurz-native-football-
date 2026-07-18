#pragma once
#include <random>

namespace boost {
  // A very basic random wrapper for headless mode.
  typedef std::mt19937 mt19937;
  
  template<typename Engine, typename Dist>
  class variate_generator {
  public:
    variate_generator(Engine& rng, Dist dist) : rng_(rng), dist_(dist) {}
    typename Dist::result_type operator()() {
      return dist_(rng_);
    }
    Engine& engine() { return rng_; }
  private:
    Engine& rng_;
    Dist dist_;
  };

  template<typename RealType = double>
  class uniform_real {
  public:
    typedef RealType result_type;
    uniform_real(RealType min = 0.0, RealType max = 1.0) : dist_(min, max) {}
    template<typename Engine>
    result_type operator()(Engine& eng) {
      return dist_(eng);
    }
  private:
    std::uniform_real_distribution<RealType> dist_;
  };

  template<typename IntType = int>
  class uniform_int {
  public:
    typedef IntType result_type;
    uniform_int(IntType min = 0, IntType max = 9) : dist_(min, max) {}
    template<typename Engine>
    result_type operator()(Engine& eng) {
      return dist_(eng);
    }
  private:
    std::uniform_int_distribution<IntType> dist_;
  };
}

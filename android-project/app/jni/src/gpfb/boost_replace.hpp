#pragma once

#include <memory>
#include <mutex>
#include <condition_variable>
#include <thread>
#include <functional>
#include <string>
#include <vector>
#include <deque>
#include <atomic>
#include <stdexcept>
#include <iostream>
#include <sstream>
#include <chrono>
#include <cctype>
#include <system_error>
#include "ghc/filesystem.hpp"
namespace boost {
    namespace filesystem = ghc::filesystem;
    using std::shared_ptr;
    using std::weak_ptr;
    using std::make_shared;
    using std::bind;
    using std::function;
    using std::thread;
    using std::condition_variable;
    using std::condition_variable;
    using condition = std::condition_variable;
    using std::unique_lock;
    
    namespace system {
        using error_code = std::error_code;
    }
    using std::lock_guard;
    using std::static_pointer_cast;
    using std::dynamic_pointer_cast;
    using std::ref;

    class mutex : public std::mutex {
    public:
        using scoped_lock = std::unique_lock<std::mutex>;
    };

    using timed_mutex = std::timed_mutex;

    inline std::string to_upper_copy(std::string s) {
        for (char& c : s) c = std::toupper((unsigned char)c);
        return s;
    }



    template<class T> class intrusive_ptr {
    private:
        T* px;
    public:
        intrusive_ptr() : px(nullptr) {}
        intrusive_ptr(T* p, bool add_ref = true) : px(p) { if(px != nullptr && add_ref) intrusive_ptr_add_ref(px); }
        template<class U> intrusive_ptr(U* p, bool add_ref = true) : px(p) { if(px != nullptr && add_ref) intrusive_ptr_add_ref(px); }
        intrusive_ptr(const intrusive_ptr& rhs) : px(rhs.px) { if(px != nullptr) intrusive_ptr_add_ref(px); }
        template<class U> intrusive_ptr(const intrusive_ptr<U>& rhs) : px(rhs.get()) { if(px != nullptr) intrusive_ptr_add_ref(px); }
        ~intrusive_ptr() { if(px != nullptr) intrusive_ptr_release(px); }
        intrusive_ptr& operator=(const intrusive_ptr& rhs) {
            intrusive_ptr(rhs).swap(*this);
            return *this;
        }
        template<class U> intrusive_ptr& operator=(const intrusive_ptr<U>& rhs) {
            intrusive_ptr(rhs).swap(*this);
            return *this;
        }
        intrusive_ptr& operator=(T* rhs) {
            intrusive_ptr(rhs).swap(*this);
            return *this;
        }
        template<class U> intrusive_ptr& operator=(U* rhs) {
            intrusive_ptr(rhs).swap(*this);
            return *this;
        }
        void reset() {
            intrusive_ptr().swap(*this);
        }
        void reset(T* rhs) {
            intrusive_ptr(rhs).swap(*this);
        }
        void swap(intrusive_ptr& rhs) {
            T* tmp = px;
            px = rhs.px;
            rhs.px = tmp;
        }
        T* get() const { return px; }
        T& operator*() const { return *px; }
        T* operator->() const { return px; }
        operator bool() const { return px != nullptr; }
        template<class U> bool operator==(const intrusive_ptr<U>& rhs) const { return px == rhs.get(); }
        template<class U> bool operator!=(const intrusive_ptr<U>& rhs) const { return px != rhs.get(); }
        bool operator==(T* rhs) const { return px == rhs; }
        bool operator!=(T* rhs) const { return px != rhs; }
    };

    template<class T, class U> bool operator==(T* a, const intrusive_ptr<U>& b) { return a == b.get(); }
    template<class T, class U> bool operator!=(T* a, const intrusive_ptr<U>& b) { return a != b.get(); }

    template<class T, class U> intrusive_ptr<T> static_pointer_cast(const intrusive_ptr<U>& p) {
        return static_cast<T*>(p.get());
    }
    template<class T, class U> intrusive_ptr<T> dynamic_pointer_cast(const intrusive_ptr<U>& p) {
        return dynamic_cast<T*>(p.get());
    }

    namespace placeholders {
        using namespace std::placeholders;
    }

    namespace detail {
        using atomic_count = std::atomic<long>;
    }

    template<typename T>
    class circular_buffer {
    public:
        using iterator = typename std::deque<T>::iterator;
        using const_iterator = typename std::deque<T>::const_iterator;
        using reverse_iterator = typename std::deque<T>::reverse_iterator;
        using const_reverse_iterator = typename std::deque<T>::const_reverse_iterator;

        circular_buffer() : capacity_(0) {}
        circular_buffer(size_t capacity) : capacity_(capacity) {}
        void push_back(const T& val) {
            if (capacity_ == 0) return;
            if (q_.size() >= capacity_) q_.pop_front();
            q_.push_back(val);
        }
        size_t size() const { return q_.size(); }
        bool empty() const { return q_.empty(); }
        T& operator[](size_t index) { return q_[index]; }
        const T& operator[](size_t index) const { return q_[index]; }
        void clear() { q_.clear(); }
        T front() { return q_.front(); }
        void pop_front() { q_.pop_front(); }
        void set_capacity(size_t new_capacity) {
            capacity_ = new_capacity;
            while(q_.size() > capacity_) q_.pop_front();
        }
        iterator begin() { return q_.begin(); }
        iterator end() { return q_.end(); }
        const_iterator begin() const { return q_.begin(); }
        const_iterator end() const { return q_.end(); }
        reverse_iterator rbegin() { return q_.rbegin(); }
        reverse_iterator rend() { return q_.rend(); }
        const_reverse_iterator rbegin() const { return q_.rbegin(); }
        const_reverse_iterator rend() const { return q_.rend(); }
    private:
        size_t capacity_;
        std::deque<T> q_;
    };

    namespace signals2 {
        class connection {
        public:
            void disconnect() {}
        };

        class trackable {
        protected:
            trackable() = default;
            ~trackable() = default;
            trackable(const trackable&) = default;
            trackable& operator=(const trackable&) = default;
        };

        template <typename Signature>
        class signal;
        
        template <typename Ret, typename... Args>
        class signal<Ret(Args...)> {
        public:
            using slot_type = std::function<Ret(Args...)>;
            connection connect(slot_type slot) { 
                slots_.push_back(slot); 
                return connection(); 
            }

            template <typename T>
            void disconnect(T slot) {
                // stub for now
            }

            void operator()(Args... args) {
                for (auto& slot : slots_) {
                    slot(args...);
                }
            }
        private:
            std::vector<slot_type> slots_;
        };
    }

    namespace posix_time {
        using ptime = std::chrono::steady_clock::time_point;
        class time_duration {
        public:
            time_duration(std::chrono::milliseconds ms) : ms_(ms) {}
            template<typename Rep, typename Period>
            time_duration(const std::chrono::duration<Rep, Period>& d) : ms_(std::chrono::duration_cast<std::chrono::milliseconds>(d)) {}
            long total_milliseconds() const { return ms_.count(); }
        private:
            std::chrono::milliseconds ms_;
        };

        class microsec_clock {
        public:
            static ptime local_time() { return std::chrono::steady_clock::now(); }
        };

        inline time_duration milliseconds(long ms) { return time_duration(std::chrono::milliseconds(ms)); }
    }

    namespace this_thread {
        inline void sleep(const posix_time::time_duration& td) {
            std::this_thread::sleep_for(std::chrono::milliseconds(td.total_milliseconds()));
        }
    }

    template<typename T, typename U>
    T lexical_cast(const U& val) {
        T res;
        std::stringstream ss;
        ss << val;
        ss >> res;
        return res;
    }
}

using namespace std::placeholders;

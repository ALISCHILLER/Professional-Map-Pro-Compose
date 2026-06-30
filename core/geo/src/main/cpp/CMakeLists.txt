cmake_minimum_required(VERSION 3.22.1)
project(geokit LANGUAGES CXX)

add_library(geokit SHARED geokit.cpp)

target_compile_features(geokit PRIVATE cxx_std_17)
target_link_libraries(geokit android log)

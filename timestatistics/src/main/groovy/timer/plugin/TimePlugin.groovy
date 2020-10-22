
package timer.plugin

import org.gradle.api.Plugin

public class TimePlugin<Project> implements Plugin {
    static def add(def x, def y) {
        return x + y
    }

    @Override
    void apply(Object o) {
        println("========begin=========")
        println(add(1,2))
        println("========end=========")
    }
}
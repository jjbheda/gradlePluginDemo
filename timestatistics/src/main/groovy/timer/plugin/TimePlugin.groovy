
package timer.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class TimePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println ("自定义插件加载成功")
        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class)
        appExtension.registerTransform(new TimePluginTransform())

    }
}
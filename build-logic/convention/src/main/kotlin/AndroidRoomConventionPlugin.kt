import androidx.room.gradle.RoomExtension
import com.callbackdev.tetto.buildlogic.library
import com.callbackdev.tetto.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room with KSP. Schemas are exported to `<module>/schemas` and committed (PLANNING.md §5), so
 * every auto-migration is generated from a schema that is in the repo's history.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.library("androidx-room-runtime"))
                add("ksp", libs.library("androidx-room-compiler"))
                add("testImplementation", libs.library("androidx-room-testing"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-junit"))
            }
        }
    }
}

/*
 * settings.gradle.kts — declaración raíz del monorepo Gradle.
 *
 * Notas de diseño:
 *  - Se usa "FAIL_ON_PROJECT_REPOS" para forzar que todos los módulos
 *    usen los repositorios declarados aquí (evita repositorios duplicados
 *    o conflictivos en submódulos).
 *  - Se incluyen 3 módulos Kotlin: :shared, :composeApp y :backend.
 *  - Los proyectos no-Kotlin (web-admin, ai-service, iosApp, infrastructure)
 *    NO se incluyen como subproyectos Gradle: tienen sus propios sistemas
 *    de build (npm, pip, Xcode, Docker).
 */

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

/*
 * foojay-resolver-convention:
 * Permite que Gradle descargue automáticamente la JDK 17 si la máquina
 * de desarrollo sólo tiene una JDK más vieja.  Necesario en este monorepo
 * porque KMP 2.0.21 + Compose 1.7 + AGP 8.7 requieren JVM target 17.
 */
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    /*
     * PREFER_SETTINGS (en vez de FAIL_ON_PROJECT_REPOS):
     * El plugin de Kotlin para wasmJs/js agrega internamente repositorios
     * para descargar Node y Yarn (nodejs.org/dist, github.com/yarnpkg).
     * Esos repos se inyectan fuera de settings.gradle, por lo que
     * FAIL_ON_PROJECT_REPOS aborta el sync con "added by unknown code".
     * PREFER_SETTINGS mantiene la intención (usar los repos centrales primero)
     * y permite que el plugin agregue los suyos cuando el target wasmJs lo exige.
     */
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        // El plugin de Kotlin para wasmJs/js descarga Node y Yarn de los siguientes
        // mirrors.  Sin estos `exclusiveContent` blocks, FAIL_ON_PROJECT_REPOS aborta
        // el sync de gradle al detectar que el plugin agrega esos repos "por fuera".
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist/") {
                    name = "Node Distributions"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }
        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
    }
}

rootProject.name = "parking-system"

include(":shared")
include(":composeApp")
include(":backend")

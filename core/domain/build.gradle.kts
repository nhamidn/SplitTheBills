plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    test {
        resources.srcDir("../../shared")
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnit()
}

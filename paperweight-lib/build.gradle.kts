plugins {
    `config-kotlin`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.httpclient)
    implementation(libs.kotson)

    // ASM for inspection
    implementation(libs.bundles.asm)

    implementation(libs.bundles.hypo)
    implementation(libs.slf4j.jdk14) // slf4j impl for hypo
    implementation(libs.bundles.cadix)

    implementation(libs.lorenzTiny)

    implementation(libs.jbsdiff)
    compileOnly(libs.gradle.shadow)
}

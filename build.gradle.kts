buildscript {

    repositories {
        mavenCentral()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", "1.1-M04"))
    }
}

apply {
    plugin("kotlin")
    plugin("application")
}

configure<ApplicationPluginConvention> {
    mainClassName = "helloTriangleKt"
}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {

    compile(kotlinModule("stdlib", "1.1-M04"))

    compile("com.github.elect86:glm:191cdfeb20")
    compile("com.github.elect86:unofficial-opengl-SDK:c0775d1a5e")

    val jogl = "2.3.2"

    compile("org.jogamp.gluegen:gluegen-rt:$jogl")
    compile("org.jogamp.jogl:jogl-all:$jogl")

    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-android-aarch64")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-android-armv6")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-linux-amd64")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-linux-armv6")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-linux-armv6hf")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-linux-i586")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-macosx-universal")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-solaris-amd64")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-solaris-i586")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-windows-amd64")
    runtime("org.jogamp.gluegen:gluegen-rt:$jogl:natives-windows-i586")

    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-android-aarch64")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-android-armv6")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-linux-amd64")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-linux-armv6")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-linux-armv6hf")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-linux-i586")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-macosx-universal")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-solaris-amd64")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-solaris-i586")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-windows-amd64")
    runtime("org.jogamp.jogl:jogl-all:$jogl:natives-windows-i586")
}

allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}
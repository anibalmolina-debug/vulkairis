plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	flatDir {
		dir("LIBS")
	}
	mavenCentral()
}




loom {
	splitEnvironmentSourceSets()

	mods {
		register("vulkairis") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

val lwjglVersion = "3.3.3"

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	// Iris & VulkanMod & Sodium dev dependencies.
	// Iris hard-depends on sodium 0.8.x and touches sodium API classes at runtime
	// (e.g. VertexSerializer in SystemReport mixin), so sodium-api must be on the
	// dev classpath even though the real sodium renderer is NOT loaded (Vulkairis
	// declares "provides sodium" + VulkanMod replaces rendering).
	modImplementation(":iris-fabric-1.8.14-beta.1+mc1.21.1")
	modImplementation(":VulkanMod-0.6.7+1.21.1")
	modCompileOnly(":sodium-fabric-0.8.12-beta.1+mc1.21.1")
	implementation(":sodium-api")
	// ModMenu for the Config button (Mods -> Vulkairis -> config). No Cloth Config:
	// settings UI is vanilla (VulkairisConfigScreen) per GUI plan.
	modImplementation(":modmenu-11.0.4")

	// Iris bundled nested jar-in-jar dependencies needed on the Loom dev classpath.
	// Without them, shader parsing/transformation throws NoClassDefFoundError.
	implementation(":jcpp-1.4.14")
	implementation(":glsl-transformer-3.0.0-pre3")
	implementation(":antlr4-runtime-4.13.1")

	// in-process GLSL -> SPIR-V via Shaderc.
	// Only the Windows natives are on the runtime classpath: the extra Linux/macOS
	// natives make the dev classpath long enough that Gradle replaces it with a
	// "pathing jar", which breaks Fabric's dev launcher (see gradlew.bat comment).
	implementation("org.lwjgl:lwjgl-shaderc:$lwjglVersion")
	runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:natives-windows")

	// Vulkan LWJGL bindings & VMA
	implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")

	implementation("org.lwjgl:lwjgl-vma:$lwjglVersion")
	runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:natives-windows")

	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
	// The Gradle JUnit test worker cannot start in this environment: the Gradle cache lives
	// under a non-ASCII user profile path, so the worker JVM fails to load GradleWorkerMain.
	// The standalone `runTestRunner` task below avoids the Gradle test worker entirely and is
	// wired into `check`, so `gradlew build` still runs the prototype test suite.
	enabled = false
}

tasks.register<JavaExec>("runTestRunner") {
	group = "verification"
	description = "Runs the Vulkairis prototype test suite"
	classpath = sourceSets.test.get().runtimeClasspath + sourceSets.getByName("client").runtimeClasspath
	mainClass.set("com.vulkairis.test.VulkairisTestRunner")
	// Opt in to parallel execution so the JUnit platform does not warn on every run.
	systemProperty("junit.jupiter.execution.parallel.enabled", "true")
}


tasks.named("check") {
	dependsOn("runTestRunner")
}

tasks.matching { it.name == "downloadAssets" }.configureEach {
	enabled = false
}

tasks.matching { it.name.startsWith("runClient") }.configureEach {
	doFirst {
		val loomCache = file(".gradle/loom-cache")
		if (loomCache.exists()) {
			loomCache.walk().filter { it.isFile && (it.extension == "cfg" || it.extension == "txt") }.forEach { f ->
				var text = f.readText(Charsets.UTF_8)
				if (text.contains("Molina")) {
					text = text.replace(Regex("""C:[\\/]Users[\\/][^\r\n;\\/]+?[\\/]\.gradle"""), """C:\\gradle-ascii""")
					logger.lifecycle("[Vulkairis] Auto-sanitized non-ASCII user path in ${f.name}")
				}
				if (f.name == "remapClasspath.txt") {
					val entries = text.split(";").filter { !it.contains("fabric-loader") }
					text = entries.joinToString(";")
					logger.lifecycle("[Vulkairis] Stripped fabric-loader from remapClasspath.txt (${entries.size} entries remain)")
				}
				f.writeText(text, Charsets.UTF_8)
			}
		}
	}
}





tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	// withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for more information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}

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

	// Iris & VulkanMod & Sodium dev dependencies
	modImplementation(files("LIBS/iris-fabric-${providers.gradleProperty("iris").get()}.jar"))
	modImplementation(files("LIBS/VulkanMod-${providers.gradleProperty("vulkanmod").get()}.jar"))
	modCompileOnly(files("LIBS/sodium-fabric-0.8.12-beta.1+mc1.21.1.jar"))
	implementation(files("LIBS/sodium-api.jar"))

	// in-process GLSL -> SPIR-V via Shaderc
	implementation("org.lwjgl:lwjgl-shaderc:$lwjglVersion")
	runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:natives-windows")
	runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:natives-linux")
	runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:natives-macos")

	// Vulkan LWJGL bindings & VMA
	implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")

	implementation("org.lwjgl:lwjgl-vma:$lwjglVersion")
	runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:natives-windows")
	runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:natives-linux")
	runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:natives-macos")

	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
	// enabled = false
}

tasks.register<JavaExec>("runTestRunner") {
	group = "verification"
	description = "Runs the Vulkairis prototype test suite"
	classpath = sourceSets.test.get().runtimeClasspath + sourceSets.getByName("client").runtimeClasspath
	mainClass.set("com.vulkairis.test.VulkairisTestRunner")
}

tasks.matching { it.name == "downloadAssets" }.configureEach {
	enabled = false
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
	withSourcesJar()

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

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}

package com.example.domain

enum class ProjectStack(
    val displayName: String,
    val shortCode: String,
    val supportsApk: Boolean,
    val supportsIpa: Boolean,
    val summary: String,
) {
    FLUTTER(
        displayName = "Flutter",
        shortCode = "FLUTTER",
        supportsApk = true,
        supportsIpa = true,
        summary = "Cross-platform Dart/Flutter project. Builds Android APK on Ubuntu and iOS IPA on macOS.",
    ),
    REACT_NATIVE(
        displayName = "React Native / Expo",
        shortCode = "RN",
        supportsApk = true,
        supportsIpa = true,
        summary = "JavaScript/TypeScript mobile project. Builds Android Gradle APK and macOS Xcode IPA.",
    ),
    KOTLIN_MULTIPLATFORM(
        displayName = "Kotlin Multiplatform",
        shortCode = "KMP",
        supportsApk = true,
        supportsIpa = true,
        summary = "Shared Kotlin project with Android Gradle and iOS Xcode targets.",
    ),
    ANDROID_NATIVE(
        displayName = "Android Native",
        shortCode = "ANDROID",
        supportsApk = true,
        supportsIpa = false,
        summary = "Native Android Gradle project. Compiles signed or debug APK packages.",
    ),
    IOS_NATIVE(
        displayName = "iOS Native",
        shortCode = "IOS",
        supportsApk = false,
        supportsIpa = true,
        summary = "Native Xcode / Swift project. Archives and packages IPA on macOS runners.",
    ),
    CUSTOM_MOBILE(
        displayName = "Custom / Hybrid",
        shortCode = "CUSTOM",
        supportsApk = true,
        supportsIpa = true,
        summary = "Generic repository layout. Generates configurable APK and IPA workflow steps.",
    ),
}

data class StackDetectionResult(
    val stack: ProjectStack,
    val detectedIndicators: List<String>,
    val hasAndroidFolder: Boolean,
    val hasIosFolder: Boolean,
    val hasExistingWorkflows: Boolean,
    val isExpoProject: Boolean = false,
    val rootEntries: List<String> = emptyList(),
)

object StackDetector {
    fun detect(
        rootFiles: List<String>,
        packageJsonContent: String? = null,
        repoLanguage: String? = null,
    ): StackDetectionResult {
        val lowerSet = rootFiles.map { it.lowercase() }.toSet()
        val indicators = mutableListOf<String>()

        val hasPubspec = "pubspec.yaml" in lowerSet
        val hasPackageJson = "package.json" in lowerSet
        val hasAppJson = "app.json" in lowerSet || "app.config.js" in lowerSet || "app.config.ts" in lowerSet
        val hasGradle = "build.gradle" in lowerSet || "build.gradle.kts" in lowerSet || "settings.gradle" in lowerSet || "settings.gradle.kts" in lowerSet
        val hasGradlew = "gradlew" in lowerSet
        val hasAndroidFolder = "android" in lowerSet || "app" in lowerSet || "composeapp" in lowerSet || "androidapp" in lowerSet
        val hasIosFolder = "ios" in lowerSet || "iosapp" in lowerSet
        val hasXcodeProj = rootFiles.any { it.endsWith(".xcodeproj", ignoreCase = true) || it.endsWith(".xcworkspace", ignoreCase = true) }
        val hasPodfile = "podfile" in lowerSet
        val hasPackageSwift = "package.swift" in lowerSet
        val hasWorkflowDir = ".github" in lowerSet

        val isExpo = (packageJsonContent?.contains("\"expo\"", ignoreCase = true) == true) ||
            (hasAppJson && packageJsonContent?.contains("react-native", ignoreCase = true) == true)
        val isReactNative = packageJsonContent?.contains("\"react-native\"", ignoreCase = true) == true || isExpo

        val stack = when {
            hasPubspec -> {
                indicators += "pubspec.yaml"
                if ("android" in lowerSet) indicators += "android/"
                if ("ios" in lowerSet) indicators += "ios/"
                ProjectStack.FLUTTER
            }
            isReactNative || (hasPackageJson && (hasAndroidFolder || hasIosFolder)) -> {
                indicators += "package.json"
                if (isExpo) indicators += "Expo SDK"
                if ("android" in lowerSet) indicators += "android/"
                if ("ios" in lowerSet) indicators += "ios/"
                ProjectStack.REACT_NATIVE
            }
            hasGradle && ("iosapp" in lowerSet || "composeapp" in lowerSet || "shared" in lowerSet) -> {
                if ("composeapp" in lowerSet) indicators += "composeApp/"
                if ("shared" in lowerSet) indicators += "shared/"
                if ("iosapp" in lowerSet) indicators += "iosApp/"
                if (hasGradlew) indicators += "gradlew"
                ProjectStack.KOTLIN_MULTIPLATFORM
            }
            hasGradle || hasGradlew -> {
                if ("build.gradle.kts" in lowerSet) indicators += "build.gradle.kts"
                else if ("build.gradle" in lowerSet) indicators += "build.gradle"
                if (hasGradlew) indicators += "gradlew"
                if ("app" in lowerSet) indicators += "app/"
                ProjectStack.ANDROID_NATIVE
            }
            hasXcodeProj || hasPodfile || hasPackageSwift || repoLanguage.equals("Swift", ignoreCase = true) -> {
                rootFiles.firstOrNull {
                    it.endsWith(".xcodeproj", ignoreCase = true) || it.endsWith(".xcworkspace", ignoreCase = true)
                }?.let { indicators += it }
                if (hasPodfile) indicators += "Podfile"
                if (hasPackageSwift) indicators += "Package.swift"
                ProjectStack.IOS_NATIVE
            }
            else -> {
                if (hasPackageJson) indicators += "package.json"
                if (!repoLanguage.isNullOrBlank()) indicators += repoLanguage
                ProjectStack.CUSTOM_MOBILE
            }
        }

        return StackDetectionResult(
            stack = stack,
            detectedIndicators = indicators.distinct(),
            hasAndroidFolder = hasAndroidFolder,
            hasIosFolder = hasIosFolder,
            hasExistingWorkflows = hasWorkflowDir,
            isExpoProject = isExpo,
            rootEntries = rootFiles.sorted(),
        )
    }
}

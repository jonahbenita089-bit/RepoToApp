package com.example

import com.example.domain.BuildMode
import com.example.domain.IosPackagingMode
import com.example.domain.ProjectStack
import com.example.domain.RepoInputParser
import com.example.domain.StackDetector
import com.example.domain.WorkflowConfig
import com.example.domain.WorkflowGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoForgeDomainTest {

    @Test
    fun parseGitHubUrlsAndSlugs() {
        assertEquals("flutter/samples", RepoInputParser.parse("flutter/samples")?.fullName)
        assertEquals("flutter/samples", RepoInputParser.parse("https://github.com/flutter/samples")?.fullName)
        assertEquals("flutter/samples", RepoInputParser.parse("https://github.com/flutter/samples.git/")?.fullName)
        assertEquals("flutter/samples", RepoInputParser.parse("git@github.com:flutter/samples.git")?.fullName)
        assertNull(RepoInputParser.parse("invalid"))
        assertNull(RepoInputParser.parse(""))
    }

    @Test
    fun detectProjectStacksCorrectly() {
        val flutter = StackDetector.detect(listOf("pubspec.yaml", "android", "ios", "lib"))
        assertEquals(ProjectStack.FLUTTER, flutter.stack)
        assertTrue(flutter.stack.supportsApk)
        assertTrue(flutter.stack.supportsIpa)

        val rn = StackDetector.detect(
            rootFiles = listOf("package.json", "android", "ios"),
            packageJsonContent = """{"dependencies": {"react-native": "0.76.0", "expo": "52.0.0"}}""",
        )
        assertEquals(ProjectStack.REACT_NATIVE, rn.stack)
        assertTrue(rn.isExpoProject)

        val android = StackDetector.detect(listOf("build.gradle.kts", "settings.gradle.kts", "gradlew", "app"))
        assertEquals(ProjectStack.ANDROID_NATIVE, android.stack)
        assertTrue(android.stack.supportsApk)
        assertFalse(android.stack.supportsIpa)

        val ios = StackDetector.detect(listOf("MyApp.xcodeproj", "Podfile"))
        assertEquals(ProjectStack.IOS_NATIVE, ios.stack)
        assertFalse(ios.stack.supportsApk)
        assertTrue(ios.stack.supportsIpa)
    }

    @Test
    fun generateFlutterApkAndUnsignedIpaWorkflow() {
        val yaml = WorkflowGenerator.generate(
            WorkflowConfig(
                stack = ProjectStack.FLUTTER,
                branch = "main",
                buildApk = true,
                buildIpa = true,
                buildMode = BuildMode.RELEASE,
                iosPackagingMode = IosPackagingMode.UNSIGNED_IPA,
                attachToGitHubRelease = true,
            ),
        )
        assertNotNull(yaml)
        assertTrue(yaml.contains("build_apk:"))
        assertTrue(yaml.contains("build_ipa:"))
        assertTrue(yaml.contains("flutter build apk"))
        assertTrue(yaml.contains("flutter build ios --release --no-codesign"))
        assertTrue(yaml.contains("app-unsigned.ipa"))
        assertTrue(yaml.contains("publish_release:"))
        assertTrue(yaml.contains("needs.build_apk.result == 'success' || needs.build_ipa.result == 'success'"))
    }
}

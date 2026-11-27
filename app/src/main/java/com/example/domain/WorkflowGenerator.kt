package com.example.domain

enum class BuildMode(val label: String, val flagValue: String) {
    RELEASE("Release", "release"),
    DEBUG("Debug", "debug"),
}

enum class IosPackagingMode(val label: String, val description: String) {
    UNSIGNED_IPA(
        "Unsigned IPA (No Apple Cert Required)",
        "Builds Runner/App via xcodebuild with CODE_SIGNING_ALLOWED=NO and packages Payload/*.app into a downloadable .ipa archive.",
    ),
    SIGNED_IPA(
        "Signed IPA (Uses Repo Signing Secrets)",
        "Uses APPLE_CERTIFICATE_BASE64, APPLE_CERT_PASSWORD, and PROVISIONING_PROFILE_BASE64 secrets for App Store / Ad-Hoc signing.",
    ),
}

data class WorkflowConfig(
    val stack: ProjectStack,
    val branch: String = "main",
    val buildApk: Boolean = true,
    val buildIpa: Boolean = true,
    val buildMode: BuildMode = BuildMode.RELEASE,
    val iosPackagingMode: IosPackagingMode = IosPackagingMode.UNSIGNED_IPA,
    val javaVersion: String = "17",
    val flutterVersion: String = "3.27.1",
    val nodeVersion: String = "20",
    val isExpoProject: Boolean = false,
    val attachToGitHubRelease: Boolean = false,
    val workflowFileName: String = "repo2app-build.yml",
)

object WorkflowGenerator {

    fun generate(config: WorkflowConfig): String {
        val sb = StringBuilder()
        sb.appendLine("name: RepoForge APK & IPA Build")
        sb.appendLine()
        sb.appendLine("on:")
        sb.appendLine("  workflow_dispatch:")
        sb.appendLine("    inputs:")
        sb.appendLine("      build_apk:")
        sb.appendLine("        description: 'Build Android APK'")
        sb.appendLine("        type: boolean")
        sb.appendLine("        default: ${config.buildApk}")
        sb.appendLine("      build_ipa:")
        sb.appendLine("        description: 'Build iOS IPA'")
        sb.appendLine("        type: boolean")
        sb.appendLine("        default: ${config.buildIpa}")
        sb.appendLine("      build_mode:")
        sb.appendLine("        description: 'Build variant (release or debug)'")
        sb.appendLine("        type: choice")
        sb.appendLine("        default: '${config.buildMode.flagValue}'")
        sb.appendLine("        options:")
        sb.appendLine("          - release")
        sb.appendLine("          - debug")
        sb.appendLine("  push:")
        sb.appendLine("    branches: [ '${config.branch}' ]")
        sb.appendLine()
        sb.appendLine("permissions:")
        sb.appendLine("  contents: write")
        sb.appendLine()
        sb.appendLine("jobs:")

        val neededJobs = mutableListOf<String>()

        if (config.buildApk) {
            neededJobs += "build_apk"
            appendApkJob(sb, config)
        }

        if (config.buildIpa) {
            neededJobs += "build_ipa"
            appendIpaJob(sb, config)
        }

        if (config.attachToGitHubRelease && neededJobs.isNotEmpty()) {
            appendReleaseJob(sb, neededJobs)
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun appendApkJob(sb: StringBuilder, config: WorkflowConfig) {
        sb.appendLine("  build_apk:")
        sb.appendLine("    name: Build Android APK")
        sb.appendLine("    if: \${{ github.event_name != 'workflow_dispatch' || inputs.build_apk }}")
        sb.appendLine("    runs-on: ubuntu-latest")
        sb.appendLine("    steps:")
        sb.appendLine("      - name: Checkout repository")
        sb.appendLine("        uses: actions/checkout@v4")
        sb.appendLine()
        sb.appendLine("      - name: Set up JDK ${config.javaVersion}")
        sb.appendLine("        uses: actions/setup-java@v4")
        sb.appendLine("        with:")
        sb.appendLine("          distribution: 'temurin'")
        sb.appendLine("          java-version: '${config.javaVersion}'")
        sb.appendLine()

        when (config.stack) {
            ProjectStack.FLUTTER -> {
                sb.appendLine("      - name: Set up Flutter")
                sb.appendLine("        uses: subosito/flutter-action@v2")
                sb.appendLine("        with:")
                sb.appendLine("          flutter-version: '${config.flutterVersion}'")
                sb.appendLine("          channel: 'stable'")
                sb.appendLine("          cache: true")
                sb.appendLine()
                sb.appendLine("      - name: Fetch Dart dependencies")
                sb.appendLine("        run: flutter pub get")
                sb.appendLine()
                sb.appendLine("      - name: Build Android APK")
                sb.appendLine("        run: |")
                sb.appendLine("          MODE=\"\${{ inputs.build_mode || '${config.buildMode.flagValue}' }}\"")
                sb.appendLine("          if [ \"\$MODE\" = \"debug\" ]; then")
                sb.appendLine("            flutter build apk --debug")
                sb.appendLine("          else")
                sb.appendLine("            flutter build apk --release")
                sb.appendLine("          fi")
                sb.appendLine()
                sb.appendLine("      - name: Collect APK artifacts")
                sb.appendLine("        run: |")
                sb.appendLine("          mkdir -p dist/apk")
                sb.appendLine("          find build/app/outputs -name '*.apk' -exec cp {} dist/apk/ \\;")
                sb.appendLine("          ls -lh dist/apk")
            }

            ProjectStack.REACT_NATIVE -> {
                sb.appendLine("      - name: Set up Node.js ${config.nodeVersion}")
                sb.appendLine("        uses: actions/setup-node@v4")
                sb.appendLine("        with:")
                sb.appendLine("          node-version: '${config.nodeVersion}'")
                sb.appendLine("          cache: 'npm'")
                sb.appendLine()
                sb.appendLine("      - name: Install JavaScript dependencies")
                sb.appendLine("        run: npm ci || npm install")
                if (config.isExpoProject) {
                    sb.appendLine()
                    sb.appendLine("      - name: Generate native Android project (Expo)")
                    sb.appendLine("        run: |")
                    sb.appendLine("          if [ ! -d \"android\" ]; then")
                    sb.appendLine("            npx expo prebuild --platform android --non-interactive")
                    sb.appendLine("          fi")
                }
                sb.appendLine()
                sb.appendLine("      - name: Build Android APK with Gradle")
                sb.appendLine("        run: |")
                sb.appendLine("          cd android")
                sb.appendLine("          chmod +x gradlew")
                sb.appendLine("          MODE=\"\${{ inputs.build_mode || '${config.buildMode.flagValue}' }}\"")
                sb.appendLine("          if [ \"\$MODE\" = \"debug\" ]; then")
                sb.appendLine("            ./gradlew assembleDebug --no-daemon")
                sb.appendLine("          else")
                sb.appendLine("            ./gradlew assembleRelease --no-daemon")
                sb.appendLine("          fi")
                sb.appendLine()
                sb.appendLine("      - name: Collect APK artifacts")
                sb.appendLine("        run: |")
                sb.appendLine("          mkdir -p dist/apk")
                sb.appendLine("          find android -path '*/build/outputs/apk/*.apk' -exec cp {} dist/apk/ \\;")
                sb.appendLine("          ls -lh dist/apk")
            }

            ProjectStack.KOTLIN_MULTIPLATFORM -> {
                sb.appendLine("      - name: Setup Gradle")
                sb.appendLine("        uses: gradle/actions/setup-gradle@v4")
                sb.appendLine()
                sb.appendLine("      - name: Build Android APK")
                sb.appendLine("        run: |")
                sb.appendLine("          chmod +x gradlew")
                sb.appendLine("          MODE=\"\${{ inputs.build_mode || '${config.buildMode.flagValue}' }}\"")
                sb.appendLine("          if [ \"\$MODE\" = \"debug\" ]; then")
                sb.appendLine("            ./gradlew assembleDebug --no-daemon")
                sb.appendLine("          else")
                sb.appendLine("            ./gradlew assembleRelease --no-daemon")
                sb.appendLine("          fi")
                sb.appendLine()
                sb.appendLine("      - name: Collect APK artifacts")
                sb.appendLine("        run: |")
                sb.appendLine("          mkdir -p dist/apk")
                sb.appendLine("          find . -path '*/build/outputs/apk/*.apk' -exec cp {} dist/apk/ \\;")
                sb.appendLine("          ls -lh dist/apk")
            }

            ProjectStack.ANDROID_NATIVE, ProjectStack.IOS_NATIVE, ProjectStack.CUSTOM_MOBILE -> {
                sb.appendLine("      - name: Setup Gradle")
                sb.appendLine("        uses: gradle/actions/setup-gradle@v4")
                sb.appendLine()
                sb.appendLine("      - name: Build Android APK")
                sb.appendLine("        run: |")
                sb.appendLine("          if [ -f \"gradlew\" ]; then")
                sb.appendLine("            chmod +x gradlew")
                sb.appendLine("            GRADLE_CMD=\"./gradlew\"")
                sb.appendLine("          elif [ -f \"android/gradlew\" ]; then")
                sb.appendLine("            cd android && chmod +x gradlew")
                sb.appendLine("            GRADLE_CMD=\"./gradlew\"")
                sb.appendLine("          else")
                sb.appendLine("            GRADLE_CMD=\"gradle\"")
                sb.appendLine("          fi")
                sb.appendLine("          MODE=\"\${{ inputs.build_mode || '${config.buildMode.flagValue}' }}\"")
                sb.appendLine("          if [ \"\$MODE\" = \"debug\" ]; then")
                sb.appendLine("            \$GRADLE_CMD assembleDebug --no-daemon")
                sb.appendLine("          else")
                sb.appendLine("            \$GRADLE_CMD assembleRelease --no-daemon || \$GRADLE_CMD assembleDebug --no-daemon")
                sb.appendLine("          fi")
                sb.appendLine()
                sb.appendLine("      - name: Collect APK artifacts")
                sb.appendLine("        run: |")
                sb.appendLine("          mkdir -p dist/apk")
                sb.appendLine("          find . -path '*/build/outputs/apk/*.apk' -exec cp {} dist/apk/ \\;")
                sb.appendLine("          ls -lh dist/apk")
            }
        }

        sb.appendLine()
        sb.appendLine("      - name: Upload Android APK artifact")
        sb.appendLine("        uses: actions/upload-artifact@v4")
        sb.appendLine("        with:")
        sb.appendLine("          name: android-apk")
        sb.appendLine("          path: dist/apk/*.apk")
        sb.appendLine("          if-no-files-found: error")
        sb.appendLine()
    }

    private fun appendIpaJob(sb: StringBuilder, config: WorkflowConfig) {
        sb.appendLine("  build_ipa:")
        sb.appendLine("    name: Build iOS IPA")
        sb.appendLine("    if: \${{ github.event_name != 'workflow_dispatch' || inputs.build_ipa }}")
        sb.appendLine("    runs-on: macos-latest")
        sb.appendLine("    steps:")
        sb.appendLine("      - name: Checkout repository")
        sb.appendLine("        uses: actions/checkout@v4")
        sb.appendLine()

        if (config.iosPackagingMode == IosPackagingMode.SIGNED_IPA) {
            sb.appendLine("      - name: Install Apple Signing Certificate & Provisioning Profile")
            sb.appendLine("        env:")
            sb.appendLine("          P12_BASE64: \${{ secrets.APPLE_CERTIFICATE_BASE64 }}")
            sb.appendLine("          P12_PASSWORD: \${{ secrets.APPLE_CERT_PASSWORD }}")
            sb.appendLine("          PROFILE_BASE64: \${{ secrets.PROVISIONING_PROFILE_BASE64 }}")
            sb.appendLine("        run: |")
            sb.appendLine("          KEYCHAIN_PATH=\$RUNNER_TEMP/build.keychain-db")
            sb.appendLine("          security create-keychain -p \"temp-pass\" \"\$KEYCHAIN_PATH\"")
            sb.appendLine("          security set-keychain-settings -lut 21600 \"\$KEYCHAIN_PATH\"")
            sb.appendLine("          security unlock-keychain -p \"temp-pass\" \"\$KEYCHAIN_PATH\"")
            sb.appendLine("          echo \"\$P12_BASE64\" | base64 --decode > \$RUNNER_TEMP/cert.p12")
            sb.appendLine("          security import \$RUNNER_TEMP/cert.p12 -P \"\$P12_PASSWORD\" -A -t cert -f pkcs12 -k \"\$KEYCHAIN_PATH\"")
            sb.appendLine("          security list-keychain -d user -s \"\$KEYCHAIN_PATH\"")
            sb.appendLine("          mkdir -p ~/Library/MobileDevice/Provisioning\\ Profiles")
            sb.appendLine("          echo \"\$PROFILE_BASE64\" | base64 --decode > ~/Library/MobileDevice/Provisioning\\ Profiles/build.mobileprovision")
            sb.appendLine()
        }

        when (config.stack) {
            ProjectStack.FLUTTER -> {
                sb.appendLine("      - name: Set up Flutter")
                sb.appendLine("        uses: subosito/flutter-action@v2")
                sb.appendLine("        with:")
                sb.appendLine("          flutter-version: '${config.flutterVersion}'")
                sb.appendLine("          channel: 'stable'")
                sb.appendLine("          cache: true")
                sb.appendLine()
                sb.appendLine("      - name: Fetch Dart dependencies")
                sb.appendLine("        run: flutter pub get")
                sb.appendLine()
                if (config.iosPackagingMode == IosPackagingMode.UNSIGNED_IPA) {
                    sb.appendLine("      - name: Build unsigned iOS app bundle")
                    sb.appendLine("        run: |")
                    sb.appendLine("          flutter build ios --release --no-codesign")
                    sb.appendLine("          mkdir -p dist/ipa/Payload")
                    sb.appendLine("          cp -R build/ios/iphoneos/Runner.app dist/ipa/Payload/")
                    sb.appendLine("          cd dist/ipa && zip -r -y app-unsigned.ipa Payload")
                    sb.appendLine("          rm -rf dist/ipa/Payload")
                } else {
                    sb.appendLine("      - name: Build signed IPA")
                    sb.appendLine("        run: |")
                    sb.appendLine("          flutter build ipa --release")
                    sb.appendLine("          mkdir -p dist/ipa")
                    sb.appendLine("          find build/ios/ipa -name '*.ipa' -exec cp {} dist/ipa/ \\;")
                }
            }

            ProjectStack.REACT_NATIVE -> {
                sb.appendLine("      - name: Set up Node.js ${config.nodeVersion}")
                sb.appendLine("        uses: actions/setup-node@v4")
                sb.appendLine("        with:")
                sb.appendLine("          node-version: '${config.nodeVersion}'")
                sb.appendLine("          cache: 'npm'")
                sb.appendLine()
                sb.appendLine("      - name: Install JS and CocoaPods dependencies")
                sb.appendLine("        run: |")
                sb.appendLine("          npm ci || npm install")
                if (config.isExpoProject) {
                    sb.appendLine("          if [ ! -d \"ios\" ]; then npx expo prebuild --platform ios --non-interactive; fi")
                }
                sb.appendLine("          if [ -f \"ios/Podfile\" ]; then cd ios && pod install; fi")
                sb.appendLine()
                appendGenericXcodeBuildSteps(sb, iosDir = "ios", isUnsigned = config.iosPackagingMode == IosPackagingMode.UNSIGNED_IPA)
            }

            ProjectStack.KOTLIN_MULTIPLATFORM -> {
                sb.appendLine("      - name: Set up JDK ${config.javaVersion}")
                sb.appendLine("        uses: actions/setup-java@v4")
                sb.appendLine("        with:")
                sb.appendLine("          distribution: 'temurin'")
                sb.appendLine("          java-version: '${config.javaVersion}'")
                sb.appendLine()
                appendGenericXcodeBuildSteps(sb, iosDir = "iosApp", isUnsigned = config.iosPackagingMode == IosPackagingMode.UNSIGNED_IPA)
            }

            ProjectStack.IOS_NATIVE, ProjectStack.ANDROID_NATIVE, ProjectStack.CUSTOM_MOBILE -> {
                sb.appendLine("      - name: Install CocoaPods if Podfile exists")
                sb.appendLine("        run: |")
                sb.appendLine("          if [ -f \"Podfile\" ]; then pod install; fi")
                sb.appendLine("          if [ -f \"ios/Podfile\" ]; then cd ios && pod install; fi")
                sb.appendLine()
                appendGenericXcodeBuildSteps(sb, iosDir = ".", isUnsigned = config.iosPackagingMode == IosPackagingMode.UNSIGNED_IPA)
            }
        }

        sb.appendLine()
        sb.appendLine("      - name: Upload iOS IPA artifact")
        sb.appendLine("        uses: actions/upload-artifact@v4")
        sb.appendLine("        with:")
        sb.appendLine("          name: ios-ipa")
        sb.appendLine("          path: dist/ipa/*.ipa")
        sb.appendLine("          if-no-files-found: error")
        sb.appendLine()
    }

    private fun appendGenericXcodeBuildSteps(sb: StringBuilder, iosDir: String, isUnsigned: Boolean) {
        sb.appendLine("      - name: Build and package iOS IPA")
        sb.appendLine("        run: |")
        sb.appendLine("          SEARCH_DIR=\"$iosDir\"")
        sb.appendLine("          if [ ! -d \"\$SEARCH_DIR\" ]; then SEARCH_DIR=\".\"; fi")
        sb.appendLine("          WORKSPACE=\$(find \"\$SEARCH_DIR\" -maxdepth 2 -name '*.xcworkspace' ! -path '*/Pods/*' | head -n 1)")
        sb.appendLine("          PROJECT=\$(find \"\$SEARCH_DIR\" -maxdepth 2 -name '*.xcodeproj' | head -n 1)")
        sb.appendLine("          if [ -n \"\$WORKSPACE\" ]; then")
        sb.appendLine("            CONTAINER_ARGS=\"-workspace \$WORKSPACE\"")
        sb.appendLine("            SCHEME=\$(xcodebuild -list -workspace \"\$WORKSPACE\" | awk '/Schemes:/{flag=1;next} flag && NF{print \$1; exit}')")
        sb.appendLine("          else")
        sb.appendLine("            CONTAINER_ARGS=\"-project \$PROJECT\"")
        sb.appendLine("            SCHEME=\$(xcodebuild -list -project \"\$PROJECT\" | awk '/Schemes:/{flag=1;next} flag && NF{print \$1; exit}')")
        sb.appendLine("          fi")
        if (isUnsigned) {
            sb.appendLine("          xcodebuild \$CONTAINER_ARGS -scheme \"\$SCHEME\" -configuration Release -sdk iphoneos -derivedDataPath build/iosDerived CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=\"\" build")
            sb.appendLine("          APP_PATH=\$(find build/iosDerived/Build/Products/Release-iphoneos -name '*.app' -maxdepth 2 | head -n 1)")
            sb.appendLine("          mkdir -p dist/ipa/Payload")
            sb.appendLine("          cp -R \"\$APP_PATH\" dist/ipa/Payload/")
            sb.appendLine("          cd dist/ipa && zip -r -y app-unsigned.ipa Payload")
            sb.appendLine("          rm -rf dist/ipa/Payload")
        } else {
            sb.appendLine("          xcodebuild \$CONTAINER_ARGS -scheme \"\$SCHEME\" -configuration Release -sdk iphoneos -archivePath build/App.xcarchive archive")
            sb.appendLine("          mkdir -p dist/ipa/Payload")
            sb.appendLine("          APP_PATH=\$(find build/App.xcarchive/Products/Applications -name '*.app' -maxdepth 1 | head -n 1)")
            sb.appendLine("          cp -R \"\$APP_PATH\" dist/ipa/Payload/")
            sb.appendLine("          cd dist/ipa && zip -r -y app-signed.ipa Payload")
            sb.appendLine("          rm -rf dist/ipa/Payload")
        }
    }

    private fun appendReleaseJob(sb: StringBuilder, neededJobs: List<String>) {
        val anySuccessExpr = neededJobs.joinToString(" || ") { "needs.$it.result == 'success'" }
        sb.appendLine("  publish_release:")
        sb.appendLine("    name: Publish GitHub Release")
        sb.appendLine("    needs: [${neededJobs.joinToString(", ")}]")
        sb.appendLine("    if: always() && ($anySuccessExpr)")
        sb.appendLine("    runs-on: ubuntu-latest")
        sb.appendLine("    steps:")
        sb.appendLine("      - name: Download build artifacts")
        sb.appendLine("        uses: actions/download-artifact@v4")
        sb.appendLine("        with:")
        sb.appendLine("          path: release-files")
        sb.appendLine()
        sb.appendLine("      - name: Create GitHub Release with APK & IPA")
        sb.appendLine("        uses: softprops/action-gh-release@v2")
        sb.appendLine("        with:")
        sb.appendLine("          tag_name: build-\${{ github.run_number }}")
        sb.appendLine("          name: RepoForge Build #\${{ github.run_number }}")
        sb.appendLine("          files: |")
        sb.appendLine("            release-files/**/*.apk")
        sb.appendLine("            release-files/**/*.ipa")
    }
}

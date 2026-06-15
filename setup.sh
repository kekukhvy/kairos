#!/bin/bash

echo "📁 Creating project structure..."

# Modules
for module in kairos-api kairos-engine kairos-worker kairos-admin kairos-sdk common; do
  mkdir -p $module/src/main/java/dev/kairos
  mkdir -p $module/src/test/java/dev/kairos
  touch $module/src/main/java/dev/kairos/.gitkeep
  touch $module/src/test/java/dev/kairos/.gitkeep
  echo "description = 'TODO'" > $module/build.gradle
done

# Adapters
for adapter in kafka sqs webhook rabbitmq; do
  mkdir -p kairos-adapters/$adapter/src/main/java/dev/kairos/adapter/$adapter
  mkdir -p kairos-adapters/$adapter/src/test/java/dev/kairos/adapter/$adapter
  touch kairos-adapters/$adapter/src/main/java/dev/kairos/adapter/$adapter/.gitkeep
  touch kairos-adapters/$adapter/src/test/java/dev/kairos/adapter/$adapter/.gitkeep
  echo "description = 'TODO'" > kairos-adapters/$adapter/build.gradle
done

# Resources
mkdir -p kairos-api/src/main/resources/db/migration

# settings.gradle
cat > settings.gradle << 'EOF'
rootProject.name = 'kairos'

include 'common'
include 'kairos-api'
include 'kairos-engine'
include 'kairos-worker'
include 'kairos-adapters:kafka'
include 'kairos-adapters:sqs'
include 'kairos-adapters:webhook'
include 'kairos-adapters:rabbitmq'
include 'kairos-admin'
include 'kairos-sdk'
EOF

# root build.gradle
cat > build.gradle << 'EOF'
plugins {
    id 'java'
}

group = 'dev.kairos'
version = '0.1.0-SNAPSHOT'

subprojects {
    apply plugin: 'java'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(26)
        }
    }

    repositories {
        mavenCentral()
    }
}
EOF

echo "🎉 Done!"
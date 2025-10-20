#!/usr/bin/env bash
set -euo pipefail

# Builds Name History without mutating gradle.properties
# Usage: ./build.sh -v <mod-version> [-m <mc-version> ...] [--all] [-- <extra gradle args>]
# Requires: Gradle wrapper, Java 21, internet access.

usage() {
  echo "Usage: $0 -v <mod-version> [-m <mc-version> ...] [--all] [-- <extra gradle args>]" >&2
  exit 1
}

supported_versions=(
  "1.21.1"
  "1.21.2"
  "1.21.3"
  "1.21.4"
  "1.21.5"
  "1.21.6"
  "1.21.7"
  "1.21.8"
  "1.21.9"
  "1.21.10"
)

declare -A yarn=(
  ["1.21.1"]="1.21.1+build.3"
  ["1.21.2"]="1.21.2+build.1"
  ["1.21.3"]="1.21.3+build.2"
  ["1.21.4"]="1.21.4+build.8"
  ["1.21.5"]="1.21.5+build.1"
  ["1.21.6"]="1.21.6+build.1"
  ["1.21.7"]="1.21.7+build.8"
  ["1.21.8"]="1.21.8+build.1"
  ["1.21.9"]="1.21.9+build.1"
  ["1.21.10"]="1.21.10+build.2"
)

declare -A fabric_api=(
  ["1.21.1"]="0.116.7+1.21.1"
  ["1.21.2"]="0.106.1+1.21.2"
  ["1.21.3"]="0.114.1+1.21.3"
  ["1.21.4"]="0.119.4+1.21.4"
  ["1.21.5"]="0.128.2+1.21.5"
  ["1.21.6"]="0.128.2+1.21.6"
  ["1.21.7"]="0.129.0+1.21.7"
  ["1.21.8"]="0.136.0+1.21.8"
  ["1.21.9"]="0.134.0+1.21.9"
  ["1.21.10"]="0.135.0+1.21.10"
)

mod_version=""
declare -a minecraft_versions=()
extra_args=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      mod_version="${2:-}" || true
      if [[ -z "$mod_version" ]]; then usage; fi
      shift 2
      ;;
    -m|--minecraft)
      mc_version="${2:-}" || true
      if [[ -z "$mc_version" ]]; then usage; fi
      minecraft_versions+=("$mc_version")
      shift 2
      ;;
    --all)
      minecraft_versions=("${supported_versions[@]}")
      shift
      ;;
    --)
      shift
      extra_args=("$@")
      break
      ;;
    *)
      usage
      ;;
  esac
done

if [[ -z "$mod_version" ]]; then
  usage
fi

if [[ ${#minecraft_versions[@]} -eq 0 ]]; then
  minecraft_versions=("1.21.10")
fi

loader_version=$(grep '^loader_version=' gradle.properties | cut -d'=' -f2)

for version in "${minecraft_versions[@]}"; do
  echo "==> Building for Minecraft ${version}"
  yarn_version=${yarn[$version]}
  fabric_version=${fabric_api[$version]}

  if [[ -z ${yarn_version:-} || -z ${fabric_version:-} ]]; then
    echo "Missing mapping or Fabric API version for ${version}" >&2
    exit 1
  fi

  rm -f "build/libs/namehistory-${mod_version}-${version}-fabric.jar"

  ./gradlew clean build \
    -Pminecraft_version="${version}" \
    -Pyarn_mappings="${yarn_version}" \
    -Pfabric_version="${fabric_version}" \
    -Ploader_version="${loader_version}" \
    -Pmod_version="${mod_version}" \
    "${extra_args[@]}"

  original_jar=$(find build/libs -maxdepth 1 -type f -name "name-history-${mod_version}.jar" -print -quit)
  if [[ -z "$original_jar" ]]; then
    original_jar=$(find build/libs -maxdepth 1 -type f -name "name-history-*.jar" ! -name "*-sources.jar" -print -quit)
  fi

  if [[ -z "$original_jar" ]]; then
    echo "Failed to locate compiled jar in build/libs" >&2
    exit 1
  fi

  target_jar="build/libs/namehistory-${mod_version}-${version}-fabric.jar"
  mv "$original_jar" "$target_jar"
  echo "Produced $target_jar"

done

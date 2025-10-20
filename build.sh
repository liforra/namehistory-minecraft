#!/usr/bin/env bash
set -euo pipefail

# Builds Name History for multiple Minecraft patches without mutating gradle.properties
# Usage: ./build.sh -v <mod-version> [-- <extra gradle args>]
# Requires: Gradle wrapper, Java 21, internet access.

usage() {
  echo "Usage: $0 -v <mod-version> [-- <extra gradle args>]" >&2
  exit 1
}

mod_version=""
extra_args=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      mod_version="${2:-}" || true
      if [[ -z "$mod_version" ]]; then usage; fi
      shift 2
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

versions=(
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

loader_version=$(grep '^loader_version=' gradle.properties | cut -d'=' -f2)

for version in "${versions[@]}"; do
  echo "==> Building for Minecraft ${version}"
  yarn_version=${yarn[$version]}
  fabric_version=${fabric_api[$version]}

  if [[ -z ${yarn_version:-} || -z ${fabric_version:-} ]]; then
    echo "Missing mapping or Fabric API version for ${version}" >&2
    exit 1
  fi

  ./gradlew clean build \
    -Pminecraft_version="${version}" \
    -Pyarn_mappings="${yarn_version}" \
    -Pfabric_version="${fabric_version}" \
    -Ploader_version="${loader_version}" \
    -Pmod_version="${mod_version}" \
    "${extra_args[@]}"

  original_jar=$(ls build/libs/name-history-*.jar | head -n 1)
  target_jar="build/libs/namehistory-${mod_version}-${version}-fabric.jar"
  mv "$original_jar" "$target_jar"
  echo "Produced $target_jar"

done

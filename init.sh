#!/usr/bin/env bash
set -euo pipefail

# Run from the repo root. Base package: dev.kairos.admin
BASE="kairos-admin/src/main/java/dev/kairos/admin"
RES="kairos-admin/src/main/resources"

# --- directories ---
dirs=(
  "$BASE/shared/layout"
  "$BASE/shared/style"
  "$BASE/shared/client"
  "$BASE/feature/task/dto"
  "$BASE/feature/task/component"
  "$RES"
)

for d in "${dirs[@]}"; do
  mkdir -p "$d"
  echo "  dir  $d"
done

# --- java files ---
files=(
  "$BASE/KairosAdminApplication.java"
  "$BASE/shared/layout/MainLayout.java"
  "$BASE/shared/style/StyleConfig.java"
  "$BASE/shared/style/Tokens.java"
  "$BASE/shared/client/KairosApiClient.java"
  "$BASE/shared/client/ApiProperties.java"
  "$BASE/feature/task/TaskView.java"
  "$BASE/feature/task/TaskService.java"
  "$BASE/feature/task/dto/TaskDto.java"
  "$BASE/feature/task/dto/CreateTaskRequest.java"
  "$BASE/feature/task/dto/ScheduleDto.java"
  "$BASE/feature/task/dto/TaskType.java"
  "$BASE/feature/task/component/TaskGrid.java"
  "$BASE/feature/task/component/TaskForm.java"
)

for f in "${files[@]}"; do
  [ -e "$f" ] || touch "$f"
  echo "  file $f"
done

# --- resources ---
[ -e "$RES/application.properties" ] || touch "$RES/application.properties"
echo "  file $RES/application.properties"

echo "Done."
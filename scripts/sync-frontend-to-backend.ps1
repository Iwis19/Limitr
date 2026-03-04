param(
  [string]$FrontendDist = "frontend/dist/limitr-frontend/browser",
  [string]$BackendStatic = "backend/src/main/resources/static"
)

if (!(Test-Path $FrontendDist)) {
  Write-Error "Frontend dist path not found: $FrontendDist"
  exit 1
}

if (!(Test-Path $BackendStatic)) {
  New-Item -ItemType Directory -Force -Path $BackendStatic | Out-Null
}

Get-ChildItem -Path $BackendStatic -Force | Remove-Item -Force -Recurse
Copy-Item -Path "$FrontendDist\*" -Destination $BackendStatic -Recurse -Force

Write-Host "Copied frontend build from '$FrontendDist' to '$BackendStatic'."

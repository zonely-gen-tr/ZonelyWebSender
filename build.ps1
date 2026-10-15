$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$lib = Join-Path $project '.build/lib'
$classes = Join-Path $project '.build/classes'
function Assert-BuildPath([string] $target) {
    $basePath = [IO.Path]::GetFullPath($project).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $resolvedPath = [IO.Path]::GetFullPath($target)
    if (-not $resolvedPath.StartsWith($basePath, [StringComparison]::OrdinalIgnoreCase)) { throw 'Derleme hedefi proje disinda' }
}
New-Item -ItemType Directory -Force $lib | Out-Null
Assert-BuildPath $classes
if (Test-Path $classes) { Remove-Item -Recurse -Force -LiteralPath $classes }
New-Item -ItemType Directory -Force $classes | Out-Null

$spigot = Join-Path $lib 'spigot-api-1.8.8.jar'
$gson = Join-Path $lib 'gson-2.8.9.jar'
if (-not (Test-Path $spigot)) {
    & curl.exe -fL --max-time 45 -o $spigot 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/spigot-api-1.8.8-R0.1-20160221.082514-43.jar'
    if ($LASTEXITCODE -ne 0) { throw 'Spigot API indirilemedi' }
}
if (-not (Test-Path $gson)) {
    & curl.exe -fL --max-time 45 -o $gson 'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar'
    if ($LASTEXITCODE -ne 0) { throw 'Gson indirilemedi' }
}

$root = Join-Path $project 'src/main/java/tc/yigit'
$sources = @()
$sources += (Get-ChildItem (Join-Path $root 'bukkit') -Filter '*.java').FullName
$sources += (Get-ChildItem (Join-Path $root 'shared') -Filter '*.java').FullName
$sources += @(
    (Join-Path $root 'events/EventManager.java'),
    (Join-Path $root 'events/EventOutput.java'),
    (Join-Path $root 'events/BukkitSocketOnCommandEvent.java'),
    (Join-Path $root 'events/BukkitSocketOnMessageEvent.java')
)
& javac --release 8 -encoding UTF-8 -cp "$spigot;$gson" -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Java derleme basarisiz' }

# Gson Bukkit 1.8 sunucularinda bulunmayabilir, bu yuzden siniflarini jar'a ekle.
Push-Location $classes
try {
    & jar xf $gson
    if ($LASTEXITCODE -ne 0) { throw 'Gson paketleme basarisiz' }
} finally { Pop-Location }
$metaPath = Join-Path $classes 'META-INF'
Assert-BuildPath $metaPath
if (Test-Path $metaPath) { Remove-Item -Recurse -Force -LiteralPath $metaPath }
Copy-Item -Force -Recurse (Join-Path $project 'src/main/resources/*') $classes
$out = Join-Path $project 'dist'
New-Item -ItemType Directory -Force $out | Out-Null
$jarPath = Join-Path $out 'ZonelyWebSender-1.0.0-beta.7.jar'
& jar cf $jarPath -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR olusturulamadi' }


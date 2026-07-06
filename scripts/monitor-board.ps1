#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$ExpectedBranch = "master",
    [string]$Since = "24 hours ago",
    [switch]$SkipTests,
    [switch]$NoWebhook
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$script:GitSafeDirectory = $null

function Invoke-CommandCapture {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [string[]]$Arguments = @()
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $FilePath @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output | ForEach-Object { $_.ToString() })
    }
}

function Invoke-GitCapture {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $gitArguments = @()
    if (-not [string]::IsNullOrWhiteSpace($script:GitSafeDirectory)) {
        $gitArguments += @("-c", "safe.directory=$script:GitSafeDirectory")
    }

    $gitArguments += $Arguments
    Invoke-CommandCapture -FilePath "git" -Arguments $gitArguments
}

function Get-CommandText {
    param([string[]]$Lines)

    ($Lines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join "`n"
}

function Get-UniqueLines {
    param([string[]]$Lines)

    $seen = @{}
    foreach ($line in $Lines) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) {
            continue
        }

        if (-not $seen.ContainsKey($trimmed)) {
            $seen[$trimmed] = $true
            $trimmed
        }
    }
}

function Get-RecentChangedFiles {
    param([string]$SinceText)

    $files = New-Object System.Collections.Generic.List[string]

    $status = Invoke-GitCapture -Arguments @("status", "--porcelain")
    if ($status.ExitCode -eq 0) {
        foreach ($line in $status.Output) {
            if ($line.Length -lt 4) {
                continue
            }

            $path = $line.Substring(3).Trim()
            if ($path -match " -> ") {
                $path = ($path -split " -> ")[-1].Trim()
            }
            $files.Add($path)
        }
    }

    $log = Invoke-GitCapture -Arguments @("log", "--since=$SinceText", "--name-only", "--pretty=format:")
    if ($log.ExitCode -eq 0) {
        foreach ($line in $log.Output) {
            if (-not [string]::IsNullOrWhiteSpace($line)) {
                $files.Add($line.Trim())
            }
        }
    }

    $lastCommit = Invoke-GitCapture -Arguments @("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD")
    if ($lastCommit.ExitCode -eq 0) {
        foreach ($line in $lastCommit.Output) {
            if (-not [string]::IsNullOrWhiteSpace($line)) {
                $files.Add($line.Trim())
            }
        }
    }

    @(Get-UniqueLines -Lines $files)
}

function Test-AnyMatch {
    param(
        [string[]]$Files,
        [string]$Pattern
    )

    @($Files | Where-Object { $_ -match $Pattern })
}

function Get-TodoCountFromWorkingTree {
    $todo = Invoke-CommandCapture -FilePath "rg" -Arguments @("--glob", "!build/**", "--glob", "!scripts/**", "--glob", "!.gradle/**", "--glob", "!.git/**", "-n", "TODO|FIXME", ".")
    if ($todo.ExitCode -gt 1) {
        return [pscustomobject]@{ Count = 0; Error = Get-CommandText -Lines $todo.Output }
    }

    [pscustomobject]@{ Count = @($todo.Output).Count; Error = "" }
}

function Get-TodoCountFromRef {
    param([string]$Ref)

    $result = Invoke-GitCapture -Arguments @("grep", "-n", "-E", "TODO|FIXME", $Ref, "--", ".", ":(exclude)scripts", ":(exclude)build", ":(exclude).gradle")
    if ($result.ExitCode -eq 0) {
        return @($result.Output).Count
    }

    0
}

function Limit-Text {
    param(
        [string]$Text,
        [int]$MaxLength = 1500
    )

    if ($Text.Length -le $MaxLength) {
        return $Text
    }

    $Text.Substring(0, $MaxLength) + "`n... truncated ..."
}

function Format-List {
    param(
        [string[]]$Items,
        [string]$EmptyText = "없음"
    )

    if (-not $Items -or $Items.Count -eq 0) {
        return "- $EmptyText"
    }

    ($Items | ForEach-Object { "- $_" }) -join "`n"
}

function Limit-Items {
    param(
        [string[]]$Items,
        [int]$Max = 8
    )

    if (-not $Items -or $Items.Count -le $Max) {
        return @($Items)
    }

    @($Items | Select-Object -First $Max) + "... 외 $($Items.Count - $Max)개"
}

function Add-UniqueIssue {
    param(
        [System.Collections.Generic.List[string]]$Issues,
        [string]$Issue
    )

    if (-not $Issues.Contains($Issue)) {
        $Issues.Add($Issue)
    }
}

try {
    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $ProjectRoot = Join-Path $PSScriptRoot ".."
    }

    $resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
    $script:GitSafeDirectory = $resolvedProjectRoot -replace "\\", "/"
    Set-Location $resolvedProjectRoot

    $issues = New-Object System.Collections.Generic.List[string]
    $changedFiles = @(Get-RecentChangedFiles -SinceText $Since)

    $branchResult = Invoke-GitCapture -Arguments @("rev-parse", "--abbrev-ref", "HEAD")
    $currentBranch = if ($branchResult.ExitCode -eq 0 -and $branchResult.Output.Count -gt 0) { $branchResult.Output[0].Trim() } else { "unknown" }
    $branchMatches = $currentBranch -eq $ExpectedBranch
    if ($currentBranch -eq "HEAD") {
        $headResult = Invoke-GitCapture -Arguments @("rev-parse", "HEAD")
        $expectedResult = Invoke-GitCapture -Arguments @("rev-parse", $ExpectedBranch)
        if (
            $headResult.ExitCode -eq 0 -and
            $expectedResult.ExitCode -eq 0 -and
            $headResult.Output.Count -gt 0 -and
            $expectedResult.Output.Count -gt 0 -and
            $headResult.Output[0].Trim() -eq $expectedResult.Output[0].Trim()
        ) {
            $currentBranch = "detached ($ExpectedBranch)"
            $branchMatches = $true
        } else {
            $currentBranch = "detached"
        }
    }

    if (-not $branchMatches) {
        Add-UniqueIssue -Issues $issues -Issue "현재 브랜치가 '$currentBranch'입니다. 기대 브랜치는 '$ExpectedBranch'입니다."
    }

    $sensitivePatterns = @{
        "Security/OAuth" = "(^|[\\/])SecurityConfig\.java$|(^|[\\/])global[\\/]security[\\/].*\.java$|oauth|OAuth"
        "Repository query" = "(^|[\\/])repository[\\/].*Repository\.java$|Repository\.java$"
        "application properties" = "(^|[\\/])application(-.*)?\.properties$"
        "GitHub Actions workflow" = "^\.github[\\/]workflows[\\/].+\.ya?ml$"
        "deployment config" = "(^|[\\/])docker-compose.*\.ya?ml$|(^|[\\/])Dockerfile$|(^|[\\/])build\.gradle$"
    }

    $sensitiveChanges = New-Object System.Collections.Generic.List[string]
    foreach ($key in $sensitivePatterns.Keys) {
        $matches = @(Test-AnyMatch -Files $changedFiles -Pattern $sensitivePatterns[$key])
        if ($matches.Count -gt 0) {
            Add-UniqueIssue -Issues $issues -Issue "$key 관련 변경이 감지되었습니다."
            foreach ($file in $matches) {
                $sensitiveChanges.Add("$file ($key)")
            }
        }
    }

    $structureFiles = @(Test-AnyMatch -Files $changedFiles -Pattern "(^|[\\/])(repository|service|controller)[\\/].*\.java$|Repository\.java$|Service\.java$|Controller\.java$")
    if ($structureFiles.Count -gt 0) {
        Add-UniqueIssue -Issues $issues -Issue "Repository/Service/Controller 구조 검토 대상 변경이 있습니다."
    }

    $todoCurrent = Get-TodoCountFromWorkingTree
    $todoPrevious = Get-TodoCountFromRef -Ref "HEAD~1"
    $todoDelta = $todoCurrent.Count - $todoPrevious
    if ($todoDelta -gt 0) {
        Add-UniqueIssue -Issues $issues -Issue "TODO/FIXME 주석이 이전 커밋 대비 ${todoDelta}개 증가했습니다."
    }

    $gradleCommand = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { ".\gradlew" }
    $gradleArguments = if ($SkipTests) {
        @("clean", "bootJar", "-x", "test")
    } else {
        @("clean", "test", "bootJar")
    }
    $buildCommandText = "./gradlew " + ($gradleArguments -join " ")
    $build = Invoke-CommandCapture -FilePath $gradleCommand -Arguments $gradleArguments
    $buildSucceeded = $build.ExitCode -eq 0
    if (-not $buildSucceeded) {
        Add-UniqueIssue -Issues $issues -Issue "$buildCommandText 실행이 실패했습니다."
    }

    $buildFailureLines = @(
        $build.Output |
            Where-Object {
                $_ -match "BUILD FAILED|FAILURE:|FAILED|Execution failed|There were failing tests|Could not|error|Exception"
            } |
            Select-Object -First 5
    )

    $rootPrefix = $resolvedProjectRoot + [System.IO.Path]::DirectorySeparatorChar
    $altRootPrefix = $resolvedProjectRoot + "/"
    $buildFailureLines = @(
        $buildFailureLines |
            ForEach-Object { $_.Replace($rootPrefix, "").Replace($altRootPrefix, "") }
    )

    $buildRelatedFiles = @(
        $build.Output |
            Select-String -Pattern '([A-Za-z]:\\.+?\.(java|gradle|properties|ya?ml)(:\d+)?)|((src|\.github|gradle)[\\/].+?\.(java|gradle|properties|ya?ml)(:\d+)?)' -AllMatches |
            ForEach-Object { $_.Matches.Value } |
            ForEach-Object {
                $file = $_.Trim()
                if ($file.StartsWith($resolvedProjectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
                    $file = $file.Substring($resolvedProjectRoot.Length).TrimStart("\", "/")
                }
                $file
            } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    )

    $filesNeedingChanges = @(Get-UniqueLines -Lines @($buildRelatedFiles + $sensitiveChanges + $structureFiles))
    $status = if (-not $buildSucceeded) {
        "위험"
    } elseif ($issues.Count -gt 0) {
        "주의"
    } else {
        "정상"
    }

    $failureSummary = if ($buildSucceeded) {
        if ($issues.Count -eq 0) { "빌드와 테스트가 통과했고 즉시 대응할 실패 징후는 없습니다." } else { ($issues -join " ") }
    } else {
        $summary = Get-CommandText -Lines $buildFailureLines
        if ([string]::IsNullOrWhiteSpace($summary)) {
            "빌드가 실패했지만 Gradle 출력에서 명확한 실패 요약을 찾지 못했습니다. 전체 로그 확인이 필요합니다."
        } else {
            Limit-Text -Text $summary -MaxLength 320
        }
    }

    $priorityItems = New-Object System.Collections.Generic.List[string]
    if (-not $buildSucceeded) {
        $priorityItems.Add("1. Gradle 실패 로그의 첫 번째 `Execution failed` 또는 `FAILED` 태스크를 기준으로 테스트/빌드 실패를 먼저 수정합니다.")
    } else {
        $priorityItems.Add("1. 현재 변경 파일 중 Security/OAuth와 Repository 쿼리 영향 범위를 테스트로 보강합니다.")
    }

    if (@(Test-AnyMatch -Files $changedFiles -Pattern "(^|[\\/])SecurityConfig\.java$|oauth|OAuth").Count -gt 0) {
        $priorityItems.Add("2. OAuth 로그인 성공/실패, 권한 없는 접근, 관리자 접근 경로를 회귀 테스트로 확인합니다.")
    } else {
        $priorityItems.Add("2. 인증/인가 핵심 경로의 스모크 테스트를 유지하고 SecurityConfig 변경 여부를 계속 감시합니다.")
    }

    if (@(Test-AnyMatch -Files $changedFiles -Pattern "(^|[\\/])application-prod\.properties$|^\.github[\\/]workflows[\\/].+\.ya?ml$|(^|[\\/])build\.gradle$").Count -gt 0) {
        $priorityItems.Add("3. prod 설정, workflow, Gradle 의존성 변경에 대해 배포 환경 변수와 CI 실행 조건을 재검증합니다.")
    } else {
        $priorityItems.Add("3. TODO/FIXME 증가와 배포 설정 변경을 다음 실행에서도 추적합니다.")
    }

    $fixSuggestions = New-Object System.Collections.Generic.List[string]
    if (-not $buildSucceeded) {
        $fixSuggestions.Add("실패한 Gradle 태스크의 관련 테스트를 단독 실행한 뒤, 실패 assertion 또는 누락된 설정값부터 수정하세요.")
    }
    if (-not $branchMatches) {
        $fixSuggestions.Add("자동화 실행 위치를 '$ExpectedBranch' 브랜치 작업공간으로 변경하거나, 실행 전에 안전하게 브랜치를 맞추도록 운영 절차를 정리하세요.")
    }
    if ($todoDelta -gt 0) {
        $fixSuggestions.Add("새 TODO/FIXME는 이슈 번호나 완료 기준을 함께 남기고, 단순 메모성 주석은 바로 제거하세요.")
    }
    if ($fixSuggestions.Count -eq 0) {
        $fixSuggestions.Add("즉시 적용할 수정은 없습니다. 민감 변경 파일이 있다면 관련 테스트 추가를 우선 검토하세요.")
    }

    $sensitiveChangesForMessage = @(Limit-Items -Items $sensitiveChanges -Max 3)
    $structureFilesForMessage = @(Limit-Items -Items $structureFiles -Max 3)
    $filesNeedingChangesForMessage = @(Limit-Items -Items $filesNeedingChanges -Max 5)

    $message = @"
**Board $ExpectedBranch 모니터링 결과**

**상태:** $status
**브랜치:** $currentBranch / 기대값: $ExpectedBranch
**빌드 명령:** ``$buildCommandText``
**빌드 결과:** $(if ($buildSucceeded) { "성공" } else { "실패" })
**테스트 실행:** $(if ($SkipTests) { "제외" } else { "포함" })
**TODO/FIXME:** 현재 $($todoCurrent.Count)개, 이전 커밋 대비 $(if ($todoDelta -gt 0) { "+$todoDelta" } else { "$todoDelta" })

**실패 원인 요약**
$failureSummary

**중요 변경 감지**
$(Format-List -Items $sensitiveChangesForMessage -EmptyText "중요 변경 없음")

**구조 검토 대상**
$(Format-List -Items $structureFilesForMessage -EmptyText "Repository/Service/Controller 변경 없음")

**수정이 필요한 파일 목록**
$(Format-List -Items $filesNeedingChangesForMessage -EmptyText "현재 자동 판별된 파일 없음")

**우선순위 높은 개선 작업 3개**
$($priorityItems -join "`n")

**바로 적용 가능한 수정 제안**
$(Format-List -Items $fixSuggestions)
"@

    $message = Limit-Text -Text $message -MaxLength 1900
    Write-Output $message

    if (-not $NoWebhook) {
        $webhookUrl = [Environment]::GetEnvironmentVariable("DISCORD_WEBHOOK_URL")
        if ([string]::IsNullOrWhiteSpace($webhookUrl)) {
            Write-Error "DISCORD_WEBHOOK_URL 환경변수가 설정되어 있지 않아 Discord 전송을 건너뜁니다."
            exit 2
        }

        $payload = @{ content = $message } | ConvertTo-Json -Depth 4
        try {
            Invoke-RestMethod -Uri $webhookUrl -Method Post -ContentType "application/json; charset=utf-8" -Body $payload | Out-Null
            Write-Output "Discord Webhook 전송 완료"
        } catch {
            $safeMessage = $_.Exception.Message
            if (-not [string]::IsNullOrWhiteSpace($webhookUrl)) {
                $safeMessage = $safeMessage.Replace($webhookUrl, "[redacted]")
            }
            Write-Error "Discord Webhook 전송 실패: $safeMessage"
            exit 3
        }
    }

    if ($status -eq "위험") {
        exit 1
    }

    exit 0
} catch {
    $errorMessage = $_.Exception.Message
    $message = @"
**Board $ExpectedBranch 모니터링 결과**

**상태:** 위험
**실패 원인 요약**
모니터링 스크립트 실행 중 오류가 발생했습니다: $errorMessage

**수정이 필요한 파일 목록**
- scripts/monitor-board.ps1

**우선순위 높은 개선 작업 3개**
1. 스크립트 실행 환경의 Git, Gradle wrapper, PowerShell 버전을 확인합니다.
2. 프로젝트 경로와 브랜치 설정이 자동화 설정과 일치하는지 확인합니다.
3. DISCORD_WEBHOOK_URL 환경변수가 설정되어 있는지 확인합니다.
"@

    $message = Limit-Text -Text $message -MaxLength 1900
    Write-Output $message

    if (-not $NoWebhook) {
        $webhookUrl = [Environment]::GetEnvironmentVariable("DISCORD_WEBHOOK_URL")
        if (-not [string]::IsNullOrWhiteSpace($webhookUrl)) {
            $payload = @{ content = $message } | ConvertTo-Json -Depth 4
            try {
                Invoke-RestMethod -Uri $webhookUrl -Method Post -ContentType "application/json; charset=utf-8" -Body $payload | Out-Null
            } catch {
                $safeMessage = $_.Exception.Message.Replace($webhookUrl, "[redacted]")
                Write-Error "Discord Webhook 전송 실패: $safeMessage"
            }
        }
    }

    exit 1
}

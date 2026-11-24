param(
    [Parameter(Mandatory = $true)]
    [string]$JMeter,
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, [long]::MaxValue)]
    [long]$ClientId,
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, [long]::MaxValue)]
    [long]$OtherClientId,
    [ValidateSet('127.0.0.1', 'localhost')]
    [string]$TargetHost = '127.0.0.1',
    [ValidateRange(1, 65535)]
    [int]$Port = 8083,
    [switch]$SmokeOnly
)

$ErrorActionPreference = 'Stop'
if (-not $env:JWT_TOKEN) {
    throw 'Supply a short-lived BUYER JWT in the process environment as JWT_TOKEN.'
}
if ($ClientId -eq $OtherClientId) {
    throw 'OtherClientId must differ from ClientId.'
}
$jmeterPath = (Get-Command $JMeter).Source
$plan = Join-Path $PSScriptRoot 'favourites-read-only.jmx'
$results = Join-Path $PSScriptRoot ("results/" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $results | Out-Null

$scenarios = @(
    @{ Name = 'smoke'; Users = 1; Ramp = 1; Duration = 5 }
    @{ Name = 'auth-checks'; Users = 0; Ramp = 1; Duration = 0; AuthChecks = 1 }
)
if (-not $SmokeOnly) {
    $scenarios += @(
        @{ Name = 'users-05'; Users = 5; Ramp = 3; Duration = 20 }
        @{ Name = 'users-20'; Users = 20; Ramp = 6; Duration = 25 }
        @{ Name = 'users-50'; Users = 50; Ramp = 10; Duration = 30 }
    )
}

foreach ($scenario in $scenarios) {
    $samplesPath = Join-Path $results ($scenario.Name + '.jtl')
    $logPath = Join-Path $results ($scenario.Name + '.log')
    $authChecks = [int]$scenario.AuthChecks
    & $jmeterPath -n -t $plan -l $samplesPath -j $logPath `
        "-Jusers=$($scenario.Users)" "-Jramp=$($scenario.Ramp)" `
        "-Jduration=$($scenario.Duration)" "-JauthChecks=$authChecks" `
        "-JHOST=$TargetHost" "-JPORT=$Port" "-JCLIENT_ID=$ClientId" "-JOTHER_CLIENT_ID=$OtherClientId" `
        '-Lorg.apache.http=WARN' `
        '-Jjmeter.save.saveservice.output_format=csv' `
        '-Jjmeter.save.saveservice.requestHeaders=false' `
        '-Jjmeter.save.saveservice.responseHeaders=false' `
        '-Jjmeter.save.saveservice.samplerData=false' `
        '-Jjmeter.save.saveservice.response_data=false' `
        '-Jjmeter.save.saveservice.response_data.on_error=false' `
        '-Jjmeter.save.saveservice.response_message=false' `
        '-Jjmeter.save.saveservice.assertion_results_failure_message=false' `
        '-Jjmeter.save.saveservice.response_code=true' `
        '-Jjmeter.save.saveservice.label=true' `
        '-Jjmeter.save.saveservice.successful=true' `
        '-Jjmeter.save.saveservice.print_field_names=true' `
        '-Jjmeter.save.saveservice.timestamp_format=ms' `
        '-Jsampleresult.timestamp.start=true'
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $samplesPath)) {
        throw "JMeter failed for $($scenario.Name). See $logPath."
    }

    $samples = @(Import-Csv -LiteralPath $samplesPath)
    if ($samples.Count -eq 0) {
        throw "JMeter recorded no samples for $($scenario.Name). See $logPath."
    }
    if ($authChecks) {
        $expected = @{ 'GET without token' = '401'; 'GET for another client' = '403' }
        $passed = @($samples | Where-Object {
            $_.success -eq 'true' -and $expected[$_.label] -eq $_.responseCode
        })
        if ($samples.Count -ne 2 -or $passed.Count -ne 2 -or
            @($passed.label | Sort-Object -Unique).Count -ne 2) {
            throw "JWT negative checks failed. See $samplesPath."
        }
        Write-Output 'JWT negative checks passed (401 without token, 403 for another client).'
        continue
    }
    if (@($samples | Where-Object { $_.label -ne 'GET favourites by client' }).Count -gt 0) {
        throw "Unexpected sample in $($scenario.Name). See $samplesPath."
    }
    $elapsed = @($samples | ForEach-Object { [long]$_.elapsed } | Sort-Object)
    $firstStart = ($samples | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Minimum).Minimum
    $lastEnd = ($samples | ForEach-Object { [long]$_.timeStamp + [long]$_.elapsed } | Measure-Object -Maximum).Maximum
    $failed = @($samples | Where-Object { $_.success -ne 'true' }).Count
    $p95Index = [int][math]::Ceiling(0.95 * $samples.Count) - 1

    $summary = [pscustomobject]@{
        Scenario = $scenario.Name
        ClientId = $ClientId
        Users = $scenario.Users
        RampSeconds = $scenario.Ramp
        ScheduledSeconds = $scenario.Duration
        Samples = $samples.Count
        Errors = $failed
        AverageMs = [math]::Round(($elapsed | Measure-Object -Average).Average, 2)
        P95Ms = $elapsed[$p95Index]
        RequestsPerSecond = [math]::Round(1000 * $samples.Count / [math]::Max(1, $lastEnd - $firstStart), 2)
        ErrorPercent = [math]::Round(100 * $failed / $samples.Count, 2)
    }
    $summary | Export-Csv -NoTypeInformation -Append -LiteralPath (Join-Path $results 'summary.csv')
    $summary | Format-Table -AutoSize
    if ($failed -gt 0) {
        throw "Scenario $($scenario.Name) failed. See $samplesPath."
    }
}

Write-Output "Results saved to $results"

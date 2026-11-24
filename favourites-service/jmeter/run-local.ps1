param(
    [Parameter(Mandatory = $true)]
    [string]$JMeter,
    [ValidateRange(1, [long]::MaxValue)]
    [long]$ClientId = 2026112401
)

$ErrorActionPreference = 'Stop'
$jmeterPath = (Get-Command $JMeter).Source
$plan = Join-Path $PSScriptRoot 'favourites-read-only.jmx'
$results = Join-Path $PSScriptRoot ("results/" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $results | Out-Null

$scenarios = @(
    @{ Name = 'smoke'; Users = 1; Ramp = 1; Duration = 5 }
    @{ Name = 'users-05'; Users = 5; Ramp = 3; Duration = 20 }
    @{ Name = 'users-20'; Users = 20; Ramp = 6; Duration = 25 }
    @{ Name = 'users-50'; Users = 50; Ramp = 10; Duration = 30 }
)

foreach ($scenario in $scenarios) {
    $samplesPath = Join-Path $results ($scenario.Name + '.jtl')
    $logPath = Join-Path $results ($scenario.Name + '.log')
    & $jmeterPath -n -t $plan -l $samplesPath -j $logPath `
        "-Jusers=$($scenario.Users)" "-Jramp=$($scenario.Ramp)" `
        "-Jduration=$($scenario.Duration)" "-JclientId=$ClientId" `
        '-Jjmeter.save.saveservice.output_format=csv' `
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
    if ($scenario.Name -eq 'smoke' -and $failed -gt 0) {
        throw "Smoke test failed. See $samplesPath."
    }
}

Write-Output "Results saved to $results"

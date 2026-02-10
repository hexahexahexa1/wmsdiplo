param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$baseUrl = $BaseUrl.TrimEnd('/')

function New-AuthHeader([string]$user, [string]$pass) {
    $pair = "$user`:$pass"
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    return @{ Authorization = "Basic $b64" }
}

function Try-ParseJson([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    try { return ($text | ConvertFrom-Json) } catch { return $null }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers,
        $Body = $null
    )
    $url = "$baseUrl$Path"
    try {
        if ($null -ne $Body) {
            $json = if ($Body -is [string]) { $Body } else { ($Body | ConvertTo-Json -Depth 10 -Compress) }
            $resp = Invoke-WebRequest -Uri $url -Method $Method -Headers $Headers -ContentType 'application/json' -Body $json -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri $url -Method $Method -Headers $Headers -UseBasicParsing
        }

        return [PSCustomObject]@{
            Status = [int]$resp.StatusCode
            Content = $resp.Content
            Json = Try-ParseJson $resp.Content
            Url = $url
        }
    } catch {
        $status = -1
        $content = ''
        if ($_.Exception.Response -ne $null) {
            try { $status = [int]$_.Exception.Response.StatusCode } catch {}
            try {
                $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
                $content = $reader.ReadToEnd()
            } catch {}
        } else {
            $content = $_.Exception.Message
        }
        return [PSCustomObject]@{
            Status = $status
            Content = $content
            Json = Try-ParseJson $content
            Url = $url
        }
    }
}

$results = New-Object System.Collections.Generic.List[object]
function Add-Result([string]$Name, [bool]$Passed, [string]$Details) {
    $results.Add([PSCustomObject]@{ Name = $Name; Passed = $Passed; Details = $Details }) | Out-Null
}
function Check-Status([string]$Name, $resp, [int[]]$allowed, [string]$details='') {
    $ok = $allowed -contains $resp.Status
    $msg = if ($ok) { "status=$($resp.Status) $details" } else { "status=$($resp.Status) expected=$($allowed -join ',') body=$($resp.Content)" }
    Add-Result $Name $ok $msg
    return $ok
}

$admin = New-AuthHeader 'admin' 'admin'
$supervisor = New-AuthHeader 'supervisor' 'supervisor'

function Ensure-StorageCapacity {
    $zones = Invoke-Api GET '/api/zones' $supervisor
    if ($zones.Status -ne 200 -or -not $zones.Json -or @($zones.Json).Count -eq 0) {
        Add-Result 'ensure_storage_zone_exists' $false ("status=$($zones.Status) body=$($zones.Content)")
        return $false
    }

    $zoneId = [long]@($zones.Json)[0].id
    $locationCode = "AUTO-STORAGE-$((Get-Date).ToString('yyyyMMddHHmmssfff'))"
    $create = Invoke-Api POST '/api/locations' $admin @{
        zoneId = $zoneId
        code = $locationCode
        aisle = 'AUTO'
        bay = '00'
        level = '00'
        maxPallets = 200
        locationType = 'STORAGE'
    }

    $ok = ($create.Status -eq 201 -or $create.Status -eq 200)
    Add-Result 'ensure_storage_capacity_location' $ok ("status=$($create.Status) code=$locationCode")
    return $ok
}

# Health
$r = Invoke-Api GET '/actuator/health' @{}
[void](Check-Status 'health_up' $r @(200))

# Analytics
$from = (Get-Date).AddDays(-7).ToString('yyyy-MM-dd')
$to = (Get-Date).ToString('yyyy-MM-dd')
[void](Check-Status 'analytics_receiving_ok' (Invoke-Api GET "/api/analytics/receiving?fromDate=$from&toDate=$to" $supervisor) @(200))
[void](Check-Status 'analytics_receiving_invalid_range' (Invoke-Api GET "/api/analytics/receiving?fromDate=$to&toDate=$from" $supervisor) @(400))
[void](Check-Status 'analytics_health_invalid_threshold' (Invoke-Api GET "/api/analytics/receiving-health?fromDate=$from&toDate=$to&thresholdHours=0" $supervisor) @(400))

# Task ID prefix filter
$page = Invoke-Api GET '/api/tasks?page=0&size=200&sort=id,asc' $supervisor
if (Check-Status 'tasks_page_fetch' $page @(200)) {
    $ids = @()
    if ($page.Json -and $page.Json.content) { $ids = @($page.Json.content | ForEach-Object { [string]$_.id }) }
    if ($ids.Count -gt 0) {
        $sample = $ids[0]
        $p1 = $sample.Substring(0,1)
        $f1 = Invoke-Api GET "/api/tasks?page=0&size=200&taskId=$p1&sort=id,asc" $supervisor
        if (Check-Status 'tasks_filter_prefix1_status' $f1 @(200)) {
            $all1 = @($f1.Json.content | ForEach-Object { [string]$_.id })
            $ok = $true
            foreach($id in $all1){ if(-not $id.StartsWith($p1)){ $ok = $false; break } }
            Add-Result 'tasks_filter_prefix1_match' $ok ("prefix=$p1 count=$($all1.Count)")
        }
        if ($sample.Length -ge 2) {
            $p2 = $sample.Substring(0,2)
            $f2 = Invoke-Api GET "/api/tasks?page=0&size=200&taskId=$p2&sort=id,asc" $supervisor
            if (Check-Status 'tasks_filter_prefix2_status' $f2 @(200)) {
                $all2 = @($f2.Json.content | ForEach-Object { [string]$_.id })
                $ok2 = $true
                foreach($id in $all2){ if(-not $id.StartsWith($p2)){ $ok2 = $false; break } }
                Add-Result 'tasks_filter_prefix2_match' $ok2 ("prefix=$p2 count=$($all2.Count)")
            }
        }
    } else {
        Add-Result 'tasks_filter_data_present' $false 'No tasks found in paginated response'
    }
}

# Create helper SKUs
$ts = Get-Date -Format 'yyyyMMddHHmmssfff'
$expectedCode = "AUTO-EXP-$ts"
$targetCode = "AUTO-TARGET-$ts"
$draftBarcode = "AUTO-DRAFT-$ts"

$expResp = Invoke-Api POST '/api/skus' $supervisor @{ code=$expectedCode; name='Auto Expected'; uom='PCS' }
$tgtResp = Invoke-Api POST '/api/skus' $supervisor @{ code=$targetCode; name='Auto Target'; uom='BOX' }
$expOk = Check-Status 'sku_expected_create' $expResp @(200,201)
$tgtOk = Check-Status 'sku_target_create' $tgtResp @(200,201)

$expectedSkuId = $null
$targetSkuId = $null
if ($expOk -and $tgtOk) {
    $expectedSkuId = [long]$expResp.Json.id
    $targetSkuId = [long]$tgtResp.Json.id
    Add-Result 'sku_ids_resolved' (($expectedSkuId -gt 0) -and ($targetSkuId -gt 0)) ("expectedSkuId=$expectedSkuId targetSkuId=$targetSkuId")
}

# Flow A: blocker/remap/placement SKU
$flowReceiptId = $null
if ($expectedSkuId -and $targetSkuId) {
    $storageReady = Ensure-StorageCapacity
    if (-not $storageReady) {
        Add-Result 'flow_start_placement_prereq' $false 'Failed to prepare free STORAGE capacity'
    }

    $doc = "AUTO-REMAP-$ts"
    $rc = Invoke-Api POST '/api/receipts/drafts' $supervisor @{ docNo=$doc; docDate=(Get-Date).ToString('yyyy-MM-dd'); supplier='AUTO'; crossDock=$false }
    if (Check-Status 'flow_receipt_create' $rc @(201)) {
        $flowReceiptId = [long]$rc.Json.id
        $add = Invoke-Api POST "/api/receipts/$flowReceiptId/lines" $supervisor @{ lineNo=1; skuId=$expectedSkuId; uom='PCS'; qtyExpected=1 }
        if (Check-Status 'flow_add_line' $add @(201)) {
            [void](Check-Status 'flow_confirm' (Invoke-Api POST "/api/receipts/$flowReceiptId/confirm" $supervisor) @(202))
            [void](Check-Status 'flow_start_receiving' (Invoke-Api POST "/api/receipts/$flowReceiptId/start-receiving" $supervisor) @(202))

            $lt = Invoke-Api GET "/api/tasks?receiptId=$flowReceiptId" $supervisor
            if (Check-Status 'flow_list_tasks' $lt @(200)) {
                $rt = @($lt.Json | Where-Object { $_.taskType -eq 'RECEIVING' })[0]
                if ($rt) {
                    $rtId = [long]$rt.id
                    $pc = "PLT-FLOW-$ts"
                    [void](Check-Status 'flow_create_pallet' (Invoke-Api POST '/api/pallets' $supervisor @{ code=$pc; status='EMPTY' }) @(201))
                    $scan = Invoke-Api POST "/api/tasks/$rtId/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$pc; qty=1; barcode=$draftBarcode; comment='' }
                    if (Check-Status 'flow_scan_mismatch' $scan @(201)) {
                        [void](Check-Status 'flow_complete_task' (Invoke-Api POST "/api/tasks/$rtId/complete" $supervisor) @(200))

                        $rs = Invoke-Api GET "/api/receipts/$flowReceiptId" $supervisor
                        Add-Result 'flow_receipt_in_progress_after_task_complete' ($rs.Status -eq 200 -and $rs.Json.status -eq 'IN_PROGRESS') ("status=$($rs.Json.status)")

                        [void](Check-Status 'flow_complete_receiving_blocked' (Invoke-Api POST "/api/receipts/$flowReceiptId/complete-receiving" $supervisor) @(409))

                        $dlist = Invoke-Api GET "/api/discrepancies?receiptId=$flowReceiptId&resolved=false" $supervisor
                        if (Check-Status 'flow_discrepancy_list' $dlist @(200)) {
                            $d = @($dlist.Json | Where-Object { $_.draftSkuId -ne $null })[0]
                            if ($d) {
                                [void](Check-Status 'flow_remap' (Invoke-Api POST "/api/discrepancies/$($d.id)/remap-sku" $supervisor @{ targetSkuId=$targetSkuId }) @(200))
                                [void](Check-Status 'flow_complete_receiving_after_remap' (Invoke-Api POST "/api/receipts/$flowReceiptId/complete-receiving" $supervisor) @(202))
                                [void](Check-Status 'flow_start_placement' (Invoke-Api POST "/api/receipts/$flowReceiptId/start-placement" $supervisor) @(202))

                                $aft = Invoke-Api GET "/api/tasks?receiptId=$flowReceiptId" $supervisor
                                if (Check-Status 'flow_tasks_after_placement' $aft @(200)) {
                                    $pts = @($aft.Json | Where-Object { $_.taskType -eq 'PLACEMENT' -and $_.status -ne 'CANCELLED' })
                                    $allTarget = $true
                                    foreach($t in $pts){ if($t.skuCode -ne $targetCode){ $allTarget = $false; break } }
                                    Add-Result 'flow_placement_sku_is_remapped' $allTarget ("expected=$targetCode count=$($pts.Count) sample=$(@($pts | Select-Object -First 3 | ForEach-Object {$_.skuCode}) -join ',')")
                                }
                            } else {
                                Add-Result 'flow_discrepancy_with_draft_exists' $false 'No unresolved discrepancy with draftSkuId'
                            }
                        }
                    }
                } else {
                    Add-Result 'flow_receiving_task_exists' $false 'No receiving task found'
                }
            }
        }
    }
}

# Flow B: receiving undo + release
$ts2 = Get-Date -Format 'yyyyMMddHHmmssfff'
if ($expectedSkuId) {
    $r2 = Invoke-Api POST '/api/receipts/drafts' $supervisor @{ docNo=("AUTO-UNDO-"+$ts2); docDate=(Get-Date).ToString('yyyy-MM-dd'); supplier='AUTO'; crossDock=$false }
    if (Check-Status 'undo_receipt_create' $r2 @(201)) {
        $rid2 = [long]$r2.Json.id
        if (Check-Status 'undo_add_line' (Invoke-Api POST "/api/receipts/$rid2/lines" $supervisor @{ lineNo=1; skuId=$expectedSkuId; uom='PCS'; qtyExpected=1 }) @(201)) {
            [void](Check-Status 'undo_confirm' (Invoke-Api POST "/api/receipts/$rid2/confirm" $supervisor) @(202))
            [void](Check-Status 'undo_start_receiving' (Invoke-Api POST "/api/receipts/$rid2/start-receiving" $supervisor) @(202))
            $t2 = Invoke-Api GET "/api/tasks?receiptId=$rid2" $supervisor
            if (Check-Status 'undo_list_tasks' $t2 @(200)) {
                $rt2 = @($t2.Json | Where-Object { $_.taskType -eq 'RECEIVING' })[0]
                if ($rt2) {
                    $task2 = [long]$rt2.id
                    $p2 = "PLT-UNDO-$ts2"
                    [void](Check-Status 'undo_create_pallet' (Invoke-Api POST '/api/pallets' $supervisor @{ code=$p2; status='EMPTY' }) @(201))
                    [void](Check-Status 'undo_scan' (Invoke-Api POST "/api/tasks/$task2/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$p2; qty=1; barcode=$expectedCode }) @(201))
                    [void](Check-Status 'undo_call' (Invoke-Api POST "/api/tasks/$task2/undo-last-scan" $supervisor) @(200))

                    $sc2 = Invoke-Api GET "/api/tasks/$task2/scans" $supervisor
                    if (Check-Status 'undo_scans_after_undo_list' $sc2 @(200)) {
                        Add-Result 'undo_scans_after_undo_empty' (@($sc2.Json).Count -eq 0) ("count=$(@($sc2.Json).Count)")
                    }

                    [void](Check-Status 'undo_complete_without_scans_blocked' (Invoke-Api POST "/api/tasks/$task2/complete" $supervisor) @(400))

                    [void](Check-Status 'release_scan_again' (Invoke-Api POST "/api/tasks/$task2/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$p2; qty=1; barcode=$expectedCode }) @(201))
                    $rel = Invoke-Api POST "/api/tasks/$task2/release" $supervisor
                    if (Check-Status 'release_call' $rel @(200)) {
                        $okReset = ($rel.Json.status -eq 'NEW' -and [decimal]$rel.Json.qtyDone -eq 0)
                        Add-Result 'release_task_reset' $okReset ("status=$($rel.Json.status) qtyDone=$($rel.Json.qtyDone)")
                    }

                    $sc3 = Invoke-Api GET "/api/tasks/$task2/scans" $supervisor
                    if (Check-Status 'release_scans_list' $sc3 @(200)) {
                        Add-Result 'release_scans_empty' (@($sc3.Json).Count -eq 0) ("count=$(@($sc3.Json).Count)")
                    }

                    $p2State = Invoke-Api GET "/api/pallets/$($rel.Json.palletId)" $supervisor
                    if ($p2State.Status -eq 200) {
                        $rolledBack = ($p2State.Json.status -eq 'EMPTY' -and $null -eq $p2State.Json.skuId)
                        Add-Result 'release_pallet_rolled_back' $rolledBack ("status=$($p2State.Json.status) skuId=$($p2State.Json.skuId)")
                    }
                }
            }
        }
    }
}

# Flow C: undo forbidden for completed
$ts3 = Get-Date -Format 'yyyyMMddHHmmssfff'
if ($expectedSkuId) {
    $r3 = Invoke-Api POST '/api/receipts/drafts' $supervisor @{ docNo=("AUTO-UC-"+$ts3); docDate=(Get-Date).ToString('yyyy-MM-dd'); supplier='AUTO'; crossDock=$false }
    if (Check-Status 'uc_receipt_create' $r3 @(201)) {
        $rid3 = [long]$r3.Json.id
        [void](Check-Status 'uc_add_line' (Invoke-Api POST "/api/receipts/$rid3/lines" $supervisor @{ lineNo=1; skuId=$expectedSkuId; uom='PCS'; qtyExpected=1 }) @(201))
        [void](Check-Status 'uc_confirm' (Invoke-Api POST "/api/receipts/$rid3/confirm" $supervisor) @(202))
        [void](Check-Status 'uc_start_receiving' (Invoke-Api POST "/api/receipts/$rid3/start-receiving" $supervisor) @(202))
        $t3 = Invoke-Api GET "/api/tasks?receiptId=$rid3" $supervisor
        if ($t3.Status -eq 200) {
            $rt3 = @($t3.Json | Where-Object { $_.taskType -eq 'RECEIVING' })[0]
            if ($rt3) {
                $pt3 = "PLT-UC-$ts3"
                [void](Check-Status 'uc_create_pallet' (Invoke-Api POST '/api/pallets' $supervisor @{ code=$pt3; status='EMPTY' }) @(201))
                [void](Check-Status 'uc_scan' (Invoke-Api POST "/api/tasks/$($rt3.id)/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$pt3; qty=1; barcode=$expectedCode }) @(201))
                [void](Check-Status 'uc_complete_task' (Invoke-Api POST "/api/tasks/$($rt3.id)/complete" $supervisor) @(200))
                [void](Check-Status 'uc_undo_blocked' (Invoke-Api POST "/api/tasks/$($rt3.id)/undo-last-scan" $supervisor) @(409))
            }
        }
    }
}

# Flow D: cross-dock + shipping + shipping undo
$ts4 = Get-Date -Format 'yyyyMMddHHmmssfff'
if ($expectedSkuId) {
    $r4 = Invoke-Api POST '/api/receipts/drafts' $supervisor @{ docNo=("AUTO-XD-"+$ts4); docDate=(Get-Date).ToString('yyyy-MM-dd'); supplier='AUTO'; crossDock=$true; outboundRef=("OUT-"+$ts4) }
    if (Check-Status 'xd_receipt_create' $r4 @(201)) {
        $rid4 = [long]$r4.Json.id
        [void](Check-Status 'xd_add_line' (Invoke-Api POST "/api/receipts/$rid4/lines" $supervisor @{ lineNo=1; skuId=$expectedSkuId; uom='PCS'; qtyExpected=1 }) @(201))
        [void](Check-Status 'xd_confirm' (Invoke-Api POST "/api/receipts/$rid4/confirm" $supervisor) @(202))
        [void](Check-Status 'xd_start_receiving' (Invoke-Api POST "/api/receipts/$rid4/start-receiving" $supervisor) @(202))

        $tr4 = Invoke-Api GET "/api/tasks?receiptId=$rid4" $supervisor
        if ($tr4.Status -eq 200) {
            $rt4 = @($tr4.Json | Where-Object { $_.taskType -eq 'RECEIVING' })[0]
            if ($rt4) {
                $px = "PLT-XD-$ts4"
                [void](Check-Status 'xd_create_pallet' (Invoke-Api POST '/api/pallets' $supervisor @{ code=$px; status='EMPTY' }) @(201))
                [void](Check-Status 'xd_scan_receiving' (Invoke-Api POST "/api/tasks/$($rt4.id)/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$px; qty=1; barcode=$expectedCode }) @(201))
                [void](Check-Status 'xd_complete_receiving_task' (Invoke-Api POST "/api/tasks/$($rt4.id)/complete" $supervisor) @(200))

                $rs4 = Invoke-Api GET "/api/receipts/$rid4" $supervisor
                Add-Result 'xd_status_ready_for_placement' ($rs4.Status -eq 200 -and $rs4.Json.status -eq 'READY_FOR_PLACEMENT') ("status=$($rs4.Json.status)")

                $sp4 = Invoke-Api POST "/api/receipts/$rid4/start-placement" $supervisor
                if (Check-Status 'xd_start_placement' $sp4 @(202)) {
                    $tp4 = Invoke-Api GET "/api/tasks?receiptId=$rid4" $supervisor
                    if ($tp4.Status -eq 200) {
                        $plNew = @($tp4.Json | Where-Object { $_.taskType -eq 'PLACEMENT' -and $_.status -eq 'NEW' })
                        foreach($pt in $plNew) {
                            Invoke-Api POST "/api/tasks/$($pt.id)/start" $supervisor | Out-Null
                            Invoke-Api POST "/api/tasks/$($pt.id)/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$pt.palletCode; qty=1; locationCode=$pt.targetLocationCode; barcode=$expectedCode } | Out-Null
                            Invoke-Api POST "/api/tasks/$($pt.id)/complete" $supervisor | Out-Null
                        }

                        $ra4 = Invoke-Api GET "/api/receipts/$rid4" $supervisor
                        Add-Result 'xd_status_ready_for_shipping_after_placement' ($ra4.Status -eq 200 -and $ra4.Json.status -eq 'READY_FOR_SHIPMENT') ("status=$($ra4.Json.status)")

                        $ss4 = Invoke-Api POST "/api/receipts/$rid4/start-shipping" $supervisor
                        if (Check-Status 'xd_start_shipping' $ss4 @(202)) {
                            $ts4all = Invoke-Api GET "/api/tasks?receiptId=$rid4" $supervisor
                            if ($ts4all.Status -eq 200) {
                                $sh = @($ts4all.Json | Where-Object { $_.taskType -eq 'SHIPPING' -and $_.status -eq 'NEW' })[0]
                                if ($sh) {
                                    [void](Check-Status 'xd_start_shipping_task' (Invoke-Api POST "/api/tasks/$($sh.id)/start" $supervisor) @(200))
                                    $sscan = Invoke-Api POST "/api/tasks/$($sh.id)/scans" $supervisor @{ requestId=("req-"+[guid]::NewGuid().ToString()); palletCode=$sh.palletCode; qty=1; barcode=$expectedCode }
                                    if (Check-Status 'xd_shipping_scan' $sscan @(201)) {
                                        [void](Check-Status 'xd_shipping_undo' (Invoke-Api POST "/api/tasks/$($sh.id)/undo-last-scan" $supervisor) @(200))
                                    }
                                } else {
                                    Add-Result 'xd_shipping_task_created' $false 'No NEW shipping task found'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

# Security checks
[void](Check-Status 'security_unauthorized_without_auth' (Invoke-Api GET '/api/tasks?page=0&size=1' @{}) @(401))
$opLogin = Invoke-Api POST '/api/auth/login' @{} @{ username='operator'; password='operator' }
if ($opLogin.Status -eq 200) {
    $operator = New-AuthHeader 'operator' 'operator'
    [void](Check-Status 'security_operator_analytics_forbidden' (Invoke-Api GET "/api/analytics/receiving?fromDate=$from&toDate=$to" $operator) @(403))
} else {
    Add-Result 'security_operator_credentials_present' $false ("operator login status=$($opLogin.Status)")
}

# Summary
$passed = @($results | Where-Object { $_.Passed }).Count
$total = $results.Count
$failed = $total - $passed

"=== API FULL REGRESSION REPORT ==="
"TOTAL=$total PASSED=$passed FAILED=$failed"
$results | ForEach-Object {
    $flag = if ($_.Passed) { 'PASS' } else { 'FAIL' }
    "$flag`t$($_.Name)`t$($_.Details)"
}
if ($failed -gt 0) { exit 2 }



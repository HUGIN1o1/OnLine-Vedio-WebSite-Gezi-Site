# 将指定目录下的文件按文件名排序后，依次 PUT 到预签名 URL（与分片 0,1,2… 一一对应）
# 用法（在 PowerShell 中）:
#   cd 项目根目录
#   .\scripts\upload-tes-to-presigned.ps1
# 或改目录:
#   .\scripts\upload-tes-to-presigned.ps1 -Folder "D:\other\chunks"

param(
    [string] $Folder = "C:\Users\Lenovo\Desktop\tes"
)

$ErrorActionPreference = "Stop"

# 预签名 PUT 地址（顺序对应 chunk 索引 0..n）；过期后需重新向业务接口申请
$urls = @(
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/0?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=4dd2b62fb4b8acccd7ebd5891b4d80762235f1aae6bb7489c832ed57d3dedee6",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/1?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=2346cf549265ba24aff84f838d8b5a1593121841dc905783c643be4097698acc",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/2?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=62a490e828c1ea70b3b04c2a8aa4234072e6ce40f4de794b122695a46f3a2eb5",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/3?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=eb8ceae63c0dc74eb2f3f7b9037b579f2e2c5762aab149a708e09c4680253a6c",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=4121aecf9656748b2ca5e96beabd3acf0338e990a0c5fb11782bfa703fda2bc1",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/5?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=c4c88d4b6cd31801934f94bd5cefc287b4f8a8bed155b4f193fb5b4fa1c166d1",
    "http://127.0.0.1:9000/geligeli/b4fc45d40giacsbc5e3e62bg34a93a1e4/6?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260412%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260412T163642Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=4a9231955c32494cbf4e39a91bef4eaaa1cc126a98b93bea26a7d4db036e7616"
)

if (-not (Test-Path -LiteralPath $Folder)) {
    Write-Error "目录不存在: $Folder"
}

$files = Get-ChildItem -LiteralPath $Folder -File | Sort-Object Name
if ($files.Count -eq 0) {
    Write-Error "目录下没有文件: $Folder"
}

$n = [Math]::Min($urls.Count, $files.Count)
if ($files.Count -ne $urls.Count) {
    Write-Warning "文件数 ($($files.Count)) 与 URL 数 ($($urls.Count)) 不一致，将只上传前 $n 个。"
}

for ($i = 0; $i -lt $n; $i++) {
    $f = $files[$i]
    $u = $urls[$i]
    Write-Host "[$i] PUT $($f.Name) -> .../$i"
    # 仅 Host 参与签名时不要随意加 Content-Type，否则可能 403
    Invoke-WebRequest -Uri $u -Method Put -InFile $f.FullName -UseBasicParsing
    Write-Host "[$i] OK ($($f.Length) bytes)"
}

Write-Host "完成，共上传 $n 个分片。"

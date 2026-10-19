"""Package tracked sources plus private local report materials; never include runtime secrets."""
import hashlib,json,pathlib,subprocess,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'delivery/学习日程Agent_最终完整版_20261020.zip'
PREFIX='学习日程Agent_最终完整版'
files={}
tracked=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode('utf8').split('\0')
for name in tracked:
    if not name:continue
    p=pathlib.PurePosixPath(name)
    if any(part in {'.git','.runtime','node_modules','target','__pycache__'} for part in p.parts):continue
    if p.name=='.env' or p.name.startswith('.env.'):raise RuntimeError('Environment file cannot enter package: '+name)
    file=ROOT/name
    if file.is_file():files[name]=file
for folder in ['delivery/materials','delivery/reports']:
    for file in (ROOT/folder).rglob('*'):
        if not file.is_file() or file.suffix.lower() not in {'.docx','.json'} or file.name.startswith('~$'):continue
        files[file.relative_to(ROOT).as_posix()]=file
video=ROOT/'delivery/学习日程Agent_五分钟自动讲解演示.mp4'
assert video.is_file()
files[video.relative_to(ROOT).as_posix()]=video
report=ROOT/'delivery/reports/学习日程Agent系统设计报告_最终交付版.docx'
qa=json.loads((ROOT/'delivery/evidence/report-qa.json').read_text(encoding='utf8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(report)==qa['reportSha256']
assert qa['wordPages']>70 and qa['allTablesHavePrecedingCaptionAndNote']
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
manifest={'branch':'codex/final-delivery','sourceCommit':commit,'applicationCodeBase':'83d80711b4ba9d452f532dbc003d77afa10458fc','pullRequest':'https://github.com/obito389/study-schedule-agent/pull/10','reportPages':qa['wordPages'],'reportSha256':sha(report),'videoSeconds':300,'tests':{'backend':102,'frontend':17,'externalBlackbox':15,'browserScenarios':3},'notCompleted':['服务重启恢复验证','正式认证','OCR/ASR','跨设备重要事项同步'],'files':{name:{'bytes':file.stat().st_size,'sha256':sha(file)} for name,file in sorted(files.items())}}
with zipfile.ZipFile(OUT,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
    for name,file in sorted(files.items()):archive.write(file,PREFIX+'/'+name)
    archive.writestr(PREFIX+'/交付清单.json',json.dumps(manifest,ensure_ascii=False,indent=2))
with zipfile.ZipFile(OUT) as archive:
    assert archive.testzip() is None
    embedded=archive.read(PREFIX+'/'+report.relative_to(ROOT).as_posix())
    assert hashlib.sha256(embedded).hexdigest()==qa['reportSha256']
    for name,file in files.items():assert hashlib.sha256(archive.read(PREFIX+'/'+name)).hexdigest()==sha(file)
summary={'archive':str(OUT),'bytes':OUT.stat().st_size,'entries':len(files)+1,'sha256':sha(OUT),'sourceCommit':commit,'reportSha256':qa['reportSha256'],'zipIntegrity':'PASS','allEntryHashesMatch':'PASS'}
(ROOT/'.runtime/package-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(summary,ensure_ascii=False,indent=2))

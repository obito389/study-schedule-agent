"""Build the 2026-10-20 user recording using its reviewed ten-chapter timeline.
The fixed edit points apply to this recording; a different recording needs realignment.
Source audio stays local and is never modified or included in Git.
"""
import argparse,hashlib,json,pathlib,re,subprocess,sys,wave
from PIL import Image,ImageDraw,ImageFont,ImageOps
ROOT=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--source',type=pathlib.Path,required=True);args=parser.parse_args()
source=args.source.resolve();work=ROOT/'.runtime/human-video';work.mkdir(parents=True,exist_ok=True)
sys.path.insert(0,str(ROOT/'.runtime/video-deps'));import imageio_ffmpeg
ff=imageio_ffmpeg.get_ffmpeg_exe()
def run(cmd):return subprocess.run([ff,'-y','-hide_banner','-loglevel','error']+cmd,check=True)
probe=subprocess.run([ff,'-hide_banner','-i',str(source)],capture_output=True,text=True,encoding='utf8',errors='replace').stderr
m=re.search(r'Duration: (\d+):(\d+):(\d+\.\d+)',probe);duration=int(m[1])*3600+int(m[2])*60+float(m[3])
# Keep at least 600 ms in each long pause, including 300 ms at each edge.
analysis=subprocess.run([ff,'-hide_banner','-i',str(source),'-af','silencedetect=noise=-32dB:d=0.5','-f','null','-'],capture_output=True,text=True,encoding='utf8',errors='replace')
if analysis.returncode:raise RuntimeError('Source audio could not be decoded')
log=analysis.stderr
(work/'silence-analysis.log').write_text(log,encoding='utf8')
starts=[float(x) for x in re.findall(r'silence_start: ([\d.]+)',log)]
ends=[float(x) for x in re.findall(r'silence_end: ([\d.]+)',log)]
removed=[(0,.60)]
for a,b in zip(starts,ends):
 if a>1 and b<361.4 and b-a>.95:removed.append((a+.30,b-.30))
removed.append((361.78,duration));removed.sort()
assert duration>361.78 and all(b>a for a,b in removed)
kept=[];last=0
for a,b in removed:
 if a>last:kept.append((last,a))
 last=b
if last<duration:kept.append((last,duration))
# A small pitch-preserving tempo adjustment keeps the talk near five and a half minutes.
kept_seconds=sum(b-a for a,b in kept);tempo=max(1,kept_seconds/340)
def mapped(t):
 return (t-sum(max(0,min(t,b)-a) for a,b in removed if t>a))/tempo
parts=[f'[0:a]atrim=start={a:.6f}:end={b:.6f},asetpts=PTS-STARTPTS[a{i}]' for i,(a,b) in enumerate(kept)]
parts.append(''.join(f'[a{i}]' for i in range(len(kept)))+f'concat=n={len(kept)}:v=0:a=1,highpass=f=70,atempo={tempo:.8f},loudnorm=I=-16:TP=-1.5:LRA=11,afade=t=in:d=0.025,afade=t=out:st={kept_seconds/tempo-.12:.6f}:d=0.12[out]')
audio=work/'narration.wav'
run(['-i',str(source),'-filter_complex',';'.join(parts),'-map','[out]','-ar','48000','-ac','1',str(audio)])
with wave.open(str(audio)) as w:out_duration=w.getnframes()/w.getframerate()
scenes=[
 ('项目介绍',.60,'01-tasks.png'),('成员分工与架构',31.55,'architecture-6.png'),
 ('新建任务',61.55,'02-task-editor.png'),('规则排程',94.25,'03-plan-preview.png'),
 ('日历与重要事项',126.1,'04-calendar-week.png'),('AI 学习助手',160.95,'06-ai.png'),
 ('导入与人工校对',205.2,'07-import-review.png'),('偏好与完成反馈',254.65,'08-profile.png'),
 ('执行记录与撤销',290.8,'09-history.png'),('测试与当前进度',320.65,'test-results')]
font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',42);small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',25)
shots=ROOT/'delivery/evidence/screenshots'
# A measured-results slide uses the saved evidence, not automatic speech recognition numbers.
board=Image.new('RGB',(1720,890),'#fffefa');d=ImageDraw.Draw(board)
d.text((60,32),'最终版实测结果',font=font,fill='#18423c')
rows=[('后端回归与 MySQL 集成','102 项通过'),('前端单元测试','17 项通过'),('HTTP 接口黑盒测试','15 项通过'),('浏览器端到端场景','3 条通过')]
for i,(label,value) in enumerate(rows):
 y=135+i*105;d.rounded_rectangle((50,y,1670,y+83),radius=12,fill=('#e8f2ec' if i%2==0 else '#f3f6f2'));d.text((80,y+17),label,font=font,fill='#18423c');d.text((1190,y+17),value,font=font,fill='#18423c')
d.text((65,600),'白盒核心模块：行覆盖 88.18%  ·  分支覆盖 66.04%',font=font,fill='#18423c')
d.text((65,670),'组员服务模块：行覆盖 49.36%  ·  分支覆盖 43.57%',font=font,fill='#18423c')
d.text((65,777),'待完善：正式认证、图片/音频自动识别、跨设备备注同步、重启恢复验证',font=small,fill='#52675f')
board.save(work/'test-results.png')
frame_specs=[]
for i,(title,start,file) in enumerate(scenes):
 end=scenes[i+1][1] if i+1<len(scenes) else 361.78
 variants=[(start,file)]
 if i==4:variants=[(start,file),(139.0,'11-calendar-event.png'),(149.0,'05-calendar-month.png')]
 for j,(vstart,vfile) in enumerate(variants):
  vend=variants[j+1][0] if j+1<len(variants) else end
  frame=Image.new('RGB',(1920,1080),'#f4f7f4');d=ImageDraw.Draw(frame)
  d.text((55,18),f'{i+1:02d}  {title}',font=font,fill='#18423c');d.text((1410,30),'熊翔讲解 · 真实界面',font=small,fill='#46665d')
  src=work/'test-results.png' if vfile=='test-results' else shots/vfile
  with Image.open(src) as im:
   im=ImageOps.contain(im.convert('RGB'),(1810,900));frame.paste(im,((1920-im.width)//2,92+(900-im.height)//2))
  d.text((55,1020),'学习日程 Agent  |  最终集成版',font=small,fill='#46665d')
  path=work/f'frame-{i:02d}-{j}.png';frame.save(path)
  frame_specs.append({'path':path,'start':mapped(vstart),'end':mapped(vend),'chapter':title})
concat=work/'frames.txt';concat.write_text(''.join(f"file '{x['path'].as_posix()}'\nduration {x['end']-x['start']:.6f}\n" for x in frame_specs)+f"file '{frame_specs[-1]['path'].as_posix()}'\n",encoding='utf8')
output=ROOT/'delivery/学习日程Agent_真人讲解版.mp4'
run(['-f','concat','-safe','0','-i',str(concat),'-i',str(audio),'-t',str(out_duration),'-r','15','-c:v','libx264','-preset','veryfast','-crf','22','-pix_fmt','yuv420p','-c:a','aac','-b:a','160k','-movflags','+faststart',str(output)])
run(['-i',str(output),'-f','null','-'])
metadata={'file':output.name,'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'sourceSeconds':duration,'seconds':round(out_duration,3),'method':'用户本人录音配真实界面截图；无合成语音','audioProcessing':{'trimmedLongPausesSeconds':round(sum(b-a for a,b in removed),3),'pitchPreservingTempo':round(tempo,5),'loudnessTargetLufs':-16,'highPassHz':70},'resolution':'1920x1080','framesPerSecond':15,'fullDecodeSucceeded':True,'bytes':output.stat().st_size,'chapters':[{'title':title,'startSeconds':round(mapped(start),3)} for title,start,_ in scenes]}
(ROOT/'delivery/evidence/human-video.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(metadata,ensure_ascii=False,indent=2))


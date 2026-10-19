"""Compose a five-minute, clearly labelled automatic narration using actual final UI screenshots."""
import json,pathlib,re,subprocess,sys,wave
from PIL import Image,ImageDraw,ImageFont,ImageOps
ROOT=pathlib.Path(__file__).resolve().parents[1]
work=ROOT/'.runtime/narration';work.mkdir(parents=True,exist_ok=True)
scenes=[
 ('项目与成员分工','01-tasks.png','本视频介绍学习日程智能助手最终交付版。项目面向同时处理课程、作业和复习任务的学生，重点是把任务、时间安排和确认执行连接起来。熊翔负责架构、智能助手、排程和用户模型，并承担组长的集成与验收工作。黄健华负责数据库和事务后端，赵欢负责导入链路，樊华钰提供前端工程和组件原型。最终界面沿用组长调整的整体设计。'),
 ('模块化架构与真实数据','architecture-6.png','最终系统以组员提交的后端模块作为工程基础，保留十六张业务表、数据访问层、动作执行器和撤销模块。主线提供稳定契约、排程算法和用户事实服务。新增的适配层负责连接最终界面，不另外复制一套任务数据库。页面显示的数据来自真实服务和数据库。模型只提出建议，正式写入仍然需要用户确认和后端校验。'),
 ('创建任务与安排时间分离','02-task-editor.png','新建任务时填写名称、截止时间、预计耗时、优先级和所属课程。表单按北京时间输入，接口显式转换时间，避免浏览器时区不同造成偏移。创建任务并不代表已经占用某个时间段。后端先检查字段、归属和当前版本，再通过统一执行器保存。重复点击使用同一个操作键，不会重复创建任务。'),
 ('规则候选与确认','03-plan-preview.png','规则排程按照截止时间、优先级和任务标识稳定排序，以三十分钟为时间槽寻找可行安排。候选需要满足截止、重叠、课程占用、禁止时段和每日上限等约束。预览阶段还没有正式时间块，用户核对后点击确认才会保存。无法完整安排的任务报告缺口，不会把截短的安排偷偷当作完成。旧版本预览必须重新生成。'),
 ('日历与重要事项','05-calendar-month.png','最终日历保留清晰的小时刻度，提供日、周、月视图。周视图可以区分工作日和周末，事件块显示时间和事项。月视图只突出有安排的日期，点击日期可以记录重要事项。这里需要明确，重要事项目前保存在当前浏览器，不是跨设备同步功能。任务与正式时间块则由后端和数据库保存。'),
 ('真实模型建议','06-ai.png','智能助手已接入真实的深度求索模型。用户描述目标后，系统先显示处理中，再返回待补充、待确认、只读查询或失败状态。模型输出会转换成结构化建议，不能直接修改数据库。明确的时间窗口必须保留，不能被默认排程静默忽略。模型失败时页面明确提示没有修改日程，并允许重试或改用规则排程。'),
 ('导入与人工校对边界','07-import-review.png','导入页面支持文本、图片和录音文件。文本可以由模型提取任务字段，但图片和录音当前仍然采用人工校对，没有宣称完成文字识别或语音转写。用户核对任务名称、截止和耗时，消除待确认信息后再保存。请求键和提交状态用于处理重复请求和失败恢复，确认后的任务会真正写入数据库。'),
 ('偏好与完成反馈','08-profile.png','任务完成时记录实际耗时，形成执行日志、画像事实和学习事件。估计服务根据有效样本给出参考倍率，样本不足三个时保持原估计。用户可以否认或删除不准确事实，失效证据不再参与计算。每日学习上限也经过统一版本和约束检查，不能通过修改偏好让已有安排突然违反规则。'),
 ('执行记录、版本与撤销','09-history.png','执行记录按照新旧顺序展示。用户可以查看操作回执，并对支持的操作进行撤销。撤销会检查原审计快照、当前状态、后续关联和硬约束，再按相反顺序恢复。存在后续修改时可能拒绝撤销，这是为了保护已经产生的新数据。完成反馈不通过普通撤销入口删除，避免任务状态和学习证据不一致。'),
 ('测试证据与交付限制','10-mobile.png','本轮完成一百零二项后端回归和集成测试、十七项前端单元测试、十五项外部接口黑盒测试，以及三条浏览器端到端场景。白盒使用真实覆盖率报告，区分核心模块和后端整体，不宣称百分之百覆盖。另有并发竞争、数据库持久化和本机小样本性能记录。服务重启恢复尚未计入已通过项。报告、源码和视频对应同一版本。正式认证、图片识别和跨设备同步仍是后续工作。')]
(work/'scenes.json').write_text(json.dumps([{'title':a,'image':b,'text':c} for a,b,c in scenes],ensure_ascii=False,indent=2),encoding='utf8')
md=['# 最终版五分钟讲解稿','本视频为实际界面截图配自动语音的演示，组长可按此稿重新录制。']
font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',42)
small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',25)
for i,(title,file,text) in enumerate(scenes):
    md += [f'\n## {i*30//60:02d}:{i*30%60:02d} {title}',text]
    frame=Image.new('RGB',(1920,1080),'#f4f7f4');d=ImageDraw.Draw(frame);d.text((55,18),f'{i+1:02d}  {title}',font=font,fill='#18423c');d.text((1390,30),'真实界面 · 自动配音演示',font=small,fill='#46665d')
    with Image.open(ROOT/'delivery/evidence/screenshots'/file) as im:
        im=ImageOps.contain(im.convert('RGB'),(1810,900));frame.paste(im,((1920-im.width)//2,92+(900-im.height)//2))
    d.text((55,1020),'学习日程 Agent  |  最终集成版  |  设计、实现与实测证据一致',font=small,fill='#46665d');frame.save(work/f'frame{i:02d}.png')
(ROOT/'delivery/五分钟讲解稿.md').write_text('\n'.join(md),encoding='utf8')
if '--encode' in sys.argv:
    sys.path.insert(0,str(ROOT/'.runtime/video-deps'));import imageio_ffmpeg
    ff=imageio_ffmpeg.get_ffmpeg_exe()
    normalized=[]
    for i in range(len(scenes)):
        audio=work/f'voice{i:02d}.wav'
        with wave.open(str(audio)) as w:duration=w.getnframes()/w.getframerate()
        tempo=max(1.0,duration/28.5);dst=work/f'fit{i:02d}.wav'
        subprocess.run([ff,'-y','-hide_banner','-loglevel','error','-i',str(audio),'-af',f'atempo={tempo},apad,atrim=duration=30','-ar','22050','-ac','1',str(dst)],check=True)
        normalized.append(dst)
    full=work/'narration.wav'
    with wave.open(str(full),'wb') as out:
        with wave.open(str(normalized[0])) as first:out.setparams(first.getparams())
        for audio in normalized:
            with wave.open(str(audio)) as w:out.writeframes(w.readframes(w.getnframes()))
    concat=work/'frames.txt'
    concat.write_text(''.join(f"file '{(work/f'frame{i:02d}.png').as_posix()}'\nduration 30\n" for i in range(10))+f"file '{(work/'frame09.png').as_posix()}'\n",encoding='utf8')
    output=ROOT/'delivery/学习日程Agent_五分钟自动讲解演示.mp4'
    subprocess.run([ff,'-y','-hide_banner','-loglevel','error','-f','concat','-safe','0','-i',str(concat),'-i',str(full),'-t','300','-r','15','-c:v','libx264','-preset','veryfast','-crf','24','-pix_fmt','yuv420p','-c:a','aac','-b:a','96k','-movflags','+faststart',str(output)],check=True)
    (ROOT/'delivery/evidence/video.json').write_text(json.dumps({'seconds':300,'method':'实际最终版截图与自动语音讲解','resolution':'1920x1080','framesPerSecond':15,'file':output.name},ensure_ascii=False,indent=2),encoding='utf8')
    print(output)

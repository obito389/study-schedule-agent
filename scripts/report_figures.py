from pathlib import Path
import csv, math
from PIL import Image,ImageDraw,ImageFont

def figures(root):
    out=root/'delivery/evidence/screenshots';out.mkdir(parents=True,exist_ok=True)
    font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',26)
    small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',21)
    def canvas(title,h=1100):
        im=Image.new('RGB',(1280,h),'white');d=ImageDraw.Draw(im);d.text((35,20),title,font=font,fill='#143c3c');return im,d
    def arrow(d,a,b,label=''):
        d.line([a,b],fill='#728b88',width=3);angle=math.atan2(b[1]-a[1],b[0]-a[0]);points=[b,(b[0]-14*math.cos(angle-.45),b[1]-14*math.sin(angle-.45)),(b[0]-14*math.cos(angle+.45),b[1]-14*math.sin(angle+.45))];d.polygon(points,fill='#728b88')
        if label:d.text(((a[0]+b[0])/2+5,(a[1]+b[1])/2-25),label,font=small,fill='#334c4b')
    im,d=canvas('学生学习日程用例图')
    d.rectangle((280,95,1000,1020),outline='#7b9990',width=3);d.text((330,110),'学习日程 Agent 系统边界',font=font,fill='black')
    d.ellipse((90,390,150,450),outline='black',width=3);d.line((120,450,120,570),fill='black',width=3);d.line((65,480,175,480),fill='black',width=3);d.line((120,570,75,645),fill='black',width=3);d.line((120,570,165,645),fill='black',width=3);d.text((82,665),'学生',font=font,fill='black')
    for i,name in enumerate(['管理课程与任务','查看日周月日历','生成候选并确认','导入与人工校对','完成任务与反馈','管理偏好与撤销']):
        y=190+i*127;d.ellipse((400,y,880,y+85),outline='#527e71',width=3,fill='#f6faf8');d.text((490,y+25),name,font=font,fill='black');d.line((175,480,400,y+42),fill='#8fa29b',width=2)
    d.rectangle((1050,390,1240,480),outline='#527e71',width=2);d.text((1065,415),'模型服务',font=font,fill='black');arrow(d,(1050,435),(1000,435),'建议')
    im.save(out/'uml-usecase.png')
    im,d=canvas('核心契约与服务类图',1150)
    boxes={
      'ScheduleSnapshot':(415,100,['now / timezone','courses / tasks','lockedBlocks / activeFacts','planningRevision']),
      'TaskSnapshot':(40,390,['id / deadline / priority','estimatedDurationMinutes','scheduledBlocks']),
      'CourseSnapshot':(415,390,['id / name','meetingBlocks']),
      'ProfileFact':(830,390,['key / value / scope','state / evidenceIds','version']),
      'AgentService':(40,740,['ModelAdapter','AgentToolRegistry','handle(instruction,snapshot)']),
      'GreedyScheduler':(415,740,['HardConstraintValidator','Clock','schedule(snapshot)']),
      'ActionExecutor':(830,740,['Repositories / user lock','preview / apply','request fingerprint'])}
    for name,(x,y,items) in boxes.items():
        d.rectangle((x,y,x+350,y+235),outline='#527e71',width=3,fill='#f9fbfa');d.line((x,y+58,x+350,y+58),fill='#527e71',width=2);d.text((x+10,y+15),name,font=font,fill='black')
        for j,t in enumerate(items):d.text((x+10,y+76+j*34),t,font=small,fill='#263f3e')
    for target in ['TaskSnapshot','CourseSnapshot','ProfileFact']:
        x,y,_=boxes[target];arrow(d,(590,335),(x+175,y),'包含')
    arrow(d,(215,740),(480,335),'读取');arrow(d,(590,740),(590,625),'校验');arrow(d,(1005,740),(1005,625),'采用契约')
    im.save(out/'uml-classes.png')
    im,d=canvas('模型预览与用户确认时序图',1200)
    names=['用户 / Vue','界面适配','AgentService','DeepSeek','ActionExecutor','MySQL']
    xs=[105,315,525,735,945,1155]
    for x,n in zip(xs,names):d.rectangle((x-88,85,x+88,145),outline='#527e71',width=2);d.text((x-80,101),n,font=small,fill='black');d.line((x,150,x,1130),fill='#c1ceca',width=2)
    steps=[(0,1,'提交自然语言要求'),(1,2,'读取快照并生成建议'),(2,3,'事务外请求结构化输出'),(3,2,'返回建议或歧义'),(2,1,'候选与状态'),(1,5,'保存服务端草案'),(1,0,'显示待确认预览'),(0,1,'用户确认'),(1,4,'执行保存的草案'),(4,5,'锁用户 / 版本 / 约束 / 写入'),(5,4,'提交与新版本'),(4,0,'回执并刷新快照')]
    for i,(a,b,label) in enumerate(steps):
        y=190+i*75;arrow(d,(xs[a],y),(xs[b],y));d.text((min(xs[a],xs[b])+8,y-30),label,font=small,fill='#1a3d39')
    im.save(out/'uml-sequence.png')
    im,d=canvas('16 表关系概览 按当前数据库外键生成',1300)
    names=['app_user','semester','course','course_session','task','task_step','time_block','execution_log','import_job','plan_draft','agent_action','profile_fact','profile_event','learning_stat','availability_window','user_preference']
    positions={n:(35+(i%4)*310,110+(i//4)*280) for i,n in enumerate(names)}
    rows=list(csv.reader((root/'.runtime/relations.tsv').read_text(encoding='utf-8-sig').splitlines(),delimiter='\t'))[1:]
    for child,parent,col in rows:
        if child in positions and parent in positions:
            a=positions[parent];b=positions[child];arrow(d,(a[0]+135,a[1]+90),(b[0]+135,b[1]+90))
    for n,(x,y) in positions.items():
        d.rectangle((x,y,x+280,y+180),outline='#527e71',width=2,fill='#f5faf7');d.text((x+10,y+18),n,font=font,fill='black')
        parents=[f'{col} → {parent}' for child,parent,col in rows if child==n]
        for i,t in enumerate(parents[:3]):d.text((x+10,y+68+i*28),t[:28],font=small,fill='#35514e')
    im.save(out/'er-overview.png')

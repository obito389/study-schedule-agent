const {chromium}=require('../frontend/node_modules/@playwright/test');
const fs=require('node:fs');const path=require('node:path');
const out=path.resolve('delivery/evidence/screenshots');fs.mkdirSync(out,{recursive:true});
const origin='http://127.0.0.1:8087/api/v1';
async function api(method,url,body){const r=await fetch(origin+url,{method,headers:{'Content-Type':'application/json','X-User-Id':'1'},body:body?JSON.stringify(body):undefined});let data=await r.json();if(!r.ok)throw new Error(JSON.stringify(data));return data.code===0?data.data:data;}
(async()=>{
 const s=await api('GET','/schedule');
 if(!s.tasks.length){
  const result=await api('POST','/ai/previews',{instruction:'明天晚上20点至21点复习数据库，创建一个60分钟的任务并安排这个时段。'});
  fs.writeFileSync('delivery/evidence/live-model.json',JSON.stringify({provider:'真实 DeepSeek',status:result.status,action:result.action,elapsedMillis:result.elapsedMillis},null,2));
  if(result.status==='NEEDS_CONFIRMATION')await api('POST',`/ai/drafts/${result.draftId}/confirm`);
  const now=await api('GET','/schedule');await api('POST','/workspace/commands',{operationKey:crypto.randomUUID(),baseRevision:now.planningRevision,type:'CREATE_TASK',authorizationScope:'schedule:write:self',payload:{title:'完成软件工程实验报告',deadline:new Date(Date.now()+3*86400000).toISOString(),durationMinutes:90,priority:2,courseId:''}});
 }
 const browser=await chromium.launch();const context=await browser.newContext({viewport:{width:1440,height:1000},recordVideo:{dir:path.resolve('.runtime/video'),size:{width:1440,height:1000}}});
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const snap=async(name)=>{await page.locator('.el-loading-mask').waitFor({state:'hidden'}).catch(()=>{});await page.waitForTimeout(650);await page.screenshot({path:path.join(out,name+'.png')});};
 await page.goto('http://127.0.0.1:5181/#/tasks');await page.getByTestId('new-task').waitFor();await snap('01-tasks');
 await page.getByTestId('new-task').click();await page.getByTestId('task-title').fill('学习计划示例');await snap('02-task-editor');await page.keyboard.press('Escape');
 await page.getByTestId('generate-plan').click();await page.getByRole('dialog').waitFor();await snap('03-plan-preview');await page.keyboard.press('Escape');
 await page.goto('http://127.0.0.1:5181/#/');await page.locator('.fc').waitFor();await snap('04-calendar-week');
 await page.getByRole('button',{name:'月',exact:true}).click();await snap('05-calendar-month');
 await page.goto('http://127.0.0.1:5181/#/ai');await page.locator('textarea').fill('查看我接下来有哪些任务和安排');await Promise.all([page.waitForResponse(r=>r.url().includes('/ai/previews'),{timeout:65000}),page.getByTestId('ai-generate').click()]);await page.waitForFunction(()=>!document.querySelector('[data-testid="ai-generate"]').disabled);await snap('06-ai');
 await page.goto('http://127.0.0.1:5181/#/import');await page.getByLabel('上传文件').setInputFiles({name:'作业通知示例.png',mimeType:'image/png',buffer:Buffer.from([1,2,3])});await page.getByRole('button',{name:'上传并生成提案'}).click();await page.getByLabel('任务名称',{exact:true}).fill('截图校对示例：完成数据库实验');await snap('07-import-review');
 await page.goto('http://127.0.0.1:5181/#/profile');await page.getByTestId('daily-limit').waitFor();await snap('08-profile');
 await page.getByRole('button',{name:/执行记录/}).click();await page.getByText('执行记录与撤销',{exact:true}).waitFor();await snap('09-history');await page.keyboard.press('Escape');await page.getByText('执行记录与撤销',{exact:true}).waitFor({state:'hidden'});
 await page.setViewportSize({width:444,height:706});await page.goto('http://127.0.0.1:5181/#/ai');await page.locator('textarea').waitFor();await snap('10-mobile');
 fs.writeFileSync('delivery/evidence/browser-capture.json',JSON.stringify({pageErrors:errors,screenshots:fs.readdirSync(out),generatedAt:new Date().toISOString()},null,2));
 await context.close();await browser.close();if(errors.length)throw new Error(errors.join(';'));
})();

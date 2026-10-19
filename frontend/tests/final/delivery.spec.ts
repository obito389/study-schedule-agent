import { test, expect } from '@playwright/test'

test('最终界面任务创建、规则确认、撤销及完成反馈',async({page})=>{
 const errors:string[]=[];page.on('pageerror',e=>errors.push(e.message))
 await page.goto('/#/tasks');await page.getByTestId('new-task').click()
 const title='最终版验收 '+Date.now();await page.getByTestId('task-title').fill(title);await page.getByTestId('task-submit').click()
 const row=page.getByRole('row').filter({hasText:title});await expect(row).toBeVisible()
 await page.getByTestId('generate-plan').click();await expect(page.getByRole('dialog')).toContainText('尚未写入日历')
 await page.getByTestId('confirm-plan').click();await expect(page.getByRole('dialog')).toBeHidden();await expect(row).toContainText('已安排')
 await page.getByRole('button',{name:/执行记录/}).click()
 const plan=page.locator('.history-item').filter({hasText:'保存排程'}).filter({has:page.getByRole('button',{name:'撤销这次操作'})}).first()
 await plan.getByRole('button',{name:'撤销这次操作'}).click();await page.getByRole('button',{name:'确认撤销',exact:true}).click()
 await expect(page.getByRole('button',{name:'确认撤销',exact:true})).toBeHidden();await page.keyboard.press('Escape')
 await row.getByRole('button',{name:'完成',exact:true}).click();await page.getByRole('button',{name:'确认完成',exact:true}).click()
 await page.getByRole('button',{name:'已完成',exact:true}).click();await expect(page.getByRole('row').filter({hasText:title})).toBeVisible()
 expect(errors).toEqual([])
})

test('导入校对、偏好持久化和窄屏导航',async({page})=>{
 await page.goto('/#/import');await page.getByLabel('上传文件').setInputFiles({name:'demo.png',mimeType:'image/png',buffer:Buffer.from([1,2,3])})
 await page.getByRole('button',{name:'上传并生成提案'}).click();await expect(page.getByText('待校对',{exact:true})).toBeVisible()
 await page.getByLabel('任务名称',{exact:true}).fill('最终导入 '+Date.now());await page.getByRole('button',{name:'已核对，确认创建任务'}).click()
 await expect(page.getByRole('heading',{name:'任务已保存'})).toBeVisible()
 await page.getByRole('button',{name:'设置',exact:true}).click();await page.getByRole('menuitem',{name:/偏好与反馈/}).click()
 await page.getByTestId('daily-limit').fill('300');await page.getByTestId('save-preference').click();await expect(page.getByText('每日最多 300 分钟',{exact:true})).toBeVisible()
 await page.reload();await expect(page.getByTestId('daily-limit')).toHaveValue('300')
 await page.setViewportSize({width:444,height:706});await page.getByRole('link',{name:'学习日历'}).click();await expect(page.locator('.fc')).toBeVisible()
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true)
 await page.getByRole('button',{name:'月',exact:true}).click();await expect(page.locator('.fc-dayGridMonth-view')).toBeVisible()
 await page.screenshot({path:'test-results/final-mobile.png',fullPage:true})
})

test('AI 待确认、固定模型确认及上游失败反馈',async({page})=>{
 await page.goto('/#/ai');const input=page.locator('textarea');await input.fill('创建一个预计60分钟的测试任务')
 await page.getByTestId('ai-generate').click();await expect(page.getByText('AI浏览器验收任务',{exact:false}).first()).toBeVisible()
 const confirm=page.getByRole('button',{name:'确认采用 AI 建议',exact:true});await confirm.click();await expect(page.getByText(/已.*保存|已.*执行/).first()).toBeVisible()
 await input.fill('失败');await page.getByTestId('ai-generate').click();await expect(page.getByTestId('ai-failed')).toBeVisible()
})


